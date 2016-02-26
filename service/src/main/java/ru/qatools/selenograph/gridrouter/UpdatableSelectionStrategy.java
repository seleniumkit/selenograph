package ru.qatools.selenograph.gridrouter;

import ru.qatools.selenograph.front.HubSummary;

import java.util.List;
import java.util.Map;

/**
 * @author Ilya Sadykov
 */
public interface UpdatableSelectionStrategy {
    long getTimestamp();

    Map<String, Integer> getHubs();

    void updateHubSummaries(List<HubSummary> hubSummaries, long timestamp);
}
