package ru.qatools.selenograph.gridrouter;

import org.jvnet.jaxb2_commons.lang.*;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

import java.io.Serializable;

import static java.lang.System.currentTimeMillis;

/**
 * @author Ilya Sadykov
 */
public class Timestamped<T extends Timestamped> implements Serializable, Equals, HashCode, ToString {
    private long timestamp = currentTimeMillis();

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @SuppressWarnings("unchecked")
    public T withTimestamp(long timestamp) {
        setTimestamp(timestamp);
        return (T) this;
    }

    @Override
    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object that, EqualsStrategy strategy) {
        return (that instanceof Timestamped && ((Timestamped) that).timestamp == this.timestamp);
    }

    @Override
    public int hashCode(ObjectLocator thisLocator, HashCodeStrategy strategy) {
        return Long.valueOf(timestamp).hashCode();
    }

    @Override
    public StringBuilder append(ObjectLocator locator, StringBuilder builder, ToStringStrategy strategy) {
        strategy.appendStart(locator, this, builder);
        appendFields(locator, builder, strategy);
        strategy.appendEnd(locator, this, builder);
        return builder;
    }

    @Override
    public StringBuilder appendFields(ObjectLocator locator, StringBuilder builder, ToStringStrategy strategy) {
        strategy.appendField(locator, this, "timestamp", builder, timestamp);
        return builder;
    }
}
