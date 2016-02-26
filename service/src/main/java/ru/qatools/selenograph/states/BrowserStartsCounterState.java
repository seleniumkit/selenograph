package ru.qatools.selenograph.states;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.Serializable;

import static java.util.function.Predicate.isEqual;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public class BrowserStartsCounterState implements Serializable {

    private boolean restarting;

    private CircularFifoQueue<Boolean> lastBrowserStarts;

    public void init(int limit) {
        if (lastBrowserStarts == null) {
            lastBrowserStarts = new CircularFifoQueue<>(limit);
        }
    }

    private void assertInitialized() {
        if (lastBrowserStarts == null) {
            throw new IllegalStateException("You must call init(int) first!");
        }
    }

    public void startSuccessful() {
        assertInitialized();
        lastBrowserStarts.add(true);
    }

    public void startFailed() {
        assertInitialized();
        lastBrowserStarts.add(false);
    }

    public int getSuccessfulStartCount() {
        assertInitialized();
        return (int) lastBrowserStarts.stream().filter(isEqual(true)).count();
    }

    public int getFailedStartCount() {
        assertInitialized();
        return (int) lastBrowserStarts.stream().filter(isEqual(false)).count();
    }

    public double getFailedStartsPercentage() {
        assertInitialized();
        return (double) getFailedStartCount() / lastBrowserStarts.maxSize();
    }

    public void setRestarting(boolean restarting) {
        this.restarting = restarting;
    }

    public boolean isRestarting() {
        return restarting;
    }
}
