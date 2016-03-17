package ru.qatools.selenograph.gridrouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.gridrouter.sessions.StatsCounter;
import ru.qatools.selenograph.BrowserInfo;
import ru.qatools.selenograph.BrowserStarted;
import ru.qatools.selenograph.SessionReleasing;
import ru.qatools.selenograph.plugins.HubBrowserStateAggregator;
import ru.yandex.qatools.camelot.api.AggregatorRepository;
import ru.yandex.qatools.camelot.api.EventProducer;
import ru.yandex.qatools.camelot.api.annotations.*;
import ru.yandex.qatools.fsm.annotations.*;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.between;
import static java.time.ZonedDateTime.now;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static ru.qatools.selenograph.util.Key.browserName;
import static ru.qatools.selenograph.util.Key.browserVersion;

/**
 * @author Ilya Sadykov
 */
@Aggregate
@Filter(custom = SessionEventFilter.class)
@FSM(start = UndefinedSessionState.class)
@Transitions({
        @Transit(to = StartSessionEvent.class, on = StartSessionEvent.class),
        @Transit(on = DeleteSessionEvent.class, stop = true)
})
public class SessionsAggregator implements StatsCounter {
    public static final String ROUTE_REGEX = "http://([^:]+):(\\d+)";
    protected static final Logger LOGGER = LoggerFactory.getLogger(SessionsAggregator.class);
    @Input(HubBrowserStateAggregator.class)
    EventProducer browserStates;
    @Input(QuotaStatsAggregator.class)
    EventProducer quotaStats;
    @Input(SessionsAggregator.class)
    private EventProducer input;

    @Repository(SessionsAggregator.class)
    private AggregatorRepository<SessionEvent> repo;

    @Repository(QuotaStatsAggregator.class)
    private AggregatorRepository<SessionsState> quotaStatsRepo;

    @NewState
    public Object newState(Class stateClass, StartSessionEvent event) throws Exception {
        return event.withTimestamp(now());
    }

    @AggregationKey
    public String aggregationKey(SessionEvent event) {
        return event.getUser() + ":" + event.getBrowser() + ":" + event.getVersion() + ":" + event.getSessionId();
    }

    @OnTransit
    public void onStart(UndefinedSessionState state, StartSessionEvent event) {
        LOGGER.debug("onStart session {} for {}:{}:{} ({})", event.getSessionId(), event.getUser(),
                event.getBrowser(), event.getVersion(), event.getRoute());
        quotaStats.produce(event);
        browserStates.produce(new BrowserStarted().withSessionId(event.getSessionId())
                .withHubHost(toHubHost(event.getRoute()))
                .withHubPort(toHubPort(event.getRoute()))
                .withTimestamp(currentTimeMillis())
                .withBrowserInfo(new BrowserInfo().withName(event.getBrowser()).withVersion(event.getVersion()))
        );
    }

    @OnTransit
    public void onDelete(StartSessionEvent state, DeleteSessionEvent event) {
        LOGGER.debug("onDelete session {} for {}:{}:{} ({})", event.getSessionId(), event.getUser(),
                event.getBrowser(), event.getVersion(), event.getRoute());
        quotaStats.produce(deleteEvent(state));
        browserStates.produce(new SessionReleasing().withSessionId(event.getSessionId())
                .withHubHost(toHubHost(state.getRoute())).withHubPort(toHubPort(state.getRoute()))
                .withTimestamp(currentTimeMillis())
                .withBrowserInfo(new BrowserInfo().withName(state.getBrowser())
                        .withVersion(state.getVersion()))
        );
    }

    @Override
    public void startSession(String sessionId, String user, String browser, String version, String route) {
        final String name = browserName(browser);
        final String ver = browserVersion(version);
        LOGGER.info("Starting session {} for {}:{}:{} ({})", sessionId, user, name, ver, route);
        final StartSessionEvent startEvent = new StartSessionEvent()
                .withSessionId(sessionId)
                .withRoute(route)
                .withUser(user)
                .withBrowser(name)
                .withVersion(ver);
        input.produce(startEvent);
    }

    @Override
    public void deleteSession(String sessionId, String route) {
        LOGGER.info("Removing session {} ({})", sessionId, route);
        input.produce(deleteEvent(findSessionById(sessionId)));
    }

    private String toHubHost(String route) {
        return route != null ? route.replaceFirst(ROUTE_REGEX, "$1") : "";
    }

    private int toHubPort(String route) {
        final String portString = route != null ? route.replaceAll(ROUTE_REGEX, "$2") : "";
        return isNumeric(portString) ? parseInt(portString) : 0;
    }

    @Override
    public void expireSessionsOlderThan(Duration duration) {
        repo.valuesMap().values().forEach(state -> {
            if (duration.compareTo(between(state.getTimestamp(), now())) < 0) {
                input.produce(deleteEvent(state));
            }
        });
    }

    @Override
    public Set<String> getActiveSessions() {
        return repo.valuesMap().entrySet().stream()
                .map(s -> s.getValue().getSessionId()).collect(toSet());
    }

    @Override
    public UserSessionsStats getStats(String user) {
        return new UserSessionsStats(quotaStatsRepo.valuesMap().entrySet().stream()
                .filter(state -> state.getValue().getUser().equals(user))
                .map(Map.Entry::getValue)
                .collect(toList()), repo.valuesMap());
    }

    @Override
    public int getSessionsCountForUser(String user) {
        return (int) countSessionsByUser(user);
    }

    @Override
    public int getSessionsCountForUserAndBrowser(String user, String browser, String version) {
        return (int) countSessionsByUserAndBrowser(user, browser, version);
    }

    public long countSessionsByUser(String user) {
        return repo.keys().stream().filter(s -> s.startsWith(user + ":")).count();
    }

    public long countSessionsByUserAndBrowser(String user, String browser, String version) {
        return repo.keys().stream().filter(s -> s.startsWith(user + ":" + browser + ":" + version)).count();
    }

    public SessionEvent findSessionById(String sessionId) {
        final Optional<Map.Entry<String, SessionEvent>> context = repo.valuesMap()
                .entrySet().stream().filter(s -> s.getKey().contains(sessionId)).findFirst();
        return context.isPresent() ? context.get().getValue() : null;
    }

    private DeleteSessionEvent deleteEvent(SessionEvent state) {
        return new DeleteSessionEvent().withUser(state.getUser())
                .withBrowser(state.getBrowser())
                .withVersion(state.getVersion())
                .withRoute(state.getRoute())
                .withSessionId(state.getSessionId());
    }
}
