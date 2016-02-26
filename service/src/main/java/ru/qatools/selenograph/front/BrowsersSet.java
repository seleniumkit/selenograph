package ru.qatools.selenograph.front;

import ru.qatools.selenograph.BrowserInfo;
import ru.qatools.selenograph.states.HubBrowserState;
import ru.qatools.selenograph.states.HubState;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toList;
import static ru.qatools.selenograph.util.Key.*;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
class BrowsersSet {

    private final HashMap<String, BrowserSummaryWrapper> map = new HashMap<>();

    public BrowsersSet(String hubAddress, HubState hubState, Map<String, HubBrowserState> storage) {
        final List<BrowserInfo> browsers = hubState.getBrowsers().stream().map(b -> new BrowserInfo()
                .withName(browserName(b.getName())).withVersion(browserVersion(b.getVersion()))
                .withMaxInstances(b.getMaxInstances())).collect(toList());
        browsers.forEach(browser -> add(browser, 0));
        browsers.stream().map(BrowserInfoWrapper::new).distinct().map(BrowserInfoWrapper::unwrap).forEach(browser ->
                add(browser.withMaxInstances(0),
                        storage.entrySet().stream().filter(s -> s.getKey().equals(byHubBrowser(hubAddress, browser)))
                        .collect(summingInt(s -> s.getValue().getRunningCount()))));
    }

    public void add(BrowserInfo info, int count) {
        BrowserSummaryWrapper browser = new BrowserSummaryWrapper(info);
        map.putIfAbsent(browser.id(), browser);
        map.get(browser.id()).add(browserVersion(info), info.getMaxInstances(), count);
    }

    public int getRunningCount() {
        return map.values().stream().collect(summingInt(BrowserSummaryWrapper::getRunningCount));
    }

    public void calcOccupied(int nodeFreeCount) {
        for (BrowserSummaryWrapper browser : map.values()) {
            browser.calcOccupied(nodeFreeCount);
        }
    }

    public Collection<BrowserSummary> getBeans() {
        return map.values().stream()
                .map(BrowserSummaryWrapper::getBean)
                .collect(toList());
    }
}
