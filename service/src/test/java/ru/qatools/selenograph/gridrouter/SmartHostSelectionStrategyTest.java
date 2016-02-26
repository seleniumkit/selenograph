package ru.qatools.selenograph.gridrouter;

import org.junit.Before;
import org.junit.Test;
import ru.qatools.gridrouter.config.Host;
import ru.qatools.gridrouter.config.Region;
import ru.qatools.selenograph.front.HubSummary;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.qatools.selenograph.gridrouter.SmartHostSelectionStrategy.FALLBACK_TIMEOUT;
import static ru.qatools.selenograph.gridrouter.SmartHostSelectionStrategy.HUB_MAX_AGE;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public class SmartHostSelectionStrategyTest {

    private static final Host HOST_1 = new Host("host1", 4444, 5);
    private static final Host HOST_2 = new Host("host2", 4444, 5);
    private static final Host HOST_3 = new Host("host3", 4444, 5);

    private static final Region REGION_1 = new Region(asList(HOST_1), "region1");
    private static final Region REGION_2 = new Region(asList(HOST_2), "region2");

    private static final HubSummary SUMMARY_1 = hubSummary("host1:4444", 5, 5, currentTimeMillis());
    private static final HubSummary SUMMARY_2 = hubSummary("host2:4444", 1, 5, currentTimeMillis());

    private SmartHostSelectionStrategy strategy;

    @Before
    public void setUp() throws Exception {
        strategy = new SmartHostSelectionStrategy();
    }

    @Test
    public void testSelectWithEmptySummariesMap() {
        assertThat(strategy.selectHost(singletonList(HOST_1)), is(equalTo(HOST_1)));
    }

    @Test
    public void testSelectWithNonMatchingSummaries() {
        strategy.updateHubSummaries(singletonList(SUMMARY_2), currentTimeMillis());
        assertThat(strategy.selectHost(singletonList(HOST_1)), is(equalTo(HOST_1)));
    }

    @Test
    public void testSelectWithMatchingSummaries() {
        strategy.updateHubSummaries(asList(SUMMARY_1, SUMMARY_2), currentTimeMillis());
        Host actual = strategy.selectHost(asList(HOST_1, HOST_2, HOST_3));
        assertThat(actual, is(equalTo(HOST_2)));
    }

    @Test
    public void testSelectRegion() {
        strategy.updateHubSummaries(singletonList(SUMMARY_2), currentTimeMillis());

        List<Region> allRegions = asList(REGION_1, REGION_2).stream().map(Region::copy).collect(toList());
        List<Region> unvisitedRegions = new ArrayList<>(allRegions);

        Region actual = strategy.selectRegion(allRegions, unvisitedRegions);
        assertThat(actual, is(equalTo(allRegions.get(1))));

        unvisitedRegions.remove(1);
        actual = strategy.selectRegion(allRegions, unvisitedRegions);
        assertThat(actual, is(equalTo(allRegions.get(1))));

        allRegions.get(1).getHosts().clear();
        actual = strategy.selectRegion(allRegions, unvisitedRegions);
        assertThat(actual, is(equalTo(allRegions.get(0))));
    }

    @Test
    public void testOutdatedUpdateTimestamp() {
        strategy.updateHubSummaries(asList(SUMMARY_2), currentTimeMillis() - 2 * FALLBACK_TIMEOUT);
        Host host2WithZeroCount = new Host(HOST_2.getName(), HOST_2.getPort(), 0);
        Host actual = strategy.selectHost(asList(HOST_1, host2WithZeroCount));
        assertThat(actual, is(equalTo(HOST_1)));
    }

    @Test
    public void testOutdatedHubTimestamp() {
        HubSummary summary2WithOutdatedTimestamp = SUMMARY_2;
        summary2WithOutdatedTimestamp.setTimestamp(currentTimeMillis() - 2 * HUB_MAX_AGE);
        strategy.updateHubSummaries(asList(summary2WithOutdatedTimestamp), currentTimeMillis());
        Host host2WithZeroCount = new Host(HOST_2.getName(), HOST_2.getPort(), 0);
        Host actual = strategy.selectHost(asList(HOST_1, host2WithZeroCount));
        assertThat(actual, is(equalTo(HOST_1)));
    }

    private static HubSummary hubSummary(String hub2, int running, int max, long timestamp) {
        HubSummary summary = new HubSummary();
        summary.setTimestamp(timestamp);
        summary.setAddress(hub2);
        summary.setRunning(running);
        summary.setMax(max);
        return summary;
    }
}
