package ru.qatools.selenograph.plugins;

import org.junit.Test;
import ru.qatools.selenograph.HubStarting;
import ru.qatools.selenograph.InputEvent;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * @author Ilya Sadykov
 */
public class InputResourceTest {

    @Test
    public void testUnmarshalling() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<inputEvent xmlns=\"urn:selenograph.qatools.ru\">" +
                "<event xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"HubStarting\" " +
                "hubHost=\"firefox33-ft-4.haze.yandex.net\" " + "hubPort=\"4445\" timestamp=\"1454605009052\">" +
                "<browsers version=\"33.0\" name=\"firefox\" maxInstances=\"1\" platform=\"LINUX\"/>" +
                "</event></inputEvent>";
        JAXBContext jc = JAXBContext.newInstance(InputEvent.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        unmarshaller.setEventHandler(new javax.xml.bind.helpers.DefaultValidationEventHandler());
        InputEvent event = (InputEvent) unmarshaller.unmarshal(new StringReader(xml));
        assertThat(event.getEvent(), notNullValue());
        assertThat(event.getEvent(), instanceOf(HubStarting.class));
        assertThat(((HubStarting)event.getEvent()).getBrowsers(), hasSize(1));
    }
}