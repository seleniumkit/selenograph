package ru.qatools.selenograph.gridrouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.qatools.gridrouter.ConfigRepository;
import ru.qatools.gridrouter.config.Browser;
import ru.qatools.gridrouter.config.Browsers;
import ru.qatools.gridrouter.config.Host;
import ru.qatools.gridrouter.config.Version;
import ru.qatools.selenograph.BrowserInfo;
import ru.qatools.selenograph.HubAlive;
import ru.qatools.selenograph.front.BrowserSummary;
import ru.qatools.selenograph.front.BrowserSummaryMerge;
import ru.qatools.selenograph.front.VersionSummary;
import ru.qatools.selenograph.plugins.HubBrowserStateAggregator;
import ru.qatools.selenograph.plugins.HubStateAggregator;
import ru.qatools.selenograph.states.HubBrowserState;
import ru.qatools.selenograph.states.HubState;
import ru.qatools.selenograph.states.HubSummariesState;
import ru.yandex.qatools.camelot.api.AggregatorRepository;
import ru.yandex.qatools.camelot.api.Constants;
import ru.yandex.qatools.camelot.api.EventProducer;
import ru.yandex.qatools.camelot.api.annotations.*;
import ru.yandex.qatools.fsm.annotations.FSM;
import ru.yandex.qatools.fsm.annotations.OnTransit;
import ru.yandex.qatools.fsm.annotations.Transit;
import ru.yandex.qatools.fsm.annotations.Transitions;

