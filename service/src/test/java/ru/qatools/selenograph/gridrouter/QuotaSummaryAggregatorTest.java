package ru.qatools.selenograph.gridrouter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import ru.qatools.gridrouter.ConfigRepository;
import ru.qatools.gridrouter.config.Browsers;
import ru.yandex.qatools.camelot.api.EventProducer;
import ru.qatools.selenograph.HubAlive;
import ru.qatools.selenograph.SeleniumEvent;
import ru.qatools.selenograph.front.BrowserSummary;
import ru.qatools.selenograph.front.HubSummary;
import ru.qatools.selenograph.states.HubSummariesState;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static ru.qatools.beanloader.BeanLoader.load;
import static ru.qatools.beanloader.BeanLoaderStrategies.resource;
import static ru.qatools.selenograph.gridrouter.QuotaSummaryAggregator.toHubSummary;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
@RunWith(Parameterized.class)
public class QuotaSummaryAggregatorTest {

    private final String[] users;
    private final String[] hubs;
    private final String hubSummariesJson;
    private final String quotaSummariesJson;

    public QuotaSummaryAggregatorTest(String hubSummariesJson, String quotaSummariesJson,
                                      String[] users, String[] hubs) {
        this.hubSummariesJson = hubSummariesJson;
        this.quotaSummariesJson = quotaSummariesJson;
        this.users = users;
        this.hubs = hubs;
    }

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"hubSummary1.json", "quotaSummaries1.json",
                        new String[]{"user1", "user2"}, new String[]{"hub1", "hub2", "hub3", "hub5"}},
                {"hubSummary2.json", "quotaSummaries2.json",
                        new String[]{"user3"}, new String[]{"hub1"}},
                {"hubSummary3.json", "quotaSummaries3.json",
                        new String[]{"user4"}, new String[]{"hub2"}},
        });
    }

    private static Map<String, Browsers> readQuotaMap(String... users) throws Exception {
        return Arrays.asList(users).stream().collect(toMap(identity(), user ->
                load(Browsers.class).from(resource("quota/" + user + ".xml")).getBean()));
    }

    private static ArrayList<HubSummary> readHubSummariesFrom(String jsonFileName) {
        InputStream stream = ClassLoader.getSystemResourceAsStream("json/" + jsonFileName);
        Type listType = new TypeToken<ArrayList<HubSummary>>() {
        }.getType();
        return new Gson().fromJson(new InputStreamReader(stream), listType);
    }

    private static HashMap<String, List<BrowserSummary>> readQuotaSummariesFrom(final String jsonFileName) {
        InputStream stream = ClassLoader.getSystemResourceAsStream("json/" + jsonFileName);
        Type mapType = new TypeToken<HashMap<String, List<BrowserSummary>>>() {
        }.getType();
        return new Gson().fromJson(new InputStreamReader(stream), mapType);
    }

    private static HashMap<String, List<BrowserSummary>> transform(Map<String, Browsers> quotaMap, List<HubSummary> summaries) {
        HubSummariesState event = new HubSummariesState();
        event.getHubSummaries().addAll(summaries);

        HashMap<String, List<BrowserSummary>> state = new HashMap<>();

        QuotaSummaryAggregator aggregator = new QuotaSummaryAggregator();
        aggregator.config = Mockito.mock(ConfigRepository.class);
        Mockito.when(aggregator.config.getQuotaMap()).thenReturn(quotaMap);

        aggregator.out = Mockito.mock(EventProducer.class, Mockito.RETURNS_DEFAULTS);

        aggregator.updateQuotaSummary(state, event);

        return state;
    }

    @Test
    public void testQuotaSummary() throws Exception {
        Map<String, Browsers> quotas = readQuotaMap(users);
        ArrayList<HubSummary> hub = readHubSummariesFrom(hubSummariesJson);
        HashMap<String, List<BrowserSummary>> quotaBrowsersActual = transform(quotas, hub);
        HashMap<String, List<BrowserSummary>> quotaBrowsersExpected = readQuotaSummariesFrom(quotaSummariesJson);
        assertThat(quotaBrowsersActual, is(equalTo(quotaBrowsersExpected)));
    }

    @Test
    public void testHubsSummary() throws Exception {
        Map<String, HubAlive> hubAlives = toHubSummary(readQuotaMap(users));
        assertThat(hubAlives.values().stream().map(SeleniumEvent::getHubHost)
                .collect(Collectors.toList()), containsInAnyOrder(hubs));
    }
}
