package ru.qatools.selenograph.plugins;

import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.qatools.camelot.test.CamelotTestRunner;
import ru.yandex.qatools.camelot.test.DisableTimers;
import ru.yandex.qatools.camelot.test.PluginMock;
import ru.qatools.selenograph.BrowserStarted;
import ru.qatools.selenograph.HubStarting;
import ru.qatools.selenograph.states.HubState;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static ru.qatools.selenograph.utils.MonitoringEventFactory.browserStarted;
import static ru.qatools.selenograph.utils.MonitoringEventFactory.hubStarting;

@RunWith(CamelotTestRunner.class)
@DisableTimers
public class HubStateAggregatorTest extends BasePluginTest {

    @PluginMock
    HubStateAggregator mock;

    @Test
    public void testHubStateAggregator() {
        HubStarting hubStarting = hubStarting();
        BrowserStarted browserStarted = browserStarted();
        send(hubStarting);
        send(browserStarted);
        verify(mock, timeout(TIMEOUT))
                .onHubStarting(any(HubState.class), any(HubStarting.class));
    }
}
