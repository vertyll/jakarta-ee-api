package com.vertyll.jakartaeeapi.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

@ApplicationScoped
public class MongoDBProducer {

    @Produces
    @ApplicationScoped
    public MongoClient createMongoClient() {
        String connectionString =
                System.getenv().getOrDefault("MONGODB_URI", "mongodb://localhost:27017");

        CodecRegistry pojoCodecRegistry =
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build());

        CodecRegistry codecRegistry =
                CodecRegistries.fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);

        MongoClientSettings settings =
                MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(connectionString))
                        .codecRegistry(codecRegistry)
                        .build();

        return MongoClients.create(settings);
    }

    @Produces
    @ApplicationScoped
    public MongoDatabase createMongoDatabase(MongoClient mongoClient) {
        String databaseName = System.getenv().getOrDefault("MONGODB_DATABASE", "exampledb");

        return mongoClient.getDatabase(databaseName);
    }

    public void closeMongoClient(
            @Disposes
            MongoClient mongoClient
    ) {
        mongoClient.close();
    }
}
