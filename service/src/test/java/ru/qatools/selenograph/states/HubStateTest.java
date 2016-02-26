package ru.qatools.selenograph.states;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.qatools.selenograph.utils.MonitoringEventFactory.browser;
import static ru.qatools.selenograph.utils.MonitoringEventFactory.hubAlive;

/**
 * @author Innokenty Shuvalov innokenty@yandex-team.ru
 */
public class HubStateTest {

    private HubState hubState;

    @Before
    public void setUp() throws Exception {
        hubState = new HubState();
    }

    @Test
    public void testIsChangedOnFirstNodeAddition() {
        hubState.update(hubAlive("hub", browser("chrome", "40", 5)));
        assertThat(hubState.hasChanged(), is(true));
    }

    @Test
    public void testIsNotChangedOnNodeUpdateWithSameParams() {
        hubState.update(hubAlive("hub", browser("chrome", "40", 5)));
        hubState.unmarkAsChanged();
        hubState.update(hubAlive("hub", browser("chrome", "40", 5)));
        assertThat(hubState.hasChanged(), is(false));
    }

    @Test
    public void testIsChangedOnNodeUpdateWithDifferentBrowserName() {
        hubState.update(hubAlive("hub", browser("chrome", "40", 5)));
        hubState.unmarkAsChanged();
        hubState.update(hubAlive("hub", browser("firefox", "40", 5)));
        assertThat(hubState.hasChanged(), is(true));
    }

    @Test
    public void testIsChangedOnNodeUpdateWithDifferentVersion() {
        hubState.update(hubAlive("hub", browser("chrome", "40", 5)));
        hubState.unmarkAsChanged();
        hubState.update(hubAlive("hub", browser("chrome", "41", 5)));
        assertThat(hubState.hasChanged(), is(true));
    }

    @Test
    public void testIsChangedOnNodeUpdateWithDifferentInstancesCount() {
        hubState.update(hubAlive("hub", browser("chrome", "40", 5)));
        hubState.unmarkAsChanged();
        hubState.update(hubAlive("hub", browser("chrome", "40", 4)));
        assertThat(hubState.hasChanged(), is(true));
    }

    @Test
    public void testIsChangedOnNodeUpdateWithDifferentBrowsersList() {
        hubState.update(hubAlive("hub", browser("chrome", "40", 5)));
        hubState.unmarkAsChanged();
        hubState.update(hubAlive("hub", browser("chrome", "40", 5), browser("chrome", "41", 5)));
        assertThat(hubState.hasChanged(), is(true));
    }
}
