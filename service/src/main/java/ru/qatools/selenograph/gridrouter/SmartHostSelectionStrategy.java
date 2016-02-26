package ru.qatools.selenograph.gridrouter;

import ru.qatools.gridrouter.config.Host;
import ru.qatools.gridrouter.config.RandomHostSelectionStrategy;
import ru.qatools.gridrouter.config.Region;
import ru.qatools.gridrouter.config.WithCount;
import ru.qatools.selenograph.front.HubSummary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toMap;
import static ru.qatools.clay.utils.DateUtil.isTimePassedSince;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public class SmartHostSelectionStrategy extends RandomHostSelectionStrategy
        implements UpdatableSelectionStrategy {

    protected static final int FALLBACK_TIMEOUT = 5 * 1000;
    protected static final int HUB_MAX_AGE = 45 * 1000;

    private long timestamp;

    /**
     * hostname -> free percentage (0 - 100)
     */
    private Map<String, Integer> hubs = emptyMap();

    //TODO split by browser and version
    public void updateHubSummaries(List<HubSummary> hubSummaries, long timestamp) {
        this.timestamp = timestamp;
        this.hubs = hubSummaries.stream()
                .filter(this::isUpdatedRecently)
                .collect(toMap(HubSummary::getAddress, this::getFreePercentage))
                .entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public Map<String, Integer> getHubs() {
        return hubs;
    }

    @Override
    public Region selectRegion(List<Region> allRegions, List<Region> unvisitedRegions) {
        return selectRandom(allRegions, this::toFreePercentage);
    }

    @Override
    public Host selectHost(List<Host> hosts) {
        return selectRandom(hosts, this::toFreePercentage);
    }

    private <T extends WithCount> T selectRandom(List<T> elements, Function<T, Integer> countMapper) {
        if (isTimePassedSince(FALLBACK_TIMEOUT, timestamp)) {
            return super.selectRandom(elements);
        }

        List<WithCount> elementsWithPercentage = new ArrayList<>(elements.size());
        Map<WithCount, T> actual = new HashMap<>(elements.size(), 1);

        for (T element : elements) {
            int count = countMapper.apply(element);
            if (count > 0) {
                WithCount withCount = () -> count;
                elementsWithPercentage.add(withCount);
                actual.put(withCount, element);
            }
        }

        if (elementsWithPercentage.isEmpty()) {
            return super.selectRandom(elements);
        }

        return actual.get(super.selectRandom(elementsWithPercentage));
    }

    public boolean isUpdatedRecently(HubSummary hub) {
        return !isTimePassedSince(HUB_MAX_AGE, hub.getTimestamp());
    }

    private int toFreePercentage(Region region) {
        return region.getHosts().stream().collect(summingInt(this::toFreePercentage));
    }

    private int toFreePercentage(Host host) {
        return hubs.getOrDefault(host.getAddress(), 0);
    }

    private int getFreePercentage(HubSummary hub) {
        return hub.getMax() == 0 ? 0 : 100 - hub.getRunning() * 100 / hub.getMax();
    }
}
