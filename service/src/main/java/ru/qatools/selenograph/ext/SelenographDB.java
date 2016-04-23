package ru.qatools.selenograph.ext;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultExchangeHolder;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.selenograph.gridrouter.BrowserContext;
import ru.qatools.selenograph.gridrouter.SessionEvent;
import ru.qatools.selenograph.gridrouter.SessionsAggregator;
import ru.qatools.selenograph.gridrouter.UserBrowser;
import ru.yandex.qatools.camelot.common.ProcessingEngine;
import ru.yandex.qatools.camelot.mongodb.MongoSerializer;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.camel.impl.DefaultExchangeHolder.unmarshal;
import static ru.qatools.mongodb.MongoPessimisticRepo.COLL_SUFFIX;
import static ru.yandex.qatools.camelot.util.MapUtil.map;

/**
 * @author Ilya Sadykov
 * WARN: MongoDB extension direct dependency!
 */
@SuppressWarnings("unchecked")
public class SelenographDB {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelenographDB.class);
    private final MongoClient mongo;
    private final String dbName;
    private final ProcessingEngine engine;
    private final MongoSerializer serializer;
    private String sessionsPluginId;

    public SelenographDB(MongoClient mongo, String dbName,
                         MongoSerializer serializer,
                         ProcessingEngine processingEngine) {
        this.mongo = mongo;
        this.dbName = dbName;
        this.engine = processingEngine;
        this.sessionsPluginId = engine.getPlugin(SessionsAggregator.class).getId();
        this.serializer = serializer;
    }

    public void init(){
        sessions().createIndex(new Document(map(
                "object.inBody.user", 1,
                "object.inBody.browser", 1,
                "object.inBody.version", 1
        )));
        sessions().createIndex(new Document(map(
                "object.inBody.user", 1
        )));
    }

    public Set<String> activeUsers() {
        Set<String> users = new LinkedHashSet<>();
        sessions().distinct("inBody.user", String.class).forEach((Consumer<String>) users::add);
        return users;
    }

    public Map<BrowserContext, Integer> sesionsByUserCount() {
        Map<BrowserContext, Integer> results = new HashMap<>();
        /**
         {$project: {inBody: 1}},
         {$unwind: { path: "$inBody", includeArrayIndex: "index" } },
         {$match: {index: 1} },
         {$project: {user: "$inBody.user", browser: "$inBody.browser", version: "$inBody.version"}},
         {$group: {
         _id: {user:"$user",browser: "$browser", version: "$version"},
         user: {$first: "$user"},
         browser: {$first: "$browser"},
         version: {$first: "$version"},
         count: {$sum: 1}
         }})
         **/
        sessions().aggregate(asList(
                new Document("$unwind", new Document(map(
                        "path", "$object",
                        "includeArrayIndex", "index"
                ))),
                new Document("$match", new Document("index", 1)),
                new Document("$project", new Document("object.inBody", 1)),
                new Document("$unwind", new Document(map(
                        "path", "$object.inBody",
                        "includeArrayIndex", "index"
                ))),
                new Document("$match", new Document("index", 1)),
                new Document("$project", new Document(map(
                        "user", "$object.inBody.user",
                        "browser", "$object.inBody.browser",
                        "version", "$object.inBody.version"
                ))),
                new Document("$group", new Document(map(
                        "_id", new Document(map(
                                "user", "$user",
                                "browser", "$browser",
                                "version", "$version"
                        )),
                        "user", new Document("$first", "$user"),
                        "browser", new Document("$first", "$browser"),
                        "version", new Document("$first", "$version"),
                        "count", new Document("$sum", 1)
                )))
        )).forEach((Consumer<Document>) d -> results.put(
                new UserBrowser()
                        .withUser(d.getString("user"))
                        .withBrowser(d.getString("browser"))
                        .withVersion(d.getString("version")),
                d.getInteger("count")
        ));
        return results;
    }

    public long countSessionsByUser(String user) {
        return sessions().count(eq("object.inBody.user", user));
    }

    public long countSessionsByUserAndBrowser(String user, String browser, String version) {
        return sessions().count(and(
                eq("object.inBody.user", user),
                eq("object.inBody.browser", browser),
                eq("object.inBody.version", version)
        ));
    }

    public Set<SessionEvent> sessionsByUser(String user) {
        Set<SessionEvent> result = new LinkedHashSet<>();
        sessions().find(eq("object.inBody.user", user)).forEach((Consumer<Document>) document ->
                result.add(convertDocument(document, SessionEvent.class)));
        return result;
    }

    public SessionEvent findSessionById(String sessionId) {
        return convertDocument(
                sessions().find(eq("_id", sessionId)).first(), SessionEvent.class
        );
    }


    private <T> T convertDocument(Document document, Class<T> clazz) {
        try {
            DefaultExchangeHolder holder = serializer.fromDBObject(document,
                    DefaultExchangeHolder.class);
            Exchange exchange = new DefaultExchange(engine.getCamelContext());
            unmarshal(exchange, holder);
            return (T) exchange.getIn().getBody();
        } catch (Exception e) {
            LOGGER.error("Failed to conert mongo document", e);
            return null;
        }
    }

    private MongoCollection<Document> sessions() {
        return mongo.getDatabase(dbName).getCollection(format("%s%s", sessionsPluginId, COLL_SUFFIX));
    }
}
