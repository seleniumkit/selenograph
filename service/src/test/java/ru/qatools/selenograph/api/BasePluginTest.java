package ru.qatools.selenograph.api;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.camelot.test.AggregatorStateStorage;
import ru.yandex.qatools.camelot.test.Helper;
import ru.yandex.qatools.camelot.test.TestHelper;
import ru.yandex.qatools.matchers.decorators.TimeoutWaiter;
import ru.qatools.selenograph.utils.TestProperties;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.camelot.test.Matchers.containStateFor;
import static ru.yandex.qatools.matchers.decorators.MatcherDecorators.should;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public abstract class BasePluginTest {

    protected static final int TIMEOUT = new TestProperties().getTimeout();

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Helper
    protected TestHelper helper;

    @Rule
    public final TestName testName = new TestName();

    @Before
    public final void logTestStart() {
        logger.info(format("%n--------------------------------------" +
                           "%nStarting test %s.%s" +
                           "%n--------------------------------------",
                           this.getClass().getSimpleName(),
                           testName.getMethodName()));
    }

    @After
    public final void logTestFinish() {
        logger.info(format("%n--------------------------------------" +
                           "%nFinished test %s.%s" +
                           "%n--------------------------------------",
                           this.getClass().getSimpleName(),
                           testName.getMethodName()));
    }

    protected static TimeoutWaiter timeoutHasExpired() {
        return TimeoutWaiter.timeoutHasExpired(TIMEOUT);
    }

    protected final void send(Object event) {
        helper.send(event);
    }

    protected void sleep(int timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected final void shouldStop(AggregatorStateStorage storage,
                                    String aggregationKey,
                                    String reason) {
        storageShould(not(containStateFor(aggregationKey)), storage, reason);
    }

    protected final void stateShouldBe(Class<?> stateClass,
                                       AggregatorStateStorage storage,
                                       String aggregationKey) {
        stateShouldBe(stateClass, storage, aggregationKey,
                "state should be instance of " + stateClass.getSimpleName());
    }

    protected final void stateShouldBe(Class<?> clazz,
                                       AggregatorStateStorage storage,
                                       String aggregationKey,
                                       String reason) {
        storageShould(containStateFor(aggregationKey, clazz), storage, reason);
    }

    protected final void storageShould(Matcher<AggregatorStateStorage> matcher,
                                       AggregatorStateStorage storage,
                                       String reason) {
        assertThat(reason, storage, should(matcher).whileWaitingUntil(timeoutHasExpired()));
    }

    protected final void storageShould(Matcher<AggregatorStateStorage> matcher,
                                       AggregatorStateStorage storage) {
        assertThat(storage, should(matcher).whileWaitingUntil(timeoutHasExpired()));
    }
}
