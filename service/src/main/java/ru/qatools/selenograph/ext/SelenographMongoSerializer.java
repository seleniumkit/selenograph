package ru.qatools.selenograph.ext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.util.JSON;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.selenograph.ext.jackson.ObjectMapperProvider;
import ru.yandex.qatools.camelot.common.MessagesSerializer;
import ru.yandex.qatools.camelot.mongodb.MongoSerializer;

import java.util.List;

/**
 * @author Ilya Sadykov
 */
@SuppressWarnings({"deprecation", "unchecked"})
public class SelenographMongoSerializer implements MongoSerializer {
    public static final String OBJECT_FIELD = "object";
    private static final Logger LOGGER = LoggerFactory.getLogger(SelenographMongoSerializer.class);
    private final MessagesSerializer serializer;
    private final ClassLoader classLoader;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("deprecation")
    public SelenographMongoSerializer(MessagesSerializer serializer, ClassLoader classLoader) {
        this.serializer = serializer;
        this.classLoader = classLoader;
        this.objectMapper = new ObjectMapperProvider().provide();
    }

    /**
     * Serialize the object to bytes
     */
    @Override
    public BasicDBObject toDBObject(Object object) {
        try {
            return new BasicDBObject(
                    OBJECT_FIELD, JSON.parse(objectMapper.writeValueAsString(object))
            );
        } catch (Exception e) {
            LOGGER.error("Failed to serialize object to basic db object", e);
            return new BasicDBObject(); //NOSONAR
        }
    }

    /**
     * Deserialize the input bytes into object
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromDBObject(Document input, Class<T> expected)
            throws Exception { //NOSONAR
        try {
            if (input == null) {
                return null;
            }
            final BasicDBList list = new BasicDBList();
            list.addAll((List) input.get(OBJECT_FIELD));
            return (T) objectMapper.readValue(list.toString(), expected);
        } catch (Exception e) {
            throw new MongoException("Unknown error occurred converting BSON to object", e);
        }
    }
}
