package ru.qatools.selenograph.ext.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;

/**
 * @author Ilya Sadykov
 */
public class SelenographJacksonModule extends Module {
    @Override
    public String getModuleName() {
        return "selenograph";
    }

    @Override
    public Version version() {
        return new Version(1, 0, 0, null, "ru.qatools.selenograph", "selenograph");
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new SelenographSerializers());
        context.addDeserializers(new SelenographDeserializers());
    }
}
