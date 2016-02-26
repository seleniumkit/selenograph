package ru.qatools.selenograph.gridrouter;

import ru.qatools.selenograph.front.BrowserSummary;
import ru.qatools.selenograph.front.BrowserSummaryMerge;
import ru.qatools.selenograph.front.HubSummary;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static ru.qatools.selenograph.front.BrowserSummaryMerge.MERGE_COLLECTOR;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public class HubBrowserSummariesMap {

    private final Map<String, List<BrowserSummary>> hubs;

    public HubBrowserSummariesMap(List<HubSummary> hubSummaries) {
        hubs = hubSummaries.stream().collect(toMap(
                HubSummary::getAddress,
                hub -> hub.getBrowsers().stream()
                        .map(BrowserSummaryMerge::new)
                        .collect(MERGE_COLLECTOR).values().stream()
                        .map(BrowserSummaryMerge::getBean)
                        .collect(toList())));
    }

    public List<BrowserSummary> getAllBrowsers() {
        return hubs.values().stream().flatMap(Collection::stream)
                .map(BrowserSummaryMerge::new)
                .collect(MERGE_COLLECTOR).values().stream()
                .map(BrowserSummaryMerge::getBean)
                .sorted(comparing(BrowserSummary::getName))
                .collect(toList());
    }

    public Stream<BrowserSummary> get(String hubAddress) {
        return hubs.getOrDefault(hubAddress, emptyList()).stream();
    }
}
