package com.vertyll.jakartaeeapi.auth;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;

import com.vertyll.jakartaeeapi.auth.SharedRefreshes.TokenPair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SharedRefreshesTest {
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

    private static RedissonClient redis;

    @BeforeAll
    static void startRedis() {
        REDIS.start();
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        redis = Redisson.create(config);
    }

    @AfterAll
    static void stopRedis() {
        redis.shutdown();
        REDIS.stop();
    }

    @Test
    void twoReplicasRefreshingOneTokenReachKeycloakOnce() throws InterruptedException, ExecutionException, TimeoutException {
        SharedRefreshes first = new SharedRefreshes(redis, "test-a");
        SharedRefreshes second = new SharedRefreshes(redis, "test-a");
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        CompletableFuture<TokenPair> leader = CompletableFuture.supplyAsync(() -> first.refresh("refresh-1", () -> {
            calls.incrementAndGet();
            entered.countDown();
            await(release);
            return new TokenPair("access-2", "refresh-2");
        }));
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        CompletableFuture<TokenPair> follower = CompletableFuture.supplyAsync(() -> second.refresh("refresh-1", () -> {
            calls.incrementAndGet();
            return new TokenPair("access-3", "refresh-3");
        }));
        release.countDown();

        assertThat(leader.get(10, TimeUnit.SECONDS).refreshToken()).isEqualTo("refresh-2");
        assertThat(follower.get(10, TimeUnit.SECONDS).refreshToken()).isEqualTo("refresh-2");
        assertThat(calls).hasValue(1);
    }

    @Test
    void aStaleRequestReceivesTheTokensAlreadyIssued() {
        new SharedRefreshes(redis, "test-b").refresh("refresh-1", () -> new TokenPair("access-2", "refresh-2"));

        TokenPair stale = new SharedRefreshes(redis, "test-b").refresh("refresh-1", () -> {
            throw new IllegalStateException("Keycloak must not be asked twice");
        });

        assertThat(stale.accessToken()).isEqualTo("access-2");
    }

    @Test
    void aRefusedRefreshIsNotShared() {
        SharedRefreshes replica = new SharedRefreshes(redis, "test-c");
        assertThatThrownBy(() -> replica.refresh("refresh-1", () -> {
            throw new IllegalStateException("refused");
        })).isInstanceOf(IllegalStateException.class);

        TokenPair retried = replica.refresh("refresh-1", () -> new TokenPair("access-2", "refresh-2"));

        assertThat(retried.refreshToken()).isEqualTo("refresh-2");
    }

    @Test
    void keysCarryTheApplicationPrefixAndNeverTheToken() {
        new SharedRefreshes(redis, "test-d").refresh("secret-refresh", () -> new TokenPair("access", "refresh"));

        assertThat(redis.getKeys().getKeysByPattern("test-d:refresh-result:*")).hasSize(1);
        assertThat(redis.getKeys().getKeysByPattern("*secret-refresh*")).isEmpty();
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
