package ru.qatools.selenograph.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.camelot.api.EventProducer;
import ru.yandex.qatools.camelot.api.annotations.AggregationKey;
import ru.yandex.qatools.camelot.api.annotations.InjectHeader;
import ru.yandex.qatools.camelot.api.annotations.Input;
import ru.yandex.qatools.camelot.api.annotations.Output;
import ru.yandex.qatools.fsm.StopConditionAware;
import ru.qatools.selenograph.StopCorrelationKey;

import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;
import static ru.qatools.clay.utils.DateUtil.isTimePassedSince;
import static ru.yandex.qatools.camelot.api.Constants.Headers.CORRELATION_KEY;
import static ru.qatools.selenograph.states.BeanUtils.stop;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public abstract class SelfStoppingAggregator implements StopConditionAware {

    /**
     * every 30 min
     */
    protected static final String TIME_TO_LIVE_CHECK_CRON = "15 */30 * * * ?";

    protected static final long TIME_TO_LIVE = TimeUnit.HOURS.toMillis(5);

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Input
    protected EventProducer self;

    @Output
    protected EventProducer out;

    private boolean stopNow = false;

    @InjectHeader(CORRELATION_KEY)
    protected String correlationKey;

    protected final boolean stopMyselfNow() {
        return checkAndStopMyself(0, 0, true);
    }

    protected final boolean stopMyself() {
        return checkAndStopMyself(0, 0, false);
    }

    protected final boolean checkAndStopMyselfNow(long time, long maxAge) {
        return checkAndStopMyself(time, maxAge, true);
    }

    protected final boolean checkAndStopMyself(long timestamp) {
        return checkAndStopMyself(timestamp, TIME_TO_LIVE);
    }

    protected final boolean checkAndStopMyself(long time, long maxAge) {
        return checkAndStopMyself(time, maxAge, false);
    }

    protected final boolean checkAndStopMyself(long time, long maxAge, boolean now) {
        if (!isTimePassedSince(maxAge, time) && time != 0) {
            return false;
        }

        logger.info("SelfStop: correlationKey={}, now={}, time={}, maxAge={}, stopNow={}",
                correlationKey, currentTimeMillis(), time, maxAge, now);
        if (!now) {
            self.produce(stop(correlationKey));
        } else {
            stopNow = true;
        }

        return true;
    }

    @AggregationKey
    public final String byCorrelationKey(StopCorrelationKey event) {
        return event.getCorrelationKey();
    }

    // Do not make this method final: test aggregator mocks will stop working
    @Override
    public boolean isStopRequired(Object o, Object o2) {
        logger.debug("Returning {} when asked for stopNow with correlationKey={}",
                stopNow, correlationKey);
        return stopNow;
    }
}
