package ru.qatools.selenograph.plugins;

import ru.yandex.qatools.camelot.api.AggregatorRepository;
import ru.yandex.qatools.camelot.api.Constants;
import ru.yandex.qatools.camelot.api.PluginsInterop;
import ru.yandex.qatools.camelot.api.annotations.Plugins;
import ru.yandex.qatools.camelot.api.annotations.Repository;
import ru.qatools.selenograph.front.HubSummary;
import ru.qatools.selenograph.states.HubSummariesState;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
@Path("/selenograph")
public class ApiResource {

    @Plugins
    PluginsInterop plugins;

    @Repository(HubSummaryAggregator.class)
    AggregatorRepository<HubSummariesState> hubSummaryRepository;

    @GET
    @Path("/hubs")
    @Produces({APPLICATION_JSON})
    public List<HubSummary> getHubs() throws IOException {
        return hubSummaryRepository.get(Constants.Keys.ALL).getHubSummaries();
    }

    @GET
    @Path("/count/{pluginId}")
    @Produces({APPLICATION_JSON})
    public int getCount(@PathParam("pluginId") String pluginId) {
        return plugins.repo(pluginId).keys().size();
    }

    @GET
    @Path("/map/{pluginId}")
    @Produces({APPLICATION_JSON})
    public Map getMap(@PathParam("pluginId") String pluginId) {
        return plugins.repo(pluginId).valuesMap();
    }

    @GET
    @Path("/list/{pluginId}")
    @Produces({APPLICATION_JSON})
    public Collection getList(@PathParam("pluginId") String pluginId) {
        return getMap(pluginId).values();
    }

    @GET
    @Path("/map/{pluginId}/{key}")
    @Produces({APPLICATION_JSON})
    public Object getState(@PathParam("pluginId") String pluginId, @PathParam("key") String key) {
        return plugins.repo(pluginId).get(key);
    }
}
