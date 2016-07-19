package ru.qatools.selenograph.ext.jackson;

import com.fasterxml.jackson.databind.module.SimpleSerializers;
import org.bson.types.ObjectId;
import org.mongojack.internal.DBRefSerializer;
import org.mongojack.internal.ObjectIdSerializer;

/**
 * @author Ilya Sadykov
 */
public class SelenographSerializers extends SimpleSerializers {
    public SelenographSerializers() {
        addSerializer(new DBRefSerializer());
        addSerializer(ObjectId.class, new ObjectIdSerializer());
    }
}
