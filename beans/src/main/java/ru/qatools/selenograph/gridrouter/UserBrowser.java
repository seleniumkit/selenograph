package ru.qatools.selenograph.gridrouter;

import java.util.Objects;

/**
 * @author Ilya Sadykov
 */
public class UserBrowser extends BrowserContext {
    @Override
    public boolean equals(Object object) {
        return object != null &&
                object instanceof UserBrowser &&
                Objects.equals(((UserBrowser) object).getBrowser(), getBrowser()) &&
                Objects.equals(((UserBrowser) object).getVersion(), getVersion()) &&
                Objects.equals(((UserBrowser) object).getUser(), getUser());
    }

    @Override
    public int hashCode() {
        return String.format("%s%s%s", getUser(), getBrowser(), getVersion()).hashCode();
    }
}