import java.util.*;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.Comparator.comparing;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static ru.qatools.selenograph.front.BrowserSummaryMerge.MERGE_COLLECTOR;
import static ru.qatools.selenograph.util.Key.browserName;
import static ru.qatools.selenograph.util.Key.browserVersion;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
@Filter(instanceOf = HubSummariesState.class)
@Aggregate
@FSM(start = HashMap.class)
@Transitions(@Transit(on = HubSummariesState.class))
public class QuotaSummaryAggregator {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaSummaryAggregator.class);

    private static final String AGGREGATED_QUOTA_KEY = Constants.Keys.ALL;

    @ConfigValue("selenograph.hub.alive.enabled")
    boolean enableAliveSend;

    @ConfigValue("selenograph.sessions.emulate.enabled")
    boolean enableSessionsEmulation;

    @ConfigValue("selenograph.sessions.emulate.chance")
    int emulateSessionChance;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    ConfigRepository config;

    @Output
    EventProducer out;

    @MainInput
    EventProducer main;

    @Autowired
    SessionsAggregator sessions;

    @Repository(HubStateAggregator.class)
    AggregatorRepository<HubState> hubsRepo;

    @Repository(HubBrowserStateAggregator.class)
    AggregatorRepository<HubBrowserState> hubroRepo;

    @Repository
    AggregatorRepository repo;

    private static List<BrowserSummary> toBrowserSummaries(Browsers browsers, HubBrowserSummariesMap hubs) {
        List<BrowserSummary> result = new ArrayList<>();
        for (Browser browser : browsers.getBrowsers()) {
            List<VersionSummary> versions = toVersionSummaries(browser, hubs);
            if (!versions.isEmpty()) {
                result.add(toBrowserSummary(browserName(browser.getName()), versions));
            }
        }
        result.sort(comparing(BrowserSummary::getName));
        return result;
    }

    private static BrowserSummary toBrowserSummary(String name, List<VersionSummary> versions) {
        BrowserSummary bean = new BrowserSummary();
        bean.setName(name);
        bean.getVersions().addAll(versions);

        versions.stream().forEach(version -> {
            bean.setMax(bean.getMax() + version.getMax());
            bean.setOccupied(bean.getOccupied() + version.getOccupied());
            bean.setRunning(bean.getRunning() + version.getRunning());
        });
        return bean;
    }

    private static List<VersionSummary> toVersionSummaries(Browser browser, HubBrowserSummariesMap hubs) {
        return browser.getVersions().parallelStream()
                .map(version -> toVersionSummary(version, hubs, browserName(browser.getName())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }

    private static Optional<VersionSummary> toVersionSummary(Version version, HubBrowserSummariesMap hubs, String browser) {
        return version.getRegions().parallelStream()
                .flatMap(region -> region.getHosts().stream())
                .map(Host::getAddress)
                .flatMap(hubs::get)
                .filter(bs -> bs.getName().equals(browser))
                .map(BrowserSummaryMerge::new)
                .collect(MERGE_COLLECTOR).values().stream()
                .map(BrowserSummaryMerge::getBean)
                .flatMap(bs -> bs.getVersions().stream())
                //find the only one version summary that corresponds to the current version
                .filter(vs -> browserVersion(vs.getVersion()).equals(browserVersion(version.getNumber())))
                .findFirst();
    }

    public static Map<String, HubAlive> toHubSummary(Map<String, Browsers> quotaMap) {
        final Map<String, HubAlive> hubsMap = new HashMap<>();
        quotaMap.values().forEach(quota -> quota.getBrowsers().forEach(browser ->
                browser.getVersions().forEach(version -> version.getRegions().stream()
                        .flatMap(region -> region.getHosts().stream()).forEach(host -> {
                            if (!hubsMap.containsKey(host.getAddress())) {
                                hubsMap.put(host.getAddress(), new HubAlive()
                                        .withHubHost(host.getName())
                                        .withHubPort(host.getPort())
                                        .withHubName(host.getAddress())
                                        .withTimestamp(currentTimeMillis())
                                );
                            }
                            final HubAlive hub = hubsMap.get(host.getAddress());
                            final BrowserInfo browserInfo = new BrowserInfo()
                                    .withName(browser.getName())
                                    .withVersion(version.getNumber())
                                    .withMaxInstances(host.getCount());
                            if (!hub.getBrowsers().contains(browserInfo)) {
                                hub.getBrowsers().add(browserInfo);
                            }
                        }))));
        return hubsMap;
    }

    @OnTimer(cron = "${selenograph.sessions.emulate.cron}", perState = false, readOnly = true, skipIfNotCompleted = true)
    public void emulateSessions() {
        LOGGER.debug("Emulate sessions events for each hub in quota map...");
        if (enableSessionsEmulation) {
            hubroRepo.valuesMap().entrySet().forEach(hubro -> {
                final String route = "https://" + hubro.getKey().split("\\|")[0];
                hubro.getValue().getRunningSessions().forEach(sessionId -> {
                    if (new Random().nextInt(100) > 30) {
                        LOGGER.debug("Sending session {} deleted for {}", sessionId, route);
                        try {
                            sessions.deleteSession(sessionId, route);
                        } catch (Exception e) {
                            LOGGER.error("Failed to send delete session for {}{}", sessionId, route);
                        }
                    }
                });
            });
            config.getQuotaMap().entrySet().forEach(quota -> quota.getValue().getBrowsers().forEach(browser ->
                    browser.getVersions().forEach(version -> version.getRegions().stream()
                            .flatMap(region -> region.getHosts().stream()).forEach(host -> {
                                if (new Random().nextInt(100) > emulateSessionChance) {
                                    final String sessionId = randomUUID().toString();
                                    try {
                                        LOGGER.debug("Sending session {} started for {}", sessionId, host.getRoute());
                                        sessions.startSession(sessionId, quota.getKey(), browser.getName(),
                                                version.getNumber(), host.getRoute());
                                        sleep(50);
                                    } catch (Exception e) {
                                        LOGGER.error("Failed to send start session for {},{}", sessionId, host.getRoute());
                                    }
                                }
                            }))));
        }
    }

    @OnTimer(cron = "${selenograph.hub.alive.cron}", perState = false, readOnly = true, skipIfNotCompleted = true)
    public void emulateHubAliveEvents() {
        LOGGER.info("Sending HubAlive events for each hub in quota map...");
        if (enableAliveSend) {
            toHubSummary(config.getQuotaMap()).values().forEach(hub -> {
                LOGGER.info("Sending HubAlive for {}", hub);
                main.produce(hub);
                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    LOGGER.error("Failed to sleep between alives send");
                }
            });
        }
    }

    @OnTransit
    public void updateQuotaSummary(HashMap<String, List<BrowserSummary>> state, HubSummariesState event) {
        LOGGER.debug("updating quota summaries");

        HubBrowserSummariesMap hubs = new HubBrowserSummariesMap(event.getHubSummaries());

        state.clear();
        state.put(AGGREGATED_QUOTA_KEY, hubs.getAllBrowsers());

        Map<String, Browsers> quotaMap = config.getQuotaMap();
        for (String quota : quotaMap.keySet()) {
            List<BrowserSummary> browsers = toBrowserSummaries(quotaMap.get(quota), hubs);
            if (!browsers.isEmpty()) {
                state.put(quota, browsers);
            }
        }

        LOGGER.debug("successfully updated quota summaries");
        out.produce(state);
    }
}
