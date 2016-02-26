package ru.qatools.selenograph.gridrouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.camelot.api.AggregatorRepository;
import ru.yandex.qatools.camelot.api.ClientMessageSender;
import ru.yandex.qatools.camelot.api.Constants;
import ru.yandex.qatools.camelot.api.annotations.ClientSender;
import ru.yandex.qatools.camelot.api.annotations.Filter;
import ru.yandex.qatools.camelot.api.annotations.OnTimer;
import ru.yandex.qatools.camelot.api.annotations.Repository;
import ru.qatools.selenograph.front.BrowserSummary;

import java.util.List;
import java.util.Map;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
@Filter(instanceOf = {})
public class QuotaSummaryClientNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaSummaryClientNotifier.class);

    @ClientSender
    ClientMessageSender client;

    @Repository(QuotaSummaryAggregator.class)
    AggregatorRepository<Map<String, List<BrowserSummary>>> summaryState;

    @OnTimer(cron = "${selenograph.gridrouter.clientNotifyCron}", perState = false, skipIfNotCompleted = true)
    public void sendUpdatesToClient() {
        Map<String, List<BrowserSummary>> state = summaryState.get(Constants.Keys.ALL);
        if (state == null) {
            LOGGER.warn("no browser summaries stored, skipping client update");
            return;
        }

        LOGGER.debug("sending client updates");
        client.send(state);

        LOGGER.debug("successfully sent client updates");
    }
}
