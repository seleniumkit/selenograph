package ru.qatools.selenograph.states;

import ru.qatools.selenograph.BrowserInfo;
import ru.qatools.selenograph.HubEvent;
import ru.qatools.selenograph.util.Key;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public class HubState extends State {

    private String name;
    private String host;
    private int port;
    private boolean active;
    private boolean changed;
    private List<BrowserInfo> browsers = new ArrayList<>();

    public HubState() {
    }

    public HubState(String host, int port, List<BrowserInfo> browsers) {
        this.host = host;
        this.port = port;
        this.browsers = browsers;
    }

    private void update(String name, String host, int port, long timestamp, List<BrowserInfo> browsers) {
        this.name = name;
        this.host = host;
        this.port = port;
        addBrowsers(browsers);
        setTimestamp(timestamp);
    }

    public void addBrowsers(List<BrowserInfo> browsers) {
        if (this.browsers != null && !this.browsers.equals(browsers)) {
            this.browsers.clear();
            this.browsers.addAll(browsers);
            changed = true;
        }
    }

    public void update(HubEvent event) {
        update(event.getHubName(), event.getHubHost(), event.getHubPort(),
                event.getTimestamp(), event.getBrowsers());
    }

    public List<BrowserInfo> getBrowsers() {
        return browsers;
    }

    public void clear() {
        changed = true;
    }

    public void alive() {
        changed = !active;
        active = true;
    }

    public void down() {
        changed = active;
        active = false;
    }

    public void unmarkAsChanged() {
        changed = false;
    }

    public String getName() {
        return name;
    }

    public String getHostname() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return Key.buildAddress(host, port);
    }

    public boolean isActive() {
        return active;
    }

    public boolean hasChanged() {
        return changed;
    }
}
