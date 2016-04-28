package ru.qatools.selenograph.ext;

import ru.yandex.qatools.camelot.common.MessagesSerializer;
import ru.yandex.qatools.camelot.mongodb.MongoSerializer;
import ru.yandex.qatools.camelot.mongodb.MongoSerializerBuilder;

/**
 * @author Ilya Sadykov
 */
public class SelenographMongoSerializerBuilder extends MongoSerializerBuilder{
    @Override
    public MongoSerializer build(MessagesSerializer msgSerializer, ClassLoader classLoader) {
        return new SelenographMongoSerializer();
    }
}
