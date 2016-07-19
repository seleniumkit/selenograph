package ru.qatools.selenograph.gridrouter;

import ru.qatools.gridrouter.sessions.GridRouterUserStats;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.round;
import static java.lang.String.format;

/**
 * @author Ilya Sadykov
 */
public class SessionsCountsPerUser extends HashMap<String, SessionsState> implements Serializable, GridRouterUserStats {

    public SessionsCountsPerUser(Map<BrowserContext, Integer> counts) {
        resetStats(counts);
    }

    public SessionsCountsPerUser() {
        // need to have no-args constructor
    }

    public static BrowserContext fromBrowserString(String browser) {
        final String[] parts = browser.replaceAll("\\[DOT\\]", ".").split(":");
        return new UserBrowser().withUser(parts[0]).withBrowser(parts[1]).withVersion(parts[2]);
    }

    public static String toBrowserString(BrowserContext browser) {
        return toBrowserString(browser.getUser(), browser.getBrowser(), browser.getVersion());
    }

    public static String toBrowserString(String user, String browser, String version) {
        return format("%s:%s:%s", user, browser, version).replaceAll("\\.", "[DOT]");
    }

    public SessionsState getFor(String user, String browser, String version) {
        return get(toBrowserString(user, browser, version));
    }

    public void updateStats(SessionsCountsPerUser counts) {
        counts.entrySet().forEach(count -> {
            putIfAbsent(count.getKey(), new SessionsState());
            final SessionsState state = get(count.getKey());
            state.setRaw(count.getValue().getRaw());
            state.setMax(state.getRaw() > state.getMax() ? state.getRaw() : state.getMax());
            state.setAvg(round(((float) state.getAvg() + (float) state.getRaw()) / 2.0f));
            state.setBrowser(count.getValue().getBrowser());
            state.setVersion(count.getValue().getVersion());
            state.setUser(count.getValue().getUser());
        });
    }

    public void resetStats(Map<BrowserContext, Integer> counts) {
        counts.entrySet().forEach(e -> {
            final BrowserContext browser = e.getKey();
            final Integer count = e.getValue();
            putIfAbsent(toBrowserString(browser), new SessionsState());
            final SessionsState state = get(toBrowserString(browser));
            state.setRaw(count);
            state.setAvg(count);
            state.setMax(count);
            state.setBrowser(browser.getBrowser());
            state.setVersion(browser.getVersion());
            state.setUser(browser.getUser());
            state.setTimestamp(browser.getTimestamp());
        });
    }
}
