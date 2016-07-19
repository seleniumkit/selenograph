package ru.qatools.selenograph.gridrouter;

import org.springframework.beans.factory.annotation.Autowired;
import ru.qatools.gridrouter.config.HostSelectionStrategy;
import ru.qatools.selenograph.ext.SelenographDB;
import ru.yandex.qatools.camelot.api.AggregatorRepository;
import ru.yandex.qatools.camelot.api.annotations.Repository;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static ru.yandex.qatools.camelot.util.MapUtil.map;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
@Path("/selenograph")
public class ApiResource {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM,dd HH:mm:ss.SSS");

    @Repository(QueueWaitAvailableBrowsersChecker.class)
    AggregatorRepository<WaitAvailableBrowserState> queueRepo;

    @Autowired
    SelenographDB database;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    private HostSelectionStrategy strategy;

    @GET
    @Path("/strategy")
    @Produces({APPLICATION_JSON})
    public StrategyData getStrategy() throws IOException {
        return new StrategyData(strategy);
    }

    @GET
    @Path("/queues")
    @Produces({APPLICATION_JSON})
    public List<Map> getQueues() throws IOException {
        return queueRepo.valuesMap().entrySet().stream().map(e ->
                map(e.getKey(), map(
                        "browser", e.getValue().getBrowser(),
                        "version", e.getValue().getVersion(),
                        "user", e.getValue().getUser(),
                        "count", e.getValue().size()
                ))).collect(toList());
    }

    @GET
    @Path("/quotas")
    @Produces({APPLICATION_JSON})
    public Map<String, BrowserSummaries> getQuotas() throws IOException {
        return database.getQuotasSummary();
    }

    @GET
    @Path("/quota/{quotaName}")
    @Produces({APPLICATION_JSON})
    public BrowserSummaries getQuota(@PathParam("quotaName") String quotaName) throws IOException {
        return database.getQuotasSummary().get(quotaName);
    }

    private class StrategyData {
        public final String lastUpdated;
        public final Map<String, Integer> hubs;

        public StrategyData(HostSelectionStrategy strategy) {
            if (strategy != null && strategy instanceof UpdatableSelectionStrategy) {
                final UpdatableSelectionStrategy smartStrategy = ((UpdatableSelectionStrategy) strategy);
                lastUpdated = DATE_FORMAT.format(new Date(smartStrategy.getTimestamp()));
                hubs = smartStrategy.getHubs();
            } else {
                lastUpdated = "NOT APPLICABLE";
                hubs = emptyMap();
            }
        }
    }
}
