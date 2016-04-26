package ru.qatools.selenograph.gridrouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.selenograph.ext.SelenographDB;
import ru.yandex.qatools.camelot.api.EventProducer;
import ru.yandex.qatools.camelot.api.annotations.*;
import ru.yandex.qatools.camelot.plugin.GraphiteReportProcessor;
import ru.yandex.qatools.camelot.plugin.GraphiteValue;
import ru.yandex.qatools.fsm.annotations.AfterTransit;
import ru.yandex.qatools.fsm.annotations.FSM;
import ru.yandex.qatools.fsm.annotations.Transit;
import ru.yandex.qatools.fsm.annotations.Transitions;

import javax.inject.Inject;
import java.util.Map;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static ru.qatools.selenograph.gridrouter.SessionsCountsPerUser.fromBrowserString;

/**
 * @author Ilya Sadykov
 */
@Aggregate
@FSM(start = SessionsCountsPerUser.class)
@Filter(instanceOf = SessionsCountsPerUser.class)
@Transitions(@Transit(on = SessionsCountsPerUser.class))
public class QuotaStatsAggregator {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaStatsAggregator.class);
    @Inject
    SessionsAggregator sessions;

    @Inject
    SelenographDB database;

    @Input
    EventProducer input;

    @ConfigValue("selenograph.gridrouter.graphite.prefix")
    String graphitePrefix;

    @Input(GraphiteReportProcessor.class)
    EventProducer graphite;

    @AfterTransit
    public void updateStats(SessionsCountsPerUser state, SessionsCountsPerUser to, SessionsCountsPerUser event) {
        state.updateStats(event);
    }

    @OnTimer(cron = "${selenograph.quota.stats.update.cron}", perState = false, skipIfNotCompleted = true)
    public void updateQuotaStats() {
        input.produce(new SessionsCountsPerUser(database.sessionsByUserCount()));
    }

    @OnTimer(cron = "0 * * * * ?", readOnly = false, skipIfNotCompleted = true)
    public void resetStats(SessionsCountsPerUser counts) {
        final Map<BrowserContext, Integer> current = database.sessionsByUserCount();
        counts.entrySet().forEach(entry -> {
            final SessionsState state = entry.getValue();
            LOGGER.info("Sending stats to graphite for {}:{}:{}...",
                    state.getUser(), state.getBrowser(), state.getVersion());
            final String prefix = format("%s.%s.%s-%s", graphitePrefix, state.getUser(),
                    state.getBrowser().replace(".", "_"),
                    state.getVersion().replace(".", "_"));
            final long timestamp = currentTimeMillis() / 1000L;
            graphite.produce(new GraphiteValue(prefix + ".stats.raw", state.getRaw(), timestamp));
            graphite.produce(new GraphiteValue(prefix + ".stats.max", state.getMax(), timestamp));
            graphite.produce(new GraphiteValue(prefix + ".stats.avg", state.getAvg(), timestamp));
            final Integer currentCount = current.get(fromBrowserString(entry.getKey()));
            state.setMax(currentCount);
            state.setAvg(currentCount);
            state.setRaw(currentCount);
        });
    }
}
