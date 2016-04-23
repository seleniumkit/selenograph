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

/**
 * @author Ilya Sadykov
 */
@Aggregate
@FSM(start = SessionsState.class)
@Filter(instanceOf = SessionsState.class)
@Transitions(@Transit(on = SessionsState.class))
public class QuotaStatsAggregator {
    protected static final Logger LOGGER = LoggerFactory.getLogger(QuotaStatsAggregator.class);
    @Inject
    SessionsAggregator sessions;

    @ConfigValue("selenograph.gridrouter.graphite.prefix")
    String graphitePrefix;

    @Input(GraphiteReportProcessor.class)
    EventProducer graphite;

    @AggregationKey
    public String aggregationKey(SessionsState event) {
        return event.getUser() + ":" + event.getBrowser() + ":" + event.getVersion();
    }

    @BeforeTransit
    public void beforeCreate(SessionsState state, SessionsState to, SessionsState event) {
        state.withUser(event.getUser())
                .withBrowser(event.getBrowser())
                .withVersion(event.getVersion())
                .withRaw(event.getRaw());
    }

    @AfterTransit
    public void updateStats(SessionsState state, SessionsState to, SessionsState event) {
        state.setMax(state.getRaw() > state.getMax() ? state.getRaw() : state.getMax());
        state.setAvg(round(((float) state.getAvg() + (float) state.getRaw()) / 2.0f));
        state.setTimestamp(currentTimeMillis());
    }

    @OnTimer(cron = "0 * * * * ?", skipIfNotCompleted = true)
    public void resetStats(SessionsState state) {
        LOGGER.info("Sending stats to graphite for {}:{}:{}...",
                state.getUser(), state.getBrowser(), state.getVersion());
        final String prefix = format("%s.%s.%s-%s", graphitePrefix, state.getUser(),
                state.getBrowser().replace(".", "_"),
                state.getVersion().replace(".", "_"));
        final long timestamp = currentTimeMillis() / 1000L;
        graphite.produce(new GraphiteValue(prefix + ".stats.raw", state.getRaw(), timestamp));
        graphite.produce(new GraphiteValue(prefix + ".stats.max", state.getMax(), timestamp));
        graphite.produce(new GraphiteValue(prefix + ".stats.avg", state.getAvg(), timestamp));
    }
}
