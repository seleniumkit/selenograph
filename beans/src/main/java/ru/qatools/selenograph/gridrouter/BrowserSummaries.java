package ru.qatools.selenograph.gridrouter;

import ru.qatools.selenograph.front.BrowserSummary;
import ru.qatools.selenograph.front.VersionSummary;

import java.util.ArrayList;
import java.util.Map;

import static ru.qatools.selenograph.gridrouter.Key.browserVersion;

/**
 * @author Ilya Sadykov
 */
public class BrowserSummaries extends ArrayList<BrowserSummary> {

    public void addOrIncrement(Map<BrowserContext, Integer> availableMap,
                               Map<BrowserContext, Integer> runningMap) {
        availableMap.entrySet().forEach(e -> {
            final BrowserContext bc = e.getKey();
            int max = e.getValue();
            int running = runningMap.getOrDefault(e.getKey(), 0);
            final BrowserSummary summary = parallelStream()
                    .filter(bs -> bs.getName().equals(e.getKey().getBrowser()))
                    .findAny().orElseGet(() -> {
                        final BrowserSummary bs = new BrowserSummary().withName(bc.getBrowser());
                        add(bs);
                        return bs;
                    });
            final VersionSummary version = summary.getVersions().parallelStream()
                    .filter(v -> v.getVersion().equals(bc.getVersion()))
                    .findAny().orElseGet(() -> {
                        final VersionSummary vs = new VersionSummary().withVersion(browserVersion(bc.getVersion()));
                        summary.getVersions().add(vs);
                        return vs;
                    });
            summary.withMax(summary.getMax() + max);
            summary.withRunning(summary.getRunning() + running);
            version.withMax(version.getMax() + max);
            version.withRunning(version.getRunning() + running);
        });
    }
}
