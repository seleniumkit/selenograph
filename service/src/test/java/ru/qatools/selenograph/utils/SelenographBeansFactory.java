package ru.qatools.selenograph.utils;

import ru.qatools.selenograph.BrowserEvent;
import ru.qatools.selenograph.BrowserInfo;
import ru.qatools.selenograph.states.HubBrowserState;
import ru.qatools.selenograph.states.HubState;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static ru.qatools.selenograph.util.Key.byHubBrowser;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public class SelenographBeansFactory {

    public static HubState hub(String name, int port, BrowserInfo... browsers) {
        HubState hub = new HubState(name, port, asList(browsers));
        hub.alive();
        return hub;
    }

    public static Map<String, HubBrowserState> hubBrowserStorage(BrowserEvent... startingEvents) {
        Map<String, HubBrowserState> result = new HashMap<>();
        for (BrowserEvent event : startingEvents) {
            String aggKey = byHubBrowser(event.getHubHost(), event.getHubPort(), event.getBrowserInfo());
            if (!result.containsKey(aggKey)) {
                HubBrowserState state = new HubBrowserState();
                result.put(aggKey, state);
            }
            result.get(aggKey).startSession(event.getSessionId(), event.getTimestamp());
        }
        return result;
    }
}
