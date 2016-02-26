package ru.qatools.selenograph.utils;

import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;

import static ru.yandex.qatools.properties.utils.PropertiesUtils.readProperties;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
@SuppressWarnings("FieldCanBeLocal")
public class TestProperties {

    public TestProperties() {
        PropertyLoader.populate(this);
    }

    public TestProperties(String resourceName) {
        PropertyLoader.populate(this, readProperties(ClassLoader.getSystemResourceAsStream(resourceName)));
    }

    @Property("selenograph.test.timeout.in.seconds")
    private int timeout = 3;

    @Property("selenograph.browserStartsHistoryLength")
    private int browserStartsHistory;

    @Property("selenograph.criticalBrowserStartFailuresPercentage")
    private int criticalBrowserStartFailuresPercentage;

    @Property("selenograph.openstack.wait.after.termination")
    private int waitAfterTermination;

    /**
     * @return timeout in milliseconds
     */
    public int getTimeout() {
        return timeout * 1000;
    }

    public int getBrowserStartsHistory() {
        return browserStartsHistory;
    }

    public int getCriticalBrowserStartFailuresPercentage() {
        return criticalBrowserStartFailuresPercentage;
    }

    public int getWaitAfterTermination() {
        return waitAfterTermination;
    }
}
