package ru.qatools.selenograph.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.camelot.api.AggregatorRepository;
import ru.yandex.qatools.camelot.api.ClientMessageSender;
import ru.yandex.qatools.camelot.api.Constants;
import ru.yandex.qatools.camelot.api.annotations.ClientSender;
import ru.yandex.qatools.camelot.api.annotations.Filter;
import ru.yandex.qatools.camelot.api.annotations.OnTimer;
import ru.yandex.qatools.camelot.api.annotations.Repository;
import ru.qatools.selenograph.states.HubSummariesState;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
@Filter(instanceOf = {})
public class HubSummaryClientNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(HubSummaryClientNotifier.class);

    @ClientSender
    ClientMessageSender client;

    @Repository(HubSummaryAggregator.class)
    AggregatorRepository<HubSummariesState> summaryState;

    @OnTimer(cron = "*/7 * * * * ?", perState = false, skipIfNotCompleted = true)
    public void sendUpdatesToClient() {
        HubSummariesState state = summaryState.get(Constants.Keys.ALL);
        if (state == null) {
            LOGGER.debug("no hub summaries stored, skipping client update");
            return;
        }

        LOGGER.debug("sending client updates");
        //noinspection ConstantConditions
        state.getHubSummaries().stream().forEach(hub -> {
            LOGGER.debug("{} - sending update to client", hub.getAddress());
            client.send(hub);
        });
        LOGGER.debug("successfully sent client updates");
    }
}
