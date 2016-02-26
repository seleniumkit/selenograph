package ru.qatools.selenograph.front;

import ru.qatools.selenograph.BrowserInfo;
import ru.qatools.selenograph.states.HubBrowserState;
import ru.qatools.selenograph.states.HubState;

import java.util.Map;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.summingInt;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public class HubSummaryWrapper {

    private final HubSummary bean = new HubSummary();

    public HubSummaryWrapper(HubState state, Map<String, HubBrowserState> storage) {
        bean.setTimestamp(state.getTimestamp());
        bean.setAddress(state.getAddress());
        bean.setActive(state.isActive());
        BrowsersSet browsers = new BrowsersSet(state.getAddress(), state, storage);
        bean.getBrowsers().addAll(browsers.getBeans());
        bean.getBrowsers().sort(comparing(BrowserSummary::getName));
        bean.setRunning(browsers.getRunningCount());
        bean.setMax(bean.getMax() + state.getBrowsers().stream().collect(summingInt(BrowserInfo::getMaxInstances)));
        browsers.calcOccupied(bean.getMax() - bean.getRunning());
    }

    public HubSummary getBean() {
        return bean;
    }
}
