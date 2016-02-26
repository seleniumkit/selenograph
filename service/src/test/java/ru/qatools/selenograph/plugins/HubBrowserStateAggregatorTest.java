package ru.qatools.selenograph.plugins;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.qatools.selenograph.BrowserInfo;
import ru.qatools.selenograph.BrowserStarted;
import ru.qatools.selenograph.SessionReleasing;
import ru.qatools.selenograph.states.HubBrowserState;
import ru.yandex.qatools.camelot.test.*;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static ru.qatools.selenograph.util.Key.byHubBrowser;
import static ru.qatools.selenograph.utils.MonitoringEventFactory.*;
import static ru.yandex.qatools.matchers.decorators.MatcherDecorators.should;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
@DisableTimers
@RunWith(CamelotTestRunner.class)
public class HubBrowserStateAggregatorTest extends BasePluginTest {

    private final BrowserInfo browser = browser("firefox", "23");
    private final String aggregationKey = byHubBrowser("hub", 4444, browser);
    @PluginMock
    HubBrowserStateAggregator nodeState;
    @AggregatorState(HubBrowserStateAggregator.class)
    AggregatorStateStorage stateStorage;

    @Test
    public void testNodeUpAndRunBrowser() {
        BrowserStarted browserStarted = browserStarted("hub", browser);
        send(browserStarted);
        assertState("There must be the only session running",
                containSession(browserStarted.getSessionId()));
    }

    @Test
    public void testBrowserEventsReverseOrder() {
        BrowserStarted starting = browserStarted("hub", browser);
        SessionReleasing releasing = sessionReleasing("hub", starting.getSessionId(), browser);

        send(releasing);
        verify(nodeState, timeout(TIMEOUT))
                .onSessionReleasing(any(HubBrowserState.class), any(SessionReleasing.class));

        send(starting);
        verify(nodeState, timeout(TIMEOUT))
                .onBrowserStarted(any(HubBrowserState.class), any(BrowserStarted.class));

        shouldStop(stateStorage, aggregationKey,
                "should stop immediately when no sessions running");
    }

    private HubBrowserState state() {
        return stateStorage.get(HubBrowserState.class, aggregationKey);
    }

    private void assertState(String message, Matcher<HubBrowserState> stateMatcher) {
        assertThat(message, state(), should(stateMatcher)
                .whileWaitingUntil(timeoutHasExpired()));
    }

    private Matcher<HubBrowserState> containSession(String sessionId) {
        return having(on(HubBrowserState.class).getRunningSessions(), contains(sessionId));
    }
}
