package ru.qatools.selenograph.front;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public class BrowserSummaryMerge {

    public static final Collector<BrowserSummaryMerge, ?, Map<String, BrowserSummaryMerge>> MERGE_COLLECTOR
            = groupingBy(bs -> bs.getBean().getName(), Collector.of(
                    BrowserSummaryMerge::new,
                    BrowserSummaryMerge::merge,
                    BrowserSummaryMerge::merge
            ));

    private final BrowserSummary bean;

    public BrowserSummaryMerge(BrowserSummary bean) {
        this.bean = bean;
    }

    private BrowserSummaryMerge() {
        this(new BrowserSummary());
    }

    public BrowserSummary getBean() {
        return bean;
    }

    public BrowserSummaryMerge merge(BrowserSummaryMerge that) {
        this.bean.setName(that.bean.getName());
        this.bean.setRunning(this.bean.getRunning() + that.bean.getRunning());
        this.bean.setMax(this.bean.getMax() + that.bean.getMax());
        this.bean.setOccupied(this.bean.getOccupied() + that.bean.getOccupied());

        Collection<VersionSummary> versions = Stream.of(this.bean.getVersions(), that.bean.getVersions())
                .flatMap(Collection::stream)
                .map(VersionSummaryMerge::new)
                .collect(VersionSummaryMerge.MERGE_COLLECTOR).values().stream()
                .map(VersionSummaryMerge::getBean)
                .collect(toList());

        this.bean.getVersions().clear();
        this.bean.getVersions().addAll(versions);

        return this;
    }
}
