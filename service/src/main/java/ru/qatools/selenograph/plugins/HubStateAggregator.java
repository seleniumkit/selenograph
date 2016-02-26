package ru.qatools.selenograph.plugins;

import ru.yandex.qatools.camelot.api.AggregatorRepository;
import ru.yandex.qatools.camelot.api.EventProducer;
import ru.yandex.qatools.camelot.api.annotations.*;
import ru.yandex.qatools.fsm.annotations.*;
import ru.qatools.selenograph.*;
import ru.qatools.selenograph.states.HubState;

import static java.util.concurrent.TimeUnit.*;
import static ru.qatools.clay.utils.DateUtil.isTimePassedSince;
import static ru.qatools.selenograph.states.BeanUtils.hubDown;
import static ru.qatools.selenograph.states.BeanUtils.stop;
import static ru.qatools.selenograph.util.Key.hubAddress;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
@Filter(instanceOf = {HubStarting.class, HubAlive.class, HubDown.class, StopCorrelationKey.class})
@Aggregate
@FSM(start = HubState.class)
@Transitions({
        @Transit(on = HubEvent.class),
        @Transit(stop = true, on = StopCorrelationKey.class)
})
public class HubStateAggregator extends SelfStoppingAggregator {
    @ConfigValue("selenograph.hub.remove.timeout")
    private final long hubRemoveTimeout = DAYS.toMillis(3);
    @ConfigValue("selenograph.hub.down.timeout")
    private final long hubDownTimeout = MINUTES.toMillis(2);
    @Repository
    protected AggregatorRepository<HubState> repo;
    @MainInput
    EventProducer mainInput;

    @AggregationKey
    public String byHub(SeleniumEvent event) {
        return hubAddress(event);
    }

    @BeforeTransit
    public void updateHubInfo(HubState state, HubEvent event) {
        state.update(event);
    }

    @OnTransit
    public void onHubStarting(HubState state, HubStarting event) {
        logger.info("{} - hub is starting!", state.getAddress());
        state.alive();
        state.clear();
    }

    @OnTransit
    public void onHubAlive(HubState state, HubAlive event) {
        logger.info("{} - hub is alive!", state.getAddress());
        state.alive();
    }

    @OnTransit
    public void onHubDown(HubState state, HubDown event) {
        logger.info("{} - hub is down!", state.getAddress());
        state.down();
        state.clear();
    }

    @AfterTransit
    public void sendStateToOutput(HubState state) {
        if (state.hasChanged()) {
            logger.info("{} - hub has changed, sending to output", state.getAddress());
            out.produce(state);
            state.unmarkAsChanged();
        }
    }

    @OnTimer(cron = "${selenograph.hub.check.cron}", skipIfNotCompleted = true, perState = false)
    public void hubAliveCheck() {
        repo.valuesMap().entrySet().forEach(entry -> {
            final HubState state = entry.getValue();
            long lastUpdate = state.getTimestamp();
            logger.info("{} - checking hub nactivity (last updated at={})",
                    state.getAddress(), lastUpdate);

            if (isTimePassedSince(hubRemoveTimeout, lastUpdate) && lastUpdate != 0) {
                logger.warn("{} - hub is dead for more than {} hours already, stopping aggregator",
                        state.getAddress(), MILLISECONDS.toHours(hubRemoveTimeout));
                self.produce(stop(entry.getKey()));
            }

            if (state.isActive() && isTimePassedSince(hubDownTimeout, lastUpdate)) {
                logger.warn("{} - hub was inactive for more than {} seconds. It is considered as down!",
                        state.getAddress(), MILLISECONDS.toSeconds(hubDownTimeout));
                self.produce(hubDown(state));
            }
        });
    }
}
