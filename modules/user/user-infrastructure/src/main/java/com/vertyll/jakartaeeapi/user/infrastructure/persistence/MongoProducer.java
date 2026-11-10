package com.vertyll.jakartaeeapi.user.infrastructure.persistence;

import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * The MongoDB client and database the accounts are kept in, from {@code mongodb.uri} and {@code mongodb.database}
 * ({@code MONGODB_URI} and {@code MONGODB_DATABASE}), which have no default, so a deployment that leaves one out fails
 * to start.
 *
 * <p>
 * A write is acknowledged once a majority of the replica set holds it, UUIDs are stored in the standard binary
 * subtype, and connecting, choosing a server and reading a reply each have a bound, so an unreachable database fails a
 * request instead of holding its thread.
 */
@ApplicationScoped
public class MongoProducer {
    private static final long CONNECT_TIMEOUT_SECONDS = 5;
    private static final long READ_TIMEOUT_SECONDS = 30;
    private static final long SERVER_SELECTION_TIMEOUT_SECONDS = 10;

    private final String uri;
    private final String databaseName;

    @Inject
    public MongoProducer(
        @ConfigProperty(name = "mongodb.uri") String uri,
        @ConfigProperty(name = "mongodb.database") String databaseName
    ) {
        this.uri = uri;
        this.databaseName = databaseName;
    }

    /** For the CDI proxy only. */
    protected MongoProducer() {
        this.uri = "";
        this.databaseName = "";
    }

    @Produces
    @ApplicationScoped
    public MongoClient createMongoClient() {
        return MongoClients.create(settings(uri));
    }

    @Produces
    @ApplicationScoped
    public MongoDatabase createMongoDatabase(MongoClient mongoClient) {
        return mongoClient.getDatabase(databaseName);
    }

    public void closeMongoClient(@Disposes MongoClient mongoClient) {
        mongoClient.close();
    }

    static MongoClientSettings settings(String uri) {
        CodecRegistry pojoCodecRegistry =
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build());

        CodecRegistry codecRegistry =
                CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);

        return MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(uri))
            .codecRegistry(codecRegistry)
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .writeConcern(WriteConcern.MAJORITY)
            .applyToClusterSettings(
                cluster -> cluster.serverSelectionTimeout(SERVER_SELECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            )
            .applyToSocketSettings(
                socket -> socket.connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            )
            .build();
    }
}
