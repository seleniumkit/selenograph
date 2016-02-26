package ru.qatools.selenograph.plugins;

import ru.yandex.qatools.camelot.api.annotations.Aggregate;
import ru.yandex.qatools.camelot.api.annotations.AggregationKey;
import ru.yandex.qatools.camelot.api.annotations.ConfigValue;
import ru.yandex.qatools.camelot.api.annotations.Filter;
import ru.yandex.qatools.fsm.annotations.*;
import ru.qatools.selenograph.*;
import ru.qatools.selenograph.states.HubBrowserState;

import static java.util.concurrent.TimeUnit.HOURS;
import static ru.qatools.selenograph.util.Key.*;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
@Filter(instanceOf = {BrowserStarted.class, SessionReleasing.class, SessionExpired.class, HubBrowserDown.class})
@Aggregate
@FSM(start = HubBrowserState.class)
@Transitions({
        @Transit(on = {BrowserEvent.class, SessionExpired.class}),
        @Transit(stop = true, on = HubBrowserDown.class)
})
public class HubBrowserStateAggregator extends SessionExpirationAggregator {

    @ConfigValue("selenograph.nodeInactivityTimeoutMillis")
    public long nodeInactivityTimeoutMillis = HOURS.toMillis(1);

    @AggregationKey
    public String aggregationKey(BrowserEvent event) {
        return byHubBrowser(event.getHubHost(), event.getHubPort(), event.getBrowserInfo());
    }

    @OnTransit
    public void onBrowserStarted(HubBrowserState state, BrowserStarted event) {
        log("Starting", event);
        state.startSession(event.getSessionId(), event.getTimestamp());
    }

    @OnTransit
    public void onSessionReleasing(HubBrowserState state, SessionReleasing event) {
        log("Releasing", event);
        state.releaseSession(event.getSessionId(), event.getTimestamp());
    }

    @AfterTransit
    public void logState(HubBrowserState state) {
        log(state);
    }

    private void log(String modifier, BrowserEvent event) {
        logger.info("{} {} v.{}",
                modifier,
                browserName(event.getBrowserInfo()),
                browserVersion(event.getBrowserInfo()));
        logger.debug("{} - {} {}", correlationKey, modifier.toLowerCase(), event.getSessionId());
    }

    private void log(HubBrowserState state) {
        logger.info("{} - state is now: count={}, running={}, stopping={}",
                correlationKey, state.getRunningCount(), state.getRunningSessions(), state.getStoppingSessions());
    }
}
