package ru.qatools.selenograph.utils;

import ru.qatools.selenograph.*;

import java.util.UUID;

import static ru.yandex.qatools.camelot.util.DateUtil.timestamp;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public class MonitoringEventFactory {

    public static BrowserInfo browser(String name, String version) {
        return browser(name, version, 5);
    }

    public static BrowserInfo browser(String name, String version, int maxInstances) {
        final BrowserInfo browserInfo = new BrowserInfo();
        browserInfo.setName(name);
        browserInfo.setVersion(version);
        browserInfo.setMaxInstances(maxInstances);
        return browserInfo;
    }

    public static HubAlive hubAlive(String hubHost) {
        return seleniumEvent(new HubAlive(), hubHost);
    }

    public static HubAlive hubAlive(String hubName, BrowserInfo... browsers) {
        return hubAlive(hubName).withBrowsers(browsers);
    }

    public static HubStarting hubStarting() {
        return hubStarting(null);
    }

    public static HubStarting hubStarting(String hubHost) {
        return seleniumEvent(new HubStarting(), hubHost);
    }

    public static HubDown hubDown() {
        return hubDown(null);
    }

    public static HubDown hubDown(String host) {
        return seleniumEvent(new HubDown(), host);
    }

    public static BrowserStarted browserStarted() {
        return browserStarted(null, null);
    }

    public static BrowserStarted browserStarted(String hub, BrowserInfo info) {
        return browserEvent(new BrowserStarted(), info, hub);
    }

    public static SessionReleasing sessionReleasing(String hub, String sessionId, BrowserInfo info) {
        return browserEvent(new SessionReleasing(), info, hub, sessionId);
    }

    private static <T extends BrowserEvent> T browserEvent(T event, BrowserInfo info, String hubHost) {
        return browserEvent(event, info, hubHost, UUID.randomUUID().toString());
    }

    private static <T extends BrowserEvent> T browserEvent(T event, BrowserInfo info, String hubHost, String sessionId) {
        event = seleniumEvent(event, hubHost);
        event.setSessionId(sessionId);
        event.setBrowserInfo(info == null ? browser("firefox", "23") : info);
        return event;
    }

    private static <T extends SeleniumEvent> T seleniumEvent(T event, String hubHost) {
        event.setHubHost(hubHost);
        event.setHubPort(4444);
        event.setTimestamp(timestamp());
        return event;
    }
}
