package ru.qatools.selenograph.util;

import de.flapdoodle.embed.mongo.distribution.Version;
import ru.yandex.qatools.embed.service.MongoEmbeddedService;

import java.io.IOException;

/**
 * @author Ilya Sadykov
 */
public class EmbeddedMongodbService extends MongoEmbeddedService {
    public EmbeddedMongodbService(String replicaSet, String mongoDatabaseName) throws IOException {
        super(replicaSet, mongoDatabaseName);
    }

    public EmbeddedMongodbService(String replicaSet, String mongoDatabaseName, String mongoUsername, String mongoPassword, String replSetName) throws IOException {
        super(replicaSet, mongoDatabaseName, mongoUsername, mongoPassword, replSetName);
    }

    public EmbeddedMongodbService(String replicaSet, String mongoDatabaseName, String mongoUsername, String mongoPassword, String replSetName, String dataDirectory, boolean enabled, int initTimeout) throws IOException {
        super(replicaSet, mongoDatabaseName, mongoUsername, mongoPassword, replSetName, dataDirectory, enabled, initTimeout);
    }

    public void setVersion(Version.Main version) {
        useVersion(version);
    }
}
