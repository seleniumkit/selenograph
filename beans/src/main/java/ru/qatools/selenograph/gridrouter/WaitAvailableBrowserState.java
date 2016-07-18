package ru.qatools.selenograph.gridrouter;

import java.io.Serializable;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static java.time.ZonedDateTime.now;
import static java.util.stream.Collectors.toList;

/**
 * @author Ilya Sadykov
 */
public class WaitAvailableBrowserState extends BrowserContext implements Serializable { //NOSONAR
    private Map<String, ZonedDateTime> requestIds = new HashMap<>();

    public void addRequest(String requestId) {
        requestIds.put(requestId, now());
    }

    public void removeRequest(String requestId) {
        requestIds.remove(requestId);
    }

    public void expireRequestsOlderThan(Duration duration) {
        requestIds.entrySet().stream()
                .filter(e -> duration.compareTo(Duration.between(e.getValue(), now())) < 0)
                .map(Map.Entry::getKey).collect(toList())
                .forEach(this::removeRequest);
    }

    public int size() {
        return requestIds.size();
    }
}
