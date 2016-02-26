package ru.qatools.selenograph.gridrouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.qatools.gridrouter.config.HostSelectionStrategy;
import ru.qatools.selenograph.states.HubSummariesState;
import ru.yandex.qatools.camelot.api.annotations.Filter;
import ru.yandex.qatools.camelot.api.annotations.Processor;

/**
 * TODO test the injection itself
 *
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
@Filter(instanceOf = HubSummariesState.class)
public class HubSummaryInjectProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HubSummaryInjectProcessor.class);

    @Autowired
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    private HostSelectionStrategy strategy;

    @Processor
    public void onSummariesUpdate(HubSummariesState state) {
        LOGGER.debug("got new summaries list, updating data in host selection strategy");
        if (strategy != null && strategy instanceof UpdatableSelectionStrategy) {
            ((UpdatableSelectionStrategy) strategy).updateHubSummaries(state.getHubSummaries(), state.getTimestamp());
        }
    }
}
