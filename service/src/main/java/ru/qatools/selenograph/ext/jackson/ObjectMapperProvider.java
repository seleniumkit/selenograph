package ru.qatools.selenograph.ext.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.PropertyAccessor.FIELD;
import static com.fasterxml.jackson.annotation.PropertyAccessor.GETTER;
import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL;

/**
 * @author Ilya Sadykov
 */
public class ObjectMapperProvider {

    public ObjectMapper provide() {
        ObjectMapper result = new ObjectMapper();
        result.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        result.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        result.setVisibility(FIELD, ANY);
        result.setVisibility(GETTER, NONE);
        result.registerModule(new JavaTimeModule());
        result.registerModule(new SelenographJacksonModule());
        result.enableDefaultTyping(NON_FINAL);
        return result;
    }
}
