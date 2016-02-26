package ru.qatools.selenograph.util;

import ru.qatools.selenograph.BrowserInfo;
import ru.qatools.selenograph.SeleniumEvent;

import static java.lang.Float.parseFloat;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNumeric;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public class Key {

    private Key() {
    }

    public static String byHubBrowser(String hubHost, int hubPort, BrowserInfo browser) {
        return byHubBrowser(hubHost + ":" + hubPort, browser);
    }

    public static String byHubBrowser(String hubAddress, BrowserInfo browser) {
        return format("%s|%s|%s", hubAddress, browserName(browser), browserVersion(browser));
    }

    public static String browserVersion(BrowserInfo browser) {
        return browserVersion(browser.getVersion());
    }

    public static String browserVersion(String version) {
        return isNumeric(version) ? String.valueOf(parseFloat(version)) : version;
    }

    public static String browserName(BrowserInfo browser) {
        return browserName(browser.getName());
    }

    public static String browserName(String name) {
        return name.contains("-") ? name.split("\\-")[0] : name;
    }

    public static String byNode(String hubHost, int hubPort, String nodeAddress) {
        return format("%s:%d|%s", hubHost, hubPort, nodeAddress);
    }

    public static String hubAddress(SeleniumEvent event) {
        return buildAddress(event.getHubHost(), event.getHubPort());
    }

    public static String buildAddress(String host, int port) {
        return host + ":" + port;
    }
}
