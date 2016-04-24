package ru.qatools.selenograph.api;

import org.apache.camel.CamelContext;
import org.apache.camel.component.seda.QueueReference;
import org.apache.camel.component.seda.SedaComponent;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.ok;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
@Path("/")
public class InputResource {
    private static final AtomicLong MESSAGES_COUNT = new AtomicLong();

    @Inject
    CamelContext camelContext;

    @PUT
    @Path("/events")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response sendMessage(String message) {
        MESSAGES_COUNT.incrementAndGet();
        return ok("ok").build();
    }

    @GET
    @Path("/perform")
    public Response resetDelayedQueues(@QueryParam("action") String action, @QueryParam("object") String object) {
        switch (action) {
            case "clearQueue":
                getSedaQueues().get(object).getQueue().clear();
                break;
        }
        return ok("ok").build();
    }

    private Map<String, QueueReference> getSedaQueues() {
        return camelContext.getComponent("seda", SedaComponent.class).getQueues();
    }


    @GET
    @Path("/events/count")
    @Produces({TEXT_PLAIN})
    public Long getCount() {
        return MESSAGES_COUNT.get();
    }
}
