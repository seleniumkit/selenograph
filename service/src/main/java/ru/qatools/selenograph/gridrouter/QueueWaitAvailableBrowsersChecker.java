package ru.qatools.selenograph.gridrouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.gridrouter.config.Version;
import ru.qatools.gridrouter.sessions.WaitAvailableBrowsersChecker;
import ru.yandex.qatools.camelot.api.EventProducer;
import ru.yandex.qatools.camelot.api.annotations.*;
import ru.yandex.qatools.fsm.annotations.*;

import java.time.Duration;

import static java.lang.String.format;

/**
 * @author Ilya Sadykov
 */
@Aggregate
@FSM(start = WaitAvailableBrowserState.class)
@Filter(instanceOf = SessionRequest.class)
@Transitions(@Transit(on = SessionRequest.class))
public class QueueWaitAvailableBrowsersChecker extends WaitAvailableBrowsersChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueWaitAvailableBrowsersChecker.class);
    @Input
    EventProducer input;

    @Override
    public void ensureFreeBrowsersAvailable(String user, String remoteHost, String browser, Version version) {
        super.ensureFreeBrowsersAvailable(user, remoteHost, browser, version);
    }

    @Override
    protected void onWait(String user, String browser, Version version, String requestId, int waitAttempt) {
        super.onWait(user, browser, version, requestId, waitAttempt);
        if (waitAttempt == 0) {
            input.produce(new SessionRequestEnqueued()
                    .withRequestId(requestId)
                    .withUser(user)
                    .withBrowser(browser)
                    .withVersion(version.getNumber())
            );
        }
    }

    @Override
    protected void onWaitFinished(String user, String browser, Version version, String requestId, int waitAttempt) {
        super.onWaitFinished(user, browser, version, requestId, waitAttempt);
        input.produce(new SessionRequestDequeued()
                .withRequestId(requestId)
                .withUser(user)
                .withBrowser(browser)
                .withVersion(version.getNumber())
        );
    }

    @AggregationKey
    public String aggregationKey(SessionRequest event) {
        return format("%s-%s-%s", event.getUser(), event.getBrowser(), event.getVersion());
    }

    @BeforeTransit
    public void onBeforeRequest(WaitAvailableBrowserState state, SessionRequest event) {
        state.setVersion(event.getVersion());
        state.setBrowser(event.getBrowser());
        state.setUser(event.getUser());
    }

    @OnTransit
    public void onDequeued(WaitAvailableBrowserState state, SessionRequestDequeued event) {
        LOGGER.debug("Removing request {} from the queue for {}-{}-{}, queue size={}",
                event.getRequestId(), event.getUser(), event.getBrowser(), event.getVersion(), state.size());
        state.removeRequest(event.getRequestId());
    }

    @OnTransit
    public void onEnqueued(WaitAvailableBrowserState state, SessionRequestEnqueued event) {
        LOGGER.debug("Adding new request {} to the queue for {}-{}-{}, queue size={}",
                event.getRequestId(), event.getUser(), event.getBrowser(), event.getVersion(), state.size());
        state.addRequest(event.getRequestId());
    }

    @OnTimer(cron = "${grid.router.queue.request.timeout.cron}", readOnly = false)
    public void checkAndExpireTimedoutRequests(WaitAvailableBrowserState state) {
        LOGGER.debug("Before expiration of outdated session requests for {}-{}-{}, queue size = {}",
                state.getUser(), state.getBrowser(), state.getVersion(), state.size());
        state.expireRequestsOlderThan(Duration.ofMillis(queueTimeout));
        LOGGER.debug("After expiration of outdated session requests for {}-{}-{}, queue size = {}",
                state.getUser(), state.getBrowser(), state.getVersion(), state.size());
    }
}
