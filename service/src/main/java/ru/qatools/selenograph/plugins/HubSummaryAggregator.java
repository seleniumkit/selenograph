package ru.qatools.selenograph.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.camelot.api.AggregatorRepository;
import ru.yandex.qatools.camelot.api.EventProducer;
import ru.yandex.qatools.camelot.api.annotations.*;
import ru.yandex.qatools.fsm.annotations.FSM;
import ru.yandex.qatools.fsm.annotations.Transit;
import ru.yandex.qatools.fsm.annotations.Transitions;
import ru.qatools.selenograph.StartEvent;
import ru.qatools.selenograph.front.HubSummaryWrapper;
import ru.qatools.selenograph.states.HubBrowserState;
import ru.qatools.selenograph.states.HubState;
import ru.qatools.selenograph.states.HubSummariesState;

import java.util.Map;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
@Filter(instanceOf = StartEvent.class)
@Aggregate
@FSM(start = HubSummariesState.class)
@Transitions(@Transit)
public class HubSummaryAggregator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HubSummaryAggregator.class);

    @Input
    protected EventProducer self;

    @Output
    protected EventProducer out;

    @Repository
    AggregatorRepository repo;

    @Repository(HubStateAggregator.class)
    AggregatorRepository<HubState> hubState;

    @Repository(HubBrowserStateAggregator.class)
    AggregatorRepository<HubBrowserState> hubBrowsers;

    @OnInit
    public void init() {
        LOGGER.info("started!");
        self.produce(new StartEvent());
    }

    @OnTimer(cron = "30 * * * * ?", perState = false, skipIfNotCompleted = true)
    public void restoreOnFail() {
        if (repo.keys().isEmpty()) {
            self.produce(new StartEvent());
        }
    }

    @OnTimer(cron = "*/1 * * * * ?", readOnly = false, skipIfNotCompleted = true)
    public void updateHubSummaries(HubSummariesState state) {
        LOGGER.debug("updating hub summaries");
        Map<String, HubBrowserState> nodesMap = hubBrowsers.valuesMap();
        state.getHubSummaries().clear();
        state.getHubSummaries().addAll(hubState.valuesMap().values().stream()
                .map(hub -> new HubSummaryWrapper(hub, nodesMap).getBean())
                .collect(toList()));
        state.setTimestamp(currentTimeMillis());
        LOGGER.debug("successfully updated hub summaries");
        out.produce(state);
    }
}
