package ru.qatools.selenograph.ext.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Date;

import static ru.yandex.qatools.camelot.util.TypesUtil.isLong;

/**
 * @author Ilya Sadykov
 */
public class SelenographDeserializers extends Deserializers.Base {

    @Override
    public JsonDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
        if (isLong(type.getRawClass())) {
            return new LongDeserializer();
        }
        if (type.getRawClass().equals(Date.class)) {
            return new DateDeserializer();
        }
        return super.findBeanDeserializer(type, config, beanDesc);
    }

    private static class LongDeserializer extends StdDeserializer<Long> {
        NumberDeserializers.LongDeserializer longDeserializer = //NOSONAR
                new NumberDeserializers.LongDeserializer(Long.class, 0L);

        LongDeserializer() {
            super(Long.class);
        }

        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final boolean array = p.hasToken(JsonToken.START_ARRAY);
            if (p.hasToken(JsonToken.START_OBJECT) || array) {
                p.nextToken();
                long value;
                if(array){
                    p.nextToken();
                    value = Long.parseLong(p.getText());
                } else {
                    value = Long.parseLong(p.nextTextValue());
                }
                p.nextToken();
                return value;
            }
            return longDeserializer.deserialize(p, ctxt);
        }
    }

    private static class DateDeserializer extends StdDeserializer<Date> {
        DateDeserializers.DateDeserializer dateDeserializer = new DateDeserializers.DateDeserializer(); //NOSONAR

        protected DateDeserializer() {
            super(Date.class);
        }

        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.hasToken(JsonToken.START_OBJECT)) {
                p.nextToken();
                p.nextToken();
                long value = Long.parseLong(p.getText());
                p.nextToken();
                return new Date(value);
            }
            return dateDeserializer.deserialize(p, ctxt);
        }
    }
}
