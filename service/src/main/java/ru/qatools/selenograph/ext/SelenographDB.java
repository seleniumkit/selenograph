package ru.qatools.selenograph.ext;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultExchangeHolder;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.gridrouter.ConfigRepository;
import ru.qatools.gridrouter.config.Browsers;
import ru.qatools.selenograph.gridrouter.*;
import ru.yandex.qatools.camelot.common.ProcessingEngine;
import ru.yandex.qatools.camelot.mongodb.MongoSerializer;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.sort;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingInt;
import static org.apache.camel.impl.DefaultExchangeHolder.unmarshal;
import static org.apache.commons.lang3.tuple.Pair.of;
import static ru.qatools.mongodb.MongoPessimisticRepo.COLL_SUFFIX;
import static ru.qatools.selenograph.gridrouter.Key.browserName;
import static ru.qatools.selenograph.gridrouter.Key.browserVersion;
import static ru.yandex.qatools.camelot.util.MapUtil.map;

/**
 * @author Ilya Sadykov
 *         WARN: MongoDB extension direct dependency!
 */
@SuppressWarnings("unchecked")
public class SelenographDB {
    public static final String ALL = "all";
    private static final Logger LOGGER = LoggerFactory.getLogger(SelenographDB.class);
    private final MongoClient mongo;
    private final String dbName;
    private final ProcessingEngine engine;
    private final MongoSerializer serializer;
    @Inject
    private ConfigRepository config;
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

    private static Map<String, Map<BrowserContext, Integer>> quotaCounts(Map<String, Browsers> quotaMap) {
        Map<String, Map<BrowserContext, Integer>> res = new LinkedHashMap<>();
        Map<Pair<BrowserContext, String>, Integer> hubMax = new HashMap<>();
        quotaMap.entrySet().forEach(e -> {
            final String quota = e.getKey();
            res.putIfAbsent(quota, new LinkedHashMap<>());
            e.getValue().getBrowsers().forEach(b ->
                    b.getVersions().forEach(v -> {
                        final BrowserContext key = new UserBrowser()
                                .withBrowser(browserName(b.getName()))
                                .withVersion(browserVersion(v.getNumber()))
                                .withTimestamp(0);
                        v.getRegions().parallelStream()
                                .flatMap(r -> r.getHosts().parallelStream())
                                .forEach(h -> {
                                    final Pair<BrowserContext, String> pair = of(key, h.getAddress());
                                    if (!hubMax.containsKey(pair) || hubMax.get(pair) < h.getCount()) {
                                        hubMax.put(pair, h.getCount());
                                    }
                                });
                        res.get(quota).putIfAbsent(key, 0);
                        res.get(quota).put(key, res.get(quota).get(key) + v.getCount());
                    }));
        });
        res.put(ALL, new HashMap<>());
        hubMax.entrySet().parallelStream().collect(groupingBy(e -> e.getKey().getKey(), summingInt(Map.Entry::getValue)))
                .entrySet().forEach(e -> res.get(ALL).putIfAbsent(e.getKey(), e.getValue()));
        return res;
    }

    private static Map<String, Map<BrowserContext, Integer>> runningCounts(Map<BrowserContext, Integer> counts) {
        Map<String, Map<BrowserContext, Integer>> res = new LinkedHashMap<>();
        counts.entrySet().forEach(e -> {
            final BrowserContext context = e.getKey();
            final String quota = context.getUser();
            final BrowserContext key = new UserBrowser()
                    .withBrowser(browserName(context.getBrowser()))
                    .withVersion(browserVersion(context.getVersion()))
                    .withTimestamp(0);
            res.putIfAbsent(ALL, new HashMap<>());
            res.putIfAbsent(quota, new HashMap<>());
            res.get(ALL).putIfAbsent(key, 0);
            res.get(quota).putIfAbsent(key, 0);
            res.get(ALL).put(key, res.get(ALL).get(key) + e.getValue());
            res.get(quota).put(key, res.get(quota).get(key) + e.getValue());
        });
        return res;
    }

    public void init() {
        sessions().createIndex(new Document(map(
                "object.inBody.user", 1,
                "object.inBody.browser", 1,
                "object.inBody.version", 1
        )));
        sessions().createIndex(new Document(map(
                "object.inBody.user", 1
        )));
    }

    public Map<String, BrowserSummaries> getQuotasSummary() {
        final Map<String, Map<BrowserContext, Integer>> running = runningCounts(sessionsByUserCount());
        final Map<String, Browsers> quotaMap = config.getQuotaMap();
        final List<String> quotas = new ArrayList<>();
        quotas.add(ALL);
        quotas.addAll(quotaMap.keySet());
        sort(quotas);
        final Map<String, Map<BrowserContext, Integer>> available = quotaCounts(quotaMap);
        final Map<String, BrowserSummaries> state = new LinkedHashMap<>();
        quotas.forEach(quota -> {
            state.putIfAbsent(quota, new BrowserSummaries());
            state.get(quota).addOrIncrement(
                    available.getOrDefault(quota, emptyMap()),
                    running.getOrDefault(quota, emptyMap())
            );
        });
        return state;
    }

    public Map<BrowserContext, Integer> sessionsByUserCount() {
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
