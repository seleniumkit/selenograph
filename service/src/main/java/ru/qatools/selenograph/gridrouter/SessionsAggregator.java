package ru.qatools.selenograph.gridrouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.gridrouter.sessions.StatsCounter;
import ru.qatools.selenograph.ext.SelenographDB;
import ru.yandex.qatools.camelot.api.AggregatorRepository;
import ru.yandex.qatools.camelot.api.EventProducer;
import ru.yandex.qatools.camelot.api.annotations.*;
import ru.yandex.qatools.camelot.common.ProcessingEngine;
import ru.yandex.qatools.fsm.StopConditionAware;
import ru.yandex.qatools.fsm.annotations.*;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static java.lang.System.currentTimeMillis;
import static java.util.Optional.ofNullable;
import static ru.qatools.selenograph.gridrouter.Key.browserName;
import static ru.qatools.selenograph.gridrouter.Key.browserVersion;
import static ru.yandex.qatools.camelot.api.Constants.Keys.ALL;

/**
 * @author Ilya Sadykov
 */
@Aggregate
@Filter(custom = SessionEventFilter.class)
@FSM(start = UndefinedSessionState.class)
@Transitions({
        @Transit(to = StartSessionEvent.class, on = SessionEvent.class),
        @Transit(on = DeleteSessionEvent.class, stop = true)
})
public class SessionsAggregator implements StatsCounter, StopConditionAware<SessionEvent, SessionEvent> {
    static final String ROUTE_REGEX = "http://([^:]+):(\\d+)";
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionsAggregator.class);
    @Inject
    SelenographDB database;
    @Inject
    ProcessingEngine procEngine;
    @Input(SessionsAggregator.class)
    private EventProducer input;
    @Input(QuotaStatsAggregator.class)
    private EventProducer qutaStats;
    @Repository(QuotaStatsAggregator.class)
    private AggregatorRepository<SessionsCountsPerUser> statsRepo;

    @NewState
    public Object newState(Class stateClass, SessionEvent event) throws Exception {
        return event.withTimestamp(currentTimeMillis());
    }

    @AggregationKey
    public String aggregationKey(SessionEvent event) {
        return event.getSessionId();
    }

    @BeforeTransit
    public void beforeUpdate(SessionEvent state, SessionEvent event) {
        LOGGER.debug("on{} session {} for {}:{}:{} ({})", event.getClass().getSimpleName(),
                event.getSessionId(), event.getUser(), event.getBrowser(), event.getVersion(), event.getRoute());
        state.setTimestamp(event.getTimestamp());
    }

    @Override
    public void startSession(String sessionId, String user, String browser, String version, String route) {
        final String name = browserName(browser);
        final String ver = browserVersion(version);
        LOGGER.info("Starting session {} for {}:{}:{} ({})", sessionId, user, name, ver, route);
        final StartSessionEvent startEvent = (StartSessionEvent) new StartSessionEvent()
                .withSessionId(sessionId)
                .withRoute(route)
                .withUser(user)
                .withBrowser(name)
                .withVersion(ver)
                .withTimestamp(currentTimeMillis());
        input.produce(startEvent);
    }

    @Override
    public void deleteSession(String sessionId, String route) {
        LOGGER.info("Removing session {} ({})", sessionId, route);
        findSessionById(sessionId).ifPresent(s -> input.produce(sessionEvent(new DeleteSessionEvent(), s)));
    }

    @Override
    public void updateSession(String sessionId, String route) {
        LOGGER.info("Updating session {} ({})", sessionId, route);
        findSessionById(sessionId).ifPresent(s -> input.produce(sessionEvent(new UpdateSessionEvent(), s)));
    }

    @Override
    public void expireSessionsOlderThan(Duration duration) {
        database.deleteSessionsOlderThan(duration.toMillis());
    }

    @Override
    public Set<String> getActiveSessions() {
        return database.getActiveSessions();
    }

    @Override
    public SessionsCountsPerUser getStats(String user) {
        final SessionsCountsPerUser stats = statsRepo.get(ALL);
        return stats != null ? stats : new SessionsCountsPerUser();
    }

    @Override
    public int getSessionsCountForUser(String user) {
        return (int) database.countSessionsByUser(user);
    }

    @Override
    public int getSessionsCountForUserAndBrowser(String user, String browser, String version) {
        return (int) database.countSessionsByUserAndBrowser(user, browser, version);
    }

    Set<SessionEvent> sessionsByUser(String user) {
        return database.sessionsByUser(user);
    }

    private Optional<SessionEvent> findSessionById(String sessionId) {
        return ofNullable(database.findSessionById(sessionId));
    }

    @SuppressWarnings("unchecked")
    private <E extends SessionEvent> E sessionEvent(E event, SessionEvent state) {
        return (E) event.withUser(state.getUser())
                .withBrowser(state.getBrowser())
                .withVersion(state.getVersion())
                .withRoute(state.getRoute())
                .withSessionId(state.getSessionId())
                .withTimestamp(currentTimeMillis());
    }

    @Override
    public boolean isStopRequired(SessionEvent state, SessionEvent event) {
        return !(state instanceof StartSessionEvent)
                || state.getBrowser() == null
                || state.getVersion() == null
                || state.getUser() == null
                || state.getSessionId() == null;
    }
}
