package ru.qatools.selenograph.gridrouter;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.util.MatcherAssertionErrors.assertThat;
import static ru.qatools.selenograph.gridrouter.SessionsAggregator.ROUTE_REGEX;

/**
 * @author Ilya Sadykov
 */
public class RegexTest {
    @Test
    public void testRegex() throws Exception {
        assertThat(
                "http://firefox-33.haze.yandex.net:4444".replaceAll(ROUTE_REGEX,"$1"),
                is("firefox-33.haze.yandex.net")
        );
        assertThat(
                "http://firefox-33.haze.yandex.net:4444".replaceAll(ROUTE_REGEX,"$2"),
                is("4444")
        );
    }
}
