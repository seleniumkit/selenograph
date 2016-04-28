package ru.qatools.selenograph.gridrouter;

import org.eclipse.jetty.util.ConcurrentArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.qatools.gridrouter.sessions.StatsCounter;
import ru.qatools.selenograph.ext.SelenographDB;
import ru.yandex.qatools.camelot.common.ProcessingEngine;

import javax.inject.Inject;
import java.time.Duration;
import java.util.*;

import static java.lang.System.currentTimeMillis;
import static ru.qatools.selenograph.gridrouter.Key.browserName;
import static ru.qatools.selenograph.gridrouter.Key.browserVersion;
import static ru.yandex.qatools.camelot.api.Constants.Keys.ALL;

/**
 * @author Ilya Sadykov
 */
public class SessionsAggregator implements StatsCounter {
    static final String ROUTE_REGEX = "http://([^:]+):(\\d+)";
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionsAggregator.class);
    private static final Queue<SessionEvent> bulkUpsertQueue = new ConcurrentArrayQueue<>();
    @Inject
    SelenographDB database;
    @Inject
    ProcessingEngine procEngine;

    @Autowired
    public SessionsAggregator(@Value("${selenograph.sessions.bulk.flush.interval.ms}")
                              long bulkFlushIntervalMs){
        final Timer bulkTimer = new Timer();
        LOGGER.info("Initializing bulk flush timer...");
        bulkTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                flushBulkUpsertBuffer();
            }
        }, new Random().nextInt(100), bulkFlushIntervalMs);
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
        bulkUpsertQueue.add(startEvent);
    }

    @Override
    public void deleteSession(String sessionId, String route) {
        LOGGER.info("Removing session {} ({})", sessionId, route);
        bulkUpsertQueue.add(new DeleteSessionEvent().withSessionId(sessionId).withRoute(route));
    }

    @Override
    public void updateSession(String sessionId, String route) {
        LOGGER.info("Updating session {} ({})", sessionId, route);
        bulkUpsertQueue.add((SessionEvent) new UpdateSessionEvent()
                .withSessionId(sessionId).withRoute(route).withTimestamp(currentTimeMillis()));
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
        final SessionsCountsPerUser stats = (SessionsCountsPerUser) procEngine.getInterop()
                .repo(QuotaStatsAggregator.class).get(ALL);
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

    public void flushBulkUpsertBuffer() {
        LOGGER.info("Flushing upsert buffer. Queue size is {}", bulkUpsertQueue.size());
        SessionEvent event;
        final List<SessionEvent> events = new ArrayList<>();
        while ((event = bulkUpsertQueue.poll()) != null) {
            events.add(event);
        }
        if(!events.isEmpty()){
            database.bulkUpsertSessions(events);
        }
    }

    public SessionEvent findSessionById(String sessionId) {
        return database.findSessionById(sessionId);
    }
}
