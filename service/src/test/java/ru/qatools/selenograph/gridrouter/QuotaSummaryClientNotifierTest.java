package ru.qatools.selenograph.gridrouter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import ru.qatools.selenograph.front.BrowserSummary;
import ru.qatools.selenograph.front.VersionSummary;
import ru.yandex.qatools.camelot.api.ClientMessageSender;
import ru.yandex.qatools.camelot.test.*;

import java.util.List;
import java.util.Map;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static ru.qatools.selenograph.ext.SelenographDB.ALL;

/**
 * @author Ilya Sadykov
 */
@SuppressWarnings("unchecked")
@RunWith(CamelotTestRunner.class)
@DisableTimers
public class QuotaSummaryClientNotifierTest extends QuotaStatsAggregatorTest {

    @Helper
    TestHelper helper;

    @ClientSenderMock(QuotaSummaryClientNotifier.class)
    ClientMessageSender frontend;

    @Test
    public void sendSummaryToClient() throws Exception {
        String sessionId1 = startSessionFor("user1", "firefox", "32.0");
        String sessionId2 = startSessionFor("user2", "chrome", "32.0");
        String sessionId3 = startSessionFor("user3", "firefox", "33.0");
        await().atMost(4, SECONDS).until(() -> sessions.getActiveSessions(),
                hasItems(sessionId1, sessionId2, sessionId3));
        reset(frontend);
        helper.invokeTimersFor(QuotaSummaryClientNotifier.class);
        final ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
        verify(frontend, timeout(1000)).send(argument.capture());
        final Map<String, BrowserSummaries> sent = argument.getValue();

        assertThat(sent.get(ALL), hasSize(2));
        final BrowserSummary allFirefox = sent.get(ALL).stream()
                .filter(s -> s.getName().equals("firefox")).findFirst().orElseThrow(RuntimeException::new);
        assertEquals(4, allFirefox.getMax());
        assertEquals(2, allFirefox.getRunning());
        assertThat(allFirefox.getVersions(), hasSize(2));
        final VersionSummary allFirefox32 = allFirefox.getVersions().stream()
                .filter(v -> v.getVersion().equals("32.0")).findFirst().orElseThrow(RuntimeException::new);
        assertEquals(3, allFirefox32.getMax());
        assertEquals(1, allFirefox32.getRunning());

        assertThat(sent.get("user1"), hasSize(1));
        assertEquals(2, sent.get("user1").get(0).getMax());
        assertEquals(1, sent.get("user1").get(0).getRunning());
        final List<VersionSummary> user1Versions = sent.get("user1").get(0).getVersions();
        assertThat(user1Versions, hasSize(1));
        assertEquals(2, user1Versions.get(0).getMax());
        assertEquals(1, user1Versions.get(0).getRunning());

        assertThat(sent.get("user2"), hasSize(2));
        final BrowserSummary user2Chrome = sent.get("user2").stream()
                .filter(s -> s.getName().equals("chrome")).findFirst().orElseThrow(RuntimeException::new);
        assertEquals(1, user2Chrome.getMax());
        assertEquals(1, user2Chrome.getRunning());
        final BrowserSummary user2Firefox = sent.get("user2").stream()
                .filter(s -> s.getName().equals("firefox")).findFirst().orElseThrow(RuntimeException::new);
        assertEquals(1, user2Firefox.getMax());
        assertEquals(0, user2Firefox.getRunning());
        assertThat(user2Firefox.getVersions(), hasSize(1));
        assertEquals(1, user2Firefox.getVersions().get(0).getMax());
        assertEquals(0, user2Firefox.getVersions().get(0).getRunning());

        assertThat(sent.get("user3"), hasSize(1));
        final BrowserSummary user3Firefox = sent.get("user3").get(0);
        assertEquals(2, user3Firefox.getMax());
        assertEquals(1, user3Firefox.getRunning());
        assertThat(user3Firefox.getVersions(), hasSize(2));
        final VersionSummary user3Firefox33 = user3Firefox.getVersions().stream()
                .filter(v -> v.getVersion().equals("33.0")).findFirst().orElseThrow(RuntimeException::new);
        assertEquals(1, user3Firefox33.getMax());
        assertEquals(1, user3Firefox33.getRunning());

    }
}