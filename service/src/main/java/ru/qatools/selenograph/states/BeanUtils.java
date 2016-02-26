package ru.qatools.selenograph.states;

import ru.qatools.selenograph.HubDown;
import ru.qatools.selenograph.SeleniumEvent;
import ru.qatools.selenograph.StopCorrelationKey;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public abstract class BeanUtils {

    public static StopCorrelationKey stop(String correlationKey) {
        StopCorrelationKey event = new StopCorrelationKey();
        event.setCorrelationKey(correlationKey);
        return event;
    }

    private static <T extends SeleniumEvent> T seleniumEvent(T event, String hubName, String hubHost, int hubPort, long timestamp) {
        event.setHubName(hubName);
        event.setHubHost(hubHost);
        event.setHubPort(hubPort);
        event.setTimestamp(timestamp);
        return event;
    }

    public static HubDown hubDown(HubState state) {
        return seleniumEvent(new HubDown(), state.getName(), state.getHostname(), state.getPort(), state.getTimestamp());
    }
}
