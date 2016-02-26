package ru.qatools.selenograph.states;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static ru.qatools.clay.utils.DateUtil.isTimePassedSince;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public class HubBrowserState implements Serializable {

    private final HashMap<String, Long> runningSessions = new HashMap<>();
    private final HashMap<String, Long> stoppingSessions = new HashMap<>();

    public void startSession(String sessionId, long timestamp) {
        if (stoppingSessions.remove(sessionId) == null) {
            runningSessions.put(sessionId, timestamp);
        }
    }

    public void releaseSession(String sessionId, long timestamp) {
        if (runningSessions.remove(sessionId) == null) {
            stoppingSessions.put(sessionId, timestamp);
        }
    }

    public void expireSession(String sessionId) {
        runningSessions.remove(sessionId);
        stoppingSessions.remove(sessionId);
    }

    public Set<String> getExpiredSessions(long timeToLiveInMillis) {
        Set<String> expired = new HashSet<>();
        expired.addAll(getExpiredSessions(runningSessions, timeToLiveInMillis));
        expired.addAll(getExpiredSessions(stoppingSessions, timeToLiveInMillis));
        return expired;
    }

    private Set<String> getExpiredSessions(Map<String, Long> sessionsMap, long ttl) {
        return sessionsMap.entrySet().stream()
                .filter(entry -> isTimePassedSince(ttl, entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(toSet());
    }

    public Set<String> getRunningSessions() {
        return runningSessions.keySet();
    }

    public Set<String> getStoppingSessions() {
        return stoppingSessions.keySet();
    }

    public int getRunningCount() {
        return Math.max(runningSessions.size() - stoppingSessions.size(), 0);
    }

    public boolean isEmpty() {
        return runningSessions.isEmpty() && stoppingSessions.isEmpty();
    }
}
