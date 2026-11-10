package com.vertyll.jakartaeeapi.user.infrastructure.persistence;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import com.vertyll.jakartaeeapi.user.domain.UserAccount;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The accounts' repository against a real MongoDB, the version the local setup runs. */
class MongoUserRepositoryTest {
    private static final int MONGO_PORT = 27_017;

    // The @AfterAll method stops the container.
    @SuppressWarnings("resource")
    private static final GenericContainer<?> MONGO =
            new GenericContainer<>(DockerImageName.parse("mongo:9")).withStartupTimeout(Duration.ofMinutes(2))
                .withStartupAttempts(3)
                .withExposedPorts(MONGO_PORT);

    private static MongoClient client;
    private MongoUserRepository repository;

    @BeforeAll
    static void start() {
        MONGO.start();
        client = MongoClients
            .create(MongoProducer.settings("mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(MONGO_PORT)));
    }

    @AfterAll
    static void stop() {
        client.close();
        MONGO.stop();
    }

    @BeforeEach
    void freshDatabase() {
        MongoDatabase database = client.getDatabase("users_test");
        database.drop();
        repository = new MongoUserRepository(database);
        repository.createIndexes();
    }

    @Test
    void anAccountIsFoundByItsKeycloakSubject() {
        UserAccount account = new UserAccount("subject", "ada@example.test", "ada", List.of("USER"));

        repository.save(account);

        assertThat(repository.findByKeycloakId("subject")).contains(account);
        assertThat(repository.findByKeycloakId("someone-else")).isEmpty();
    }

    @Test
    void savingTheSameSubjectAgainReplacesTheAccount() {
        repository.save(new UserAccount("subject", "ada@example.test", "ada", List.of("USER")));

        repository.save(new UserAccount("subject", "ada@new.test", "ada", List.of("USER", "ADMIN")));

        assertThat(repository.findByKeycloakId("subject"))
            .contains(new UserAccount("subject", "ada@new.test", "ada", List.of("USER", "ADMIN")));
    }

    @Test
    void twoSubjectsCannotShareAnEmail() {
        repository.save(new UserAccount("first", "ada@example.test", "ada", List.of("USER")));
        UserAccount sameEmail = new UserAccount("second", "ada@example.test", "ada-2", List.of("USER"));

        assertThatThrownBy(() -> repository.save(sameEmail)).isInstanceOf(MongoWriteException.class);
    }

    @Test
    void theUniqueIndexesAreNamedAndReplaceTheOnesOfAnEarlierRelease() {
        MongoDatabase database = client.getDatabase("users_rename_test");
        database.drop();
        MongoCollection<Document> users = database.getCollection("users");
        users.createIndex(Indexes.ascending("keycloakId"), new IndexOptions().unique(true));
        users.createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));
        users.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));

        MongoUserRepository upgraded = new MongoUserRepository(database);
        upgraded.createIndexes();
        upgraded.createIndexes();

        List<Document> indexes = new ArrayList<>();
        users.listIndexes().forEach(indexes::add);
        assertThat(indexes).extracting(index -> index.getString("name"))
            .containsExactlyInAnyOrder("_id_", "uk_users_keycloak_id", "uk_users_email", "uk_users_username");
        assertThat(indexes).filteredOn(index -> index.getString("name").startsWith("uk_"))
            .allSatisfy(index -> assertThat(index.getBoolean("unique")).isTrue());
    }
}
