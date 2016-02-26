package ru.qatools.selenograph.gridrouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.camelot.api.EventProducer;
import ru.yandex.qatools.camelot.api.annotations.*;
import ru.yandex.qatools.camelot.plugin.GraphiteReportProcessor;
import ru.yandex.qatools.camelot.plugin.GraphiteValue;
import ru.yandex.qatools.fsm.annotations.*;

import javax.inject.Inject;

import static java.lang.Math.round;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.time.ZonedDateTime.now;

/**
 * @author Ilya Sadykov
 */
@Aggregate
@FSM(start = SessionsState.class)
@Filter(custom = SessionEventFilter.class)
@Transitions(@Transit(on = SessionEvent.class))
public class QuotaStatsAggregator {
    protected static final Logger LOGGER = LoggerFactory.getLogger(QuotaStatsAggregator.class);
    @Inject
    SessionsAggregator sessions;

    @ConfigValue("selenograph.gridrouter.graphite.prefix")
    String graphitePrefix;

    @Input(GraphiteReportProcessor.class)
    EventProducer graphite;

    @AggregationKey
    public String aggregationKey(SessionEvent event) {
        return event.getUser() + ":" + event.getBrowser() + ":" + event.getVersion();
    }

    @BeforeTransit
    public void beforeCreate(SessionsState state, SessionEvent event) {
        state.withUser(event.getUser())
                .withBrowser(event.getBrowser())
                .withVersion(event.getVersion());
    }

    @OnTransit
    public void onStart(SessionsState state, StartSessionEvent event) {
        state.setRaw(state.getRaw() + 1);
        LOGGER.debug("Starting session {} for {}:{}:{}, new raw is: {}", event.getSessionId(),
                state.getUser(), state.getBrowser(), state.getVersion(), state.getRaw());
    }

    @OnTransit
    public void onDelete(SessionsState state, DeleteSessionEvent event) {
        state.setRaw(state.getRaw() - 1);
        LOGGER.debug("Stopping session {} for {}:{}:{}, new raw is: {}", event.getSessionId(),
                state.getUser(), state.getBrowser(), state.getVersion(), state.getRaw());
    }

    @AfterTransit
    public void updateStats(SessionsState state, SessionEvent event) {
        state.setMax(state.getRaw() > state.getMax() ? state.getRaw() : state.getMax());
        state.setAvg(round(((float) state.getAvg() + (float) state.getRaw()) / 2.0f));
        state.setTimestamp(now());
    }

    @OnTimer(cron = "0 * * * * ?", readOnly = false, skipIfNotCompleted = true)
    public void resetStats(SessionsState state) {
        LOGGER.info("Resetting stats every minute for {}:{}:{}...",
                state.getUser(), state.getBrowser(), state.getVersion());
        final String prefix = format("%s.%s.%s-%s", graphitePrefix, state.getUser(),
                state.getBrowser().replace(".", "_"),
                state.getVersion().replace(".", "_"));
        final long timestamp = currentTimeMillis() / 1000L;
        final int count = (int) sessions.countSessionsByUserAndBrowser(state.getUser(),
                state.getBrowser(), state.getVersion());
        graphite.produce(new GraphiteValue(prefix + ".stats.raw", state.getRaw(), timestamp));
        graphite.produce(new GraphiteValue(prefix + ".stats.max", state.getMax(), timestamp));
        graphite.produce(new GraphiteValue(prefix + ".stats.avg", state.getAvg(), timestamp));
        graphite.produce(new GraphiteValue(prefix + ".stats.current", count, timestamp));
        state.setMax(count);
        state.setAvg(count);
        state.setRaw(count);
    }
}
