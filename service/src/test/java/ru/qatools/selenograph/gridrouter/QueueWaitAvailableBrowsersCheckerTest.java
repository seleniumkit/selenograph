package ru.qatools.selenograph.gridrouter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import ru.qatools.gridrouter.config.Version;
import ru.yandex.qatools.camelot.test.*;

import static com.jayway.awaitility.Awaitility.await;
import static java.lang.Thread.sleep;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.IntStream.rangeClosed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * @author Ilya Sadykov
 */
@DisableTimers
@RunWith(CamelotTestRunner.class)
public class QueueWaitAvailableBrowsersCheckerTest {

    @Helper
    TestHelper helper;

    @Autowired
    QueueWaitAvailableBrowsersChecker queue;

    @PluginMock
    QueueWaitAvailableBrowsersChecker mock;

    @AggregatorState(QueueWaitAvailableBrowsersChecker.class)
    AggregatorStateStorage repo;

    @Test
    public void testCountEnqueuedRequests() throws Exception {
        final String reqId = randomUUID().toString();
        rangeClosed(0, 2).forEach(i -> queue.onWait("user", "firefox", version("33"), reqId, i));
        verify(mock, timeout(4000L).times(1)).onBeforeRequest(any(), any());
        verify(mock, timeout(4000L).times(1)).onEnqueued(any(), any());
        await().atMost(2, SECONDS).until(() -> state("user-firefox-33"), notNullValue());

        assertThat(state("user-firefox-33").size(), equalTo(1));

        queue.onWaitFinished("user", "firefox", version("33"), reqId, 3);
        verify(mock, timeout(2000L).times(1)).onDequeued(any(), any());

        await().atMost(2, SECONDS).until(() -> state("user-firefox-33").size(), equalTo(0));
    }

    @Test
    public void testExpiredRequestsRemoval() throws Exception {
        queue.onWait("user", "firefox", version("33"), "reqId", 0);
        await().atMost(2, SECONDS).until(() -> state("user-firefox-33"), notNullValue());
        assertThat(state("user-firefox-33").size(), equalTo(1));
        sleep(1000);
        helper.invokeTimersFor(QueueWaitAvailableBrowsersChecker.class);
        await().atMost(2, SECONDS).until(() -> state("user-firefox-33").size(), equalTo(0));
    }

    private WaitAvailableBrowserState state(String key) {
        return repo.getActual(key);
    }

    private Version version(String number) {
        final Version version = new Version();
        version.setNumber(number);
        return version;
    }
}