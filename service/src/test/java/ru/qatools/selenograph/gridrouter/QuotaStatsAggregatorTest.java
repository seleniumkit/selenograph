package ru.qatools.selenograph.gridrouter;

import org.apache.camel.component.mock.MockEndpoint;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qatools.camelot.plugin.GraphiteReportProcessor;
import ru.yandex.qatools.camelot.plugin.GraphiteValue;
import ru.yandex.qatools.camelot.test.*;
import ru.qatools.selenograph.gridrouter.UserSessionsStats.StatsData;
import ru.qatools.selenograph.plugins.HubBrowserStateAggregator;

import java.util.Set;
import java.util.UUID;

import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static ru.yandex.qatools.matchers.decorators.MatcherDecorators.should;
import static ru.yandex.qatools.matchers.decorators.TimeoutWaiter.timeoutHasExpired;

/**
 * @author Ilya Sadykov
 */
@RunWith(CamelotTestRunner.class)
@DisableTimers
public class QuotaStatsAggregatorTest {
    public static final int TIMEOUT = 3000;

    @Helper
    TestHelper helper;

    @Autowired
    SessionsAggregator sessions;

    @PluginMock
    QuotaStatsAggregator mock;

    @PluginMock
    GraphiteReportProcessor graphite;

    @AggregatorState(QuotaStatsAggregator.class)
    AggregatorStateStorage counterStorage;

    @AggregatorState(SessionsAggregator.class)
    AggregatorStateStorage sessionsStorage;

    @EndpointPluginInput(QuotaStatsAggregator.class)
    MockEndpoint quotaStatsAgg;

    @EndpointPluginInput(HubBrowserStateAggregator.class)
    MockEndpoint nodeBrowserState;

    @Test
    public void testStartMultipleSessions() throws Exception {
        // Launch 3 sessions
        expectMessagesCount(3);
        String sessionId1 = startSessionFor("vasya");
        String sessionId2 = startSessionFor("vasya");
        String sessionId3 = startSessionFor("vasya");
        assertMessagesReceived();
        assertSessionStateFor("vasya", sessionId1, notNullValue());
        assertSessionStateFor("vasya", sessionId2, notNullValue());
        assertSessionStateFor("vasya", sessionId3, notNullValue());
        sleep(1000);

        assertThat(activeSessionsFor("vasya"), hasItems(sessionId1, sessionId2, sessionId3));
        assertThat(stateFor("vasya").getMax(), is(3));
        assertThat(stateFor("vasya").getRaw(), is(3));
        assertThat(stateFor("vasya").getAvg(), greaterThanOrEqualTo(2));

        // Stop two sessions
        expectMessagesCount(2);
        stopSessionFor("vasya", sessionId1);
        stopSessionFor("vasya", sessionId2);
        assertMessagesReceived();
        sleep(1000);
        assertThat(activeSessionsFor("vasya"), not(hasItem(sessionId1)));
        assertThat(activeSessionsFor("vasya"), not(hasItem(sessionId2)));
        assertThat(stateFor("vasya").getMax(), is(3));
        assertThat(stateFor("vasya").getRaw(), is(1));
        assertThat(stateFor("vasya").getAvg(), is(2));

        // Start one more session
        expectMessagesCount(1);
        String sessionId4 = startSessionFor("vasya");
        assertMessagesReceived();
        assertSessionStateFor("vasya", sessionId4, notNullValue());
        sleep(1000);
        assertThat(activeSessionsFor("vasya"), hasItem(sessionId4));
        assertThat(stateFor("vasya").getMax(), is(3));
        assertThat(stateFor("vasya").getRaw(), is(2));
        assertThat(stateFor("vasya").getAvg(), is(2));
        assertThat(sessions.getActiveSessions(), containsInAnyOrder(sessionId3, sessionId4));

        // Start one more session
        expectMessagesCount(1);
        String sessionId5 = startSessionFor("petya");
        assertMessagesReceived();
        assertSessionStateFor("vasya", sessionId5, notNullValue());
        sleep(1000);
        assertThat(activeSessionsFor("petya"), hasItem(sessionId5));
        assertThat(stateFor("petya").getMax(), is(1));
        assertThat(stateFor("petya").getRaw(), is(1));
        assertThat(stateFor("petya").getAvg(), is(1));

        assertThat(sessions.getActiveSessions(), containsInAnyOrder(sessionId3, sessionId4, sessionId5));
        verify(mock, timeout(TIMEOUT).times(5)).onStart(any(SessionsState.class), any(StartSessionEvent.class));
        verify(mock, timeout(TIMEOUT).times(2)).onDelete(any(SessionsState.class), any(DeleteSessionEvent.class));
        verify(mock, timeout(TIMEOUT).times(7)).updateStats(any(SessionsState.class), any(SessionEvent.class));

        assertThat(sessions.getStats("petya").keySet(), hasSize(1));
        assertThat(sessions.getStats("vasya").keySet(), hasSize(1));
        final StatsData vasyaStats = sessions.getStats("vasya").values().stream().findFirst().get();
        final StatsData petyaStats = sessions.getStats("petya").values().stream().findFirst().get();
        assertThat(vasyaStats.getCurrent(), is(2L));
        assertThat(vasyaStats.getMax(), is(3));
        assertThat(vasyaStats.getRaw(), is(2));
        assertThat(vasyaStats.getAvg(), is(2));
        assertThat(petyaStats.getCurrent(), is(1L));
        assertThat(petyaStats.getMax(), is(1));
        assertThat(petyaStats.getRaw(), is(1));
        assertThat(petyaStats.getAvg(), is(1));
        helper.invokeTimersFor(QuotaStatsAggregator.class);
        assertThat(stateFor("petya").getMax(), is(1));
        assertThat(stateFor("petya").getAvg(), is(1));
        assertThat(stateFor("petya").getRaw(), is(1));
        assertThat(stateFor("vasya").getMax(), is(2));
        assertThat(stateFor("vasya").getAvg(), is(2));
        assertThat(stateFor("vasya").getRaw(), is(2));
        verify(graphite, timeout(TIMEOUT).times(8)).process(any(GraphiteValue.class));
    }


