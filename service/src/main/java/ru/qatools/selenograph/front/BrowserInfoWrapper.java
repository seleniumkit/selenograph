package ru.qatools.selenograph.front;

import ru.qatools.selenograph.BrowserInfo;

import static ru.qatools.selenograph.util.Key.browserName;
import static ru.qatools.selenograph.util.Key.browserVersion;

/**
 * @author Ilya Sadykov
 */
public class BrowserInfoWrapper {
    BrowserInfo info;

    public BrowserInfoWrapper(BrowserInfo info) {
        this.info = info;
    }

    public BrowserInfo unwrap() {
        return info;
    }

    @Override
    @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BrowserInfoWrapper that = (BrowserInfoWrapper) o;
        return browserName(info).equals(browserName(that.info)) &&
                browserVersion(info).equals(browserVersion(that.info));
    }

    @Override
    public int hashCode() {
        return (browserName(info) + browserVersion(info)).hashCode();
    }
}
