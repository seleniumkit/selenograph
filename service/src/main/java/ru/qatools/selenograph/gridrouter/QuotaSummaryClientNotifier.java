package ru.qatools.selenograph.gridrouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.selenograph.ext.SelenographDB;
import ru.yandex.qatools.camelot.api.ClientMessageSender;
import ru.yandex.qatools.camelot.api.annotations.ClientSender;
import ru.yandex.qatools.camelot.api.annotations.Filter;
import ru.yandex.qatools.camelot.api.annotations.OnTimer;

import javax.inject.Inject;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
@Filter(instanceOf = {})
public class QuotaSummaryClientNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaSummaryClientNotifier.class);
    @Inject
    SelenographDB database;

    @ClientSender
    ClientMessageSender client;

    @OnTimer(cron = "${selenograph.gridrouter.clientNotifyCron}", perState = false, skipIfNotCompleted = true)
    public void sendUpdatesToClient() {
        LOGGER.debug("sending client updates");
        client.send(database.getQuotasSummary());
        LOGGER.debug("successfully sent client updates");
    }

}
