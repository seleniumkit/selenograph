package ru.qatools.selenograph.gridrouter;

import org.apache.commons.lang3.StringUtils;

import static java.lang.Float.parseFloat;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public class Key {

    private Key() {
    }

    public static String browserVersion(String version) {
        return StringUtils.isNumeric(version) ? String.valueOf(parseFloat(version)) : version;
    }

    public static String browserName(String name) {
        return name.contains("-") ? name.split("\\-")[0] : name;
    }

    public static String byBrowserVersion(String browserName, String version) {
        return browserName(browserName) + ":" + browserVersion(version);
    }
}