    @Test
    public void testNoTransitForNoContent() throws Exception {
        startSessionFor("vasya");
        helper.sendTo(SessionsAggregator.class, "");
        helper.sendTo(SessionsAggregator.class, new StartSessionEvent().withUser("vasya"));
        sleep(2000);
        verify(mock, timeout(TIMEOUT).times(1)).onStart(any(SessionsState.class), any(StartSessionEvent.class));
    }

    private void assertMessagesReceived() throws InterruptedException {
        nodeBrowserState.assertIsSatisfied();
        quotaStatsAgg.assertIsSatisfied();
    }

    private void expectMessagesCount(int count) {
        nodeBrowserState.reset();
        quotaStatsAgg.reset();
        quotaStatsAgg.expectedMessageCount(count);
        nodeBrowserState.expectedMessageCount(count);
    }

    private void assertSessionStateFor(String user, String sessionId, Matcher<Object> matcher) {
        assertThat(counterStorage.get(SessionsState.class, keyFor(user)),
                should(notNullValue()).whileWaitingUntil(timeoutHasExpired(TIMEOUT)));
        assertThat(sessionsStorage.get(SessionEvent.class, keyFor(user) + ":" + sessionId),
                should(matcher).whileWaitingUntil(timeoutHasExpired(TIMEOUT)));
    }

    private Set<String> activeSessionsFor(String user) {
        return sessionsStorage.keys().stream()
                .filter(s -> s.startsWith(user))
                .map(s -> s.substring(s.lastIndexOf(":") + 1))
                .collect(toSet());
    }

    private SessionsState stateFor(String user) {
        return counterStorage.getActual(keyFor(user));
    }

    private String keyFor(String user) {
        return user + ":firefox:33.0";
    }

    private void stopSessionFor(String user, String sessionId) {
        sessions.deleteSession(sessionId);
    }

    private String startSessionFor(String user) {
        final String sessionId = UUID.randomUUID().toString();
        sessions.startSession(sessionId, user, "firefox", "33.0");
        return sessionId;
    }
}