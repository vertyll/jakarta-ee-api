package com.vertyll.jakartaeeapi.auth.infrastructure.redis;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
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
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;

import com.vertyll.jakartaeeapi.auth.domain.TokenPair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SharedRefreshesTest {
    // The @AfterAll method stops the container.
    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:8-alpine").withStartupTimeout(Duration.ofMinutes(2))
                .withStartupAttempts(3)
                .withExposedPorts(6379);

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

    @Test
    void withoutRedisEachReplicaRefreshesOnItsOwn() {
        TokenPair pair = SharedRefreshes.inProcessOnly().refresh("refresh-1", () -> new TokenPair("access", "refresh"));

        assertThat(pair).isEqualTo(new TokenPair("access", "refresh"));
    }

    @Test
    void aRedisThatFailsDoesNotStopTheRefresh() {
        RBucket<String> broken = mock();
        when(broken.get()).thenThrow(new RedisException("down"));
        RedissonClient store = mock();
        when(store.<String>getBucket(anyString(), any(Codec.class))).thenReturn(broken);

        TokenPair pair =
                new SharedRefreshes(store, "test-e").refresh("refresh-1", () -> new TokenPair("access", "refresh"));

        assertThat(pair).isEqualTo(new TokenPair("access", "refresh"));
    }

    @Test
    void tokensThatCannotBeSharedAreStillReturned() {
        RBucket<String> bucket = mock();
        when(bucket.setIfAbsent(anyString(), any(Duration.class))).thenReturn(true);
        doThrow(new RedisException("down")).when(bucket).set(anyString(), any(Duration.class));
        RedissonClient store = mock();
        when(store.<String>getBucket(anyString(), any(Codec.class))).thenReturn(bucket);

        TokenPair pair =
                new SharedRefreshes(store, "test-f").refresh("refresh-1", () -> new TokenPair("access", "refresh"));

        assertThat(pair).isEqualTo(new TokenPair("access", "refresh"));
    }

    @Test
    void aRefusedRefreshIsNotHiddenByALockThatCannotBeReleased() {
        RBucket<String> bucket = mock();
        when(bucket.setIfAbsent(anyString(), any(Duration.class))).thenReturn(true);
        when(bucket.delete()).thenThrow(new RedisException("down"));
        RedissonClient store = mock();
        when(store.<String>getBucket(anyString(), any(Codec.class))).thenReturn(bucket);

        SharedRefreshes refreshes = new SharedRefreshes(store, "test-g");

        assertThatThrownBy(() -> refreshes.refresh("refresh-1", () -> {
            throw new IllegalArgumentException("refused");
        })).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aValueThatIsNoTokenPairIsNotShared() {
        redis.<String>getBucket("test-h:refresh-result:" + sha256("refresh-1"), StringCodec.INSTANCE).set("garbage");

        TokenPair pair =
                new SharedRefreshes(redis, "test-h").refresh("refresh-1", () -> new TokenPair("access", "refresh"));

        assertThat(pair).isEqualTo(new TokenPair("access", "refresh"));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
