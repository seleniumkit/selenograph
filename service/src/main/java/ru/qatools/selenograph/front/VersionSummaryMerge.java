package ru.qatools.selenograph.front;

import java.util.Map;
import java.util.stream.Collector;

import static java.util.stream.Collectors.groupingBy;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public class VersionSummaryMerge {

    public static final Collector<VersionSummaryMerge, ?, Map<String, VersionSummaryMerge>> MERGE_COLLECTOR
            = groupingBy(VersionSummaryMerge::getId, Collector.of(
                    VersionSummaryMerge::new,
                    VersionSummaryMerge::merge,
                    VersionSummaryMerge::merge
            ));

    private final VersionSummary bean;

    public VersionSummaryMerge(VersionSummary bean) {
        this.bean = bean;
    }

    public VersionSummaryMerge() {
        this(new VersionSummary());
    }

    public VersionSummary getBean() {
        return bean;
    }

    public String getId() {
        return bean.getVersion();
    }

    public VersionSummaryMerge merge(VersionSummaryMerge that) {
        this.bean.setVersion(that.bean.getVersion());
        this.bean.setMax(this.bean.getMax() + that.bean.getMax());
        this.bean.setRunning(this.bean.getRunning() + that.bean.getRunning());
        this.bean.setOccupied(this.bean.getOccupied() + that.bean.getOccupied());
        return this;
    }
}
