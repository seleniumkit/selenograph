package ru.qatools.selenograph.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.camelot.api.EventProducer;
import ru.yandex.qatools.camelot.api.annotations.*;
import ru.yandex.qatools.fsm.StopConditionAware;
import ru.yandex.qatools.fsm.annotations.OnTransit;
import ru.qatools.selenograph.SessionExpired;
import ru.qatools.selenograph.states.HubBrowserState;

import static java.util.concurrent.TimeUnit.MINUTES;
import static ru.yandex.qatools.camelot.api.Constants.Headers.CORRELATION_KEY;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public abstract class SessionExpirationAggregator
        implements StopConditionAware<HubBrowserState, Object> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @ConfigValue("selenograph.maxSessionTimeMillis")
    public long maxSessionTimeMillis = MINUTES.toMillis(20);

    @Input
    protected EventProducer self;

    @InjectHeader(CORRELATION_KEY)
    protected String correlationKey;

    @AggregationKey
    public String bySessionExpired(SessionExpired event) {
        return event.getCorrelationKey();
    }

    @OnTransit
    public void onSessionExpired(HubBrowserState state, SessionExpired event) {
        logger.info("Session is expired {}", event.getSessionId());
        state.expireSession(event.getSessionId());
    }

    @Override
    public boolean isStopRequired(HubBrowserState state, Object event) {
        if (state.isEmpty()) {
            logger.debug("Stopping state for {} because it's empty", correlationKey);
            return true;
        }
        return false;
    }

    @OnTimer(cron = "0 * * * * ?", skipIfNotCompleted = true)
    public void expiredSessionsCheck(HubBrowserState state) {
        for (String sessionId : state.getExpiredSessions(maxSessionTimeMillis)) {
            logger.debug("Session is expired: {}", sessionId);
            selfExpireSession(sessionId);
        }
    }

    private void selfExpireSession(String sessionId) {
        SessionExpired sessionExpired = new SessionExpired();
        sessionExpired.setCorrelationKey(correlationKey);
        sessionExpired.setSessionId(sessionId);
        self.produce(sessionExpired);
    }
}
