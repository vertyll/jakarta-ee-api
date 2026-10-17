package com.vertyll.jakartaeeapi.user;

import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;

@Singleton
public class UserRepository {
    private static final String COLLECTION = "users";
    private static final String KEYCLOAK_ID = "keycloakId";
    private static final String EMAIL = "email";
    private static final String USERNAME = "username";
    private static final String ROLES = "roles";

    private final MongoDatabase database;

    @Inject
    public UserRepository(MongoDatabase database) {
        this.database = database;
    }

    @PostConstruct
    void createIndexes() {
        collection().createIndex(Indexes.ascending(KEYCLOAK_ID), new IndexOptions().unique(true));
        collection().createIndex(Indexes.ascending(EMAIL), new IndexOptions().unique(true));
        collection().createIndex(Indexes.ascending(USERNAME), new IndexOptions().unique(true));
    }

    public UserAccount save(UserAccount account) {
        Bson byId = Filters.eq(KEYCLOAK_ID, account.keycloakId());
        Document document = new Document(KEYCLOAK_ID, account.keycloakId()).append(EMAIL, account.email())
            .append(USERNAME, account.username())
            .append(ROLES, account.roles());
        collection().replaceOne(byId, document, new ReplaceOptions().upsert(true));
        return account;
    }

    public Optional<UserAccount> findByKeycloakId(String keycloakId) {
        return Optional.ofNullable(collection().find(Filters.eq(KEYCLOAK_ID, keycloakId)).first())
            .map(UserRepository::toAccount);
    }

    private MongoCollection<Document> collection() {
        return database.getCollection(COLLECTION);
    }

    private static UserAccount toAccount(Document document) {
        return new UserAccount(
            document.getString(KEYCLOAK_ID),
            document.getString(EMAIL),
            document.getString(USERNAME),
            document.getList(ROLES, String.class, List.of())
        );
    }
}
