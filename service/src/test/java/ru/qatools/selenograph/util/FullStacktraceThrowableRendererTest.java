package ru.qatools.selenograph.util;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

/**
 * @author Ilya Sadykov
 */
public class FullStacktraceThrowableRendererTest {


    @Test(expected = RuntimeException.class)
    public void testRender() throws Exception {
        try {
            callNotImplementedMethod();
        } catch (NotImplementedException e) {
            final String[] rendered = new FullStacktraceThrowableRenderer().doRender(e);
            assertThat(asList(rendered),
                    hasItem("org.apache.commons.lang3.NotImplementedException: java.lang.IllegalArgumentException: Illegal!"));
            assertThat(asList(rendered),
                    hasItem("\tat ru.qatools.selenograph.util.FullStacktraceThrowableRendererTest." +
                            "callIllegalArgumentMethod(FullStacktraceThrowableRendererTest.java:40)"));
            throw e;
        }
    }

    private void callNotImplementedMethod() {
        try {
            callIllegalArgumentMethod();
        } catch (IllegalArgumentException e) {
            throw new NotImplementedException(e);
        }
    }

    private void callIllegalArgumentMethod() {
        throw new IllegalArgumentException("Illegal!");
    }
}