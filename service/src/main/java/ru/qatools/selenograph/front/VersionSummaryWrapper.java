package ru.qatools.selenograph.front;

import static java.lang.Math.max;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
class VersionSummaryWrapper {

    private final VersionSummary bean = new VersionSummary();

    public VersionSummaryWrapper(String version) {
        bean.setVersion(version);
    }

    public void increase(int max, int count) {
        bean.setMax(bean.getMax() + max);
        bean.setRunning(bean.getRunning() + count);
    }

    public int getRunningCount() {
        return bean.getRunning();
    }

    public void calcOccupied(int nodeFreeCount) {
        int versionFreeCount = bean.getMax() - bean.getRunning();
        bean.setOccupied(max(0, versionFreeCount - nodeFreeCount));
    }

    public VersionSummary getBean() {
        return bean;
    }
}
