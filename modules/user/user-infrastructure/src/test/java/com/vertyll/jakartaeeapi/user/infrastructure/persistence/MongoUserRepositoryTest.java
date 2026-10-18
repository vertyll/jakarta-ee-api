package com.vertyll.jakartaeeapi.user.infrastructure.persistence;

import java.util.List;

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
import com.mongodb.client.MongoDatabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The accounts' repository against a real MongoDB, the version the local setup runs. */
class MongoUserRepositoryTest {
    private static final int MONGO_PORT = 27_017;
    private static final GenericContainer<?> MONGO =
            new GenericContainer<>(DockerImageName.parse("mongo:8")).withExposedPorts(MONGO_PORT);

    private static MongoClient client;
    private MongoUserRepository repository;

    @BeforeAll
    static void start() {
        MONGO.start();
        client = MongoClients.create("mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(MONGO_PORT));
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
}
