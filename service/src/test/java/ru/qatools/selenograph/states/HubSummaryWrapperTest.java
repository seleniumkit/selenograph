package ru.qatools.selenograph.states;

import org.junit.Before;
import org.junit.Test;
import ru.qatools.selenograph.front.BrowserSummary;
import ru.qatools.selenograph.front.HubSummary;
import ru.qatools.selenograph.front.HubSummaryWrapper;
import ru.qatools.selenograph.front.VersionSummary;
import ru.yandex.qatools.camelot.api.Storage;
import ru.yandex.qatools.camelot.common.LocalMemoryStorage;

import java.util.Iterator;

import static java.util.Collections.sort;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.qatools.selenograph.utils.MonitoringEventFactory.browser;
import static ru.qatools.selenograph.utils.MonitoringEventFactory.browserStarted;
import static ru.qatools.selenograph.utils.SelenographBeansFactory.hub;
import static ru.qatools.selenograph.utils.SelenographBeansFactory.hubBrowserStorage;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 */
public class HubSummaryWrapperTest {

    protected Storage<HubBrowserState> repo;

    @Before
    public void prepare() {
        repo = new LocalMemoryStorage<>(hubBrowserStorage(
                browserStarted("hub1", browser("chrome", "33.0")),
                browserStarted("hub1", browser("chrome", "33.0")),
                browserStarted("hub1", browser("firefox", "25.0")),
                browserStarted("hub1", browser("firefox", "35.0")),
                browserStarted("hub1", browser("firefox", "35.0")),
                browserStarted("hub2", browser("firefox", "25.0")),
                browserStarted("hub2", browser("phantomjs", "1.0"))
        ));
    }

    @Test
    public void testBuildHubSummary() {
        HubSummary hub = new HubSummaryWrapper(hub("hub1", 4444,
                browser("firefox", "25", 7),
                browser("chrome", "33", 3),
                browser("firefox", "35", 4),
                browser("firefox", "35", 11)
        ), repo.valuesMap()).getBean();

        assertThat(hub.getAddress(), is("hub1:4444"));
        assertThat(hub.getBrowsers().size(), is(2));
        assertThat(hub.getMax(), is(25));

        sortBrowsers(hub);

        Iterator<BrowserSummary> browsersIterator = hub.getBrowsers().iterator();
        BrowserSummary browser;

        browser = browsersIterator.next();
        assertBrowser(browser, "chrome", 1);
        assertVersion(browser, 0, "33.0", 2, 3);

        browser = browsersIterator.next();
        assertBrowser(browser, "firefox", 2);
        assertVersion(browser, 0, "25.0", 1, 7);
        assertVersion(browser, 1, "35.0", 2, 15);
    }

    public void assertVersion(BrowserSummary browser, int index,
                              String version, int running, int max) {
        VersionSummary versionSummary = browser.getVersions().get(index);
        assertThat(versionSummary.getVersion(), is(version));
        assertThat(versionSummary.getRunning(), is(running));
        assertThat(versionSummary.getMax(), is(max));
    }

    public void assertBrowser(BrowserSummary browser,
                              String name, int versions) {
        assertThat(browser.getName(), is(name));
        assertThat(browser.getVersions().size(), is(versions));
        sortVersions(browser);
    }

    public void sortBrowsers(HubSummary hub) {
        sort(hub.getBrowsers(), (o1, o2) -> o1.getName().compareTo(o2.getName()));
    }

    public void sortVersions(BrowserSummary browser) {
        sort(browser.getVersions(), (o1, o2) -> o1.getVersion().compareTo(o2.getVersion()));
    }
}
