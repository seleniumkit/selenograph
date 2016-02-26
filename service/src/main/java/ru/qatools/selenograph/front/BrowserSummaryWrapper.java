package ru.qatools.selenograph.front;

import ru.qatools.selenograph.BrowserInfo;

import java.util.HashMap;

import static java.lang.Math.max;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toList;
import static ru.qatools.selenograph.util.Key.browserName;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
class BrowserSummaryWrapper {

    private final BrowserSummary bean = new BrowserSummary();

    private final HashMap<String, VersionSummaryWrapper> versions = new HashMap<>();

    public BrowserSummaryWrapper(BrowserInfo info) {
        bean.setName(browserName(info));
    }

    protected String id() {
        return bean.getName();
    }

    public void add(String version, int maxInstances, int runningCount) {
        this.increase(maxInstances, runningCount);
        versions.putIfAbsent(version, new VersionSummaryWrapper(version));
        versions.get(version).increase(maxInstances, runningCount);
    }

    private void increase(int maxInstances, int runningCount) {
        bean.setMax(bean.getMax() + maxInstances);
        bean.setRunning(bean.getRunning() + runningCount);
    }

    public int getRunningCount() {
        return versions.values().stream().collect(summingInt(VersionSummaryWrapper::getRunningCount));
    }

    public void calcOccupied(int nodeFreeCount) {
        for (VersionSummaryWrapper version : versions.values()) {
            version.calcOccupied(nodeFreeCount);
        }
        int browserFreeCount = bean.getMax() - bean.getRunning();
        bean.setOccupied(max(0, browserFreeCount - nodeFreeCount));
    }

    public BrowserSummary getBean() {
        bean.getVersions().addAll(
                versions.values().stream()
                        .map(VersionSummaryWrapper::getBean)
                        .collect(toList()));
        return bean;
    }

    @Override
    @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        BrowserSummaryWrapper that = (BrowserSummaryWrapper) o;

        if (!id().equals(that.id()))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id().hashCode();
    }
}
