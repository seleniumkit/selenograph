package ru.qatools.selenograph.gridrouter;

import org.jvnet.jaxb2_commons.lang.*;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

import java.io.Serializable;
import java.time.temporal.Temporal;

import static java.time.ZonedDateTime.now;

/**
 * @author Ilya Sadykov
 */
public class Timestamped<T extends Timestamped> implements Serializable, Equals, HashCode, ToString {
    private Temporal timestamp = now();

    public Temporal getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Temporal timestamp) {
        this.timestamp = timestamp;
    }

    @SuppressWarnings("unchecked")
    public T withTimestamp(Temporal timestamp) {
        setTimestamp(timestamp);
        return (T) this;
    }

    @Override
    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object that, EqualsStrategy strategy) {
        return (that instanceof Timestamped && ((Timestamped) that).timestamp.equals(this.timestamp));
    }

    @Override
    public int hashCode(ObjectLocator thisLocator, HashCodeStrategy strategy) {
        return timestamp.hashCode();
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
