package ru.qatools.selenograph.plugins;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.qatools.selenograph.HubStarting;
import ru.qatools.selenograph.front.HubSummary;
import ru.qatools.selenograph.states.HubState;
import ru.qatools.selenograph.states.HubSummariesState;
import ru.yandex.qatools.camelot.api.ClientMessageSender;
import ru.yandex.qatools.camelot.api.Constants;
import ru.yandex.qatools.camelot.test.*;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static ru.qatools.selenograph.util.Key.hubAddress;
import static ru.qatools.selenograph.utils.MonitoringEventFactory.hubDown;
import static ru.qatools.selenograph.utils.MonitoringEventFactory.hubStarting;
import static ru.yandex.qatools.camelot.test.Matchers.containStateFor;
import static ru.yandex.qatools.matchers.decorators.MatcherDecorators.should;

@RunWith(CamelotTestRunner.class)
@UseProperties("selenograph-hub-remove.properties")
@DisableTimers
public class HubSummaryAggregatorTest extends BasePluginTest {

    @PluginMock
    HubStateAggregator stateAggregator;

    @AggregatorState(HubStateAggregator.class)
    AggregatorStateStorage stateStorage;

    @AggregatorState(HubSummaryAggregator.class)
    AggregatorStateStorage summaryStorage;

    @ClientSenderMock(HubSummaryClientNotifier.class)
    ClientMessageSender sender;

    @Test
    public void testBasicWorkflow() {
        HubStarting hubStarting = hubStarting();
        String key = hubAddress(hubStarting);
        Matcher<HubSummary> activeSummary = hubSummaryFor(key, true);
        Matcher<HubSummary> inactiveSummary = hubSummaryFor(key, false);

        send(hubStarting);
        storageShould(containStateFor(key), stateStorage);

        helper.invokeTimersFor(HubSummaryAggregator.class);
        assertSummaryState(hasItem(activeSummary));

        helper.invokeTimersFor(HubSummaryClientNotifier.class);
        verify(sender, timeout(TIMEOUT)).send(argThat(is(activeSummary)));

        send(hubDown());
        assertHubState(is(not(active())), key);
        assertSummaryState(hasItem(activeSummary));

        helper.invokeTimersFor(HubSummaryAggregator.class);
        assertSummaryState(hasItem(inactiveSummary));

        helper.invokeTimersFor(HubSummaryClientNotifier.class);
        verify(sender, timeout(TIMEOUT)).send(argThat(is(inactiveSummary)));

        helper.invokeTimersFor(HubStateAggregator.class);
        storageShould(not(containStateFor(key)), stateStorage);

        reset(sender);
        helper.invokeTimersFor(HubSummaryAggregator.class);
        assertSummaryState(isEmpty());
        helper.invokeTimersFor(HubSummaryClientNotifier.class);
        sleep(1000);
        verify(sender, never()).send(any());
    }

    @Test
    public void testTwoHubs() {
        HubStarting event1 = hubStarting("hub1");
        HubStarting event2 = hubStarting("hub2");
        String address1 = hubAddress(event1);
        String address2 = hubAddress(event2);
        Matcher<HubSummary> hubSummary1 = hubSummaryFor(address1, true);
        Matcher<HubSummary> hubSummary2 = hubSummaryFor(address2, true);

        send(event1);
        send(event2);
        storageShould(containStateFor(address1), stateStorage);
        storageShould(containStateFor(address2), stateStorage);

        helper.invokeTimersFor(HubSummaryAggregator.class);
        assertSummaryState(hasItem(hubSummary1));
        assertSummaryState(hasItem(hubSummary2));

        helper.invokeTimersFor(HubSummaryClientNotifier.class);
        verify(sender, timeout(TIMEOUT)).send(argThat(is(hubSummary1)));
        verify(sender, timeout(TIMEOUT)).send(argThat(is(hubSummary2)));
    }

    private void assertHubState(Matcher<HubState> stateMatcher, String key) {
        assertThat(stateStorage.get(HubState.class, key), should(stateMatcher)
                .whileWaitingUntil(timeoutHasExpired()));
    }

    private void assertSummaryState(Matcher<HubSummariesState> stateMatcher) {
        assertThat(summaryStorage.get(HubSummariesState.class, Constants.Keys.ALL), should(stateMatcher)
                .whileWaitingUntil(timeoutHasExpired()));
    }

    private Matcher<HubSummary> hubSummaryFor(String hubAddress, boolean active) {
        return both(hasAddress(hubAddress)).and(active(active));
    }

    private Matcher<HubSummary> active(boolean active) {
        return having(on(HubSummary.class).isActive(), equalTo(active));
    }

    private Matcher<HubSummary> hasAddress(String hubAddress) {
        return having(on(HubSummary.class).getAddress(), equalTo(hubAddress));
    }

    private Matcher<HubState> active() {
        return having(on(HubState.class).isActive(), is(true));
    }

    private Matcher<HubSummariesState> isEmpty() {
        return having(on(HubSummariesState.class).getHubSummaries(), empty());
    }

    private Matcher<HubSummariesState> hasItem(Matcher<HubSummary> summaryMatcher) {
        return having(on(HubSummariesState.class).getHubSummaries(), Matchers.hasItem(summaryMatcher));
    }
}
