package ru.qatools.selenograph.gridrouter;

import ru.qatools.gridrouter.sessions.GridRouterUserStats;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * @author Ilya Sadykov
 */
public class UserSessionsStats extends HashMap<String, UserSessionsStats.StatsData> implements GridRouterUserStats {

    public UserSessionsStats(Collection<SessionsState> states, Map<String, SessionEvent> allSessions) {
        states.forEach(state -> {
                    final String browserKey = format("%s:%s:%s", state.getUser(),
                            state.getBrowser(), state.getVersion());
                    put(browserKey, new StatsData(state.getMax(), state.getAvg(), state.getRaw(),
                            allSessions.entrySet().stream()
                                    .filter(e -> e.getKey().startsWith(browserKey)).count()

                    ));
                }
        );
    }

    public static class StatsData {
        int max;
        int avg;
        int raw;
        long current;

        public StatsData(int max, int avg, int raw, long current) {
            this.max = max;
            this.avg = avg;
            this.raw = raw;
            this.current = current;
        }

        public int getMax() {
            return max;
        }

        public int getAvg() {
            return avg;
        }

        public int getRaw() {
            return raw;
        }

        public long getCurrent() {
            return current;
        }
    }
}
