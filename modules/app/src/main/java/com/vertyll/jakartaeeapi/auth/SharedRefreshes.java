package com.vertyll.jakartaeeapi.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class SharedRefreshes {
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final Duration RESULT_TTL = Duration.ofSeconds(30);
    private static final Duration WAIT_INTERVAL = Duration.ofMillis(100);
    private static final int WAIT_ATTEMPTS = 50;
    private static final String SEPARATOR = "\n";

    private final @Nullable RedissonClient redis;
    private final String keyPrefix;

    @Inject
    public SharedRefreshes(RedissonClient redis) {
        this(redis, RedisSettings.keyPrefix());
    }

    SharedRefreshes(@Nullable RedissonClient redis, String keyPrefix) {
        this.redis = redis;
        this.keyPrefix = keyPrefix;
    }

    public static SharedRefreshes inProcessOnly() {
        return new SharedRefreshes(null, "");
    }

    public TokenPair refresh(String refreshToken, Supplier<TokenPair> keycloak) {
        RedissonClient store = redis;
        if (store == null) {
            return keycloak.get();
        }
        String id = sha256(refreshToken);
        RBucket<String> lock = store.getBucket(keyPrefix + ":refresh-lock:" + id, StringCodec.INSTANCE);
        RBucket<String> result = store.getBucket(keyPrefix + ":refresh-result:" + id, StringCodec.INSTANCE);
        Optional<TokenPair> shared;
        boolean leader;
        try {
            shared = read(result);
            leader = shared.isEmpty() && lock.setIfAbsent("1", LOCK_TTL);
        } catch (RedisException e) {
            log.warn("Redis unavailable, refreshing without coordinating replicas: {}", e.getMessage());
            return keycloak.get();
        }
        if (shared.isPresent()) {
            return shared.get();
        }
        if (leader) {
            return lead(lock, result, keycloak);
        }
        return awaitOtherReplica(lock, result).orElseGet(keycloak);
    }

    private static TokenPair lead(RBucket<String> lock, RBucket<String> result, Supplier<TokenPair> keycloak) {
        boolean refreshed = false;
        TokenPair pair;
        try {
            pair = keycloak.get();
            refreshed = true;
        } finally {
            if (!refreshed) {
                release(lock);
            }
        }
        try {
            result.set(pair.accessToken() + SEPARATOR + pair.refreshToken(), RESULT_TTL);
        } catch (RedisException e) {
            log.warn("Could not share the refreshed tokens with other replicas: {}", e.getMessage());
        }
        return pair;
    }

    private static Optional<TokenPair> awaitOtherReplica(RBucket<String> lock, RBucket<String> result) {
        try {
            for (int attempt = 0; attempt < WAIT_ATTEMPTS; attempt++) {
                Thread.sleep(WAIT_INTERVAL);
                Optional<TokenPair> shared = read(result);
                if (shared.isPresent() || !lock.isExists()) {
                    return shared;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RedisException e) {
            log.warn("Redis unavailable while waiting for another replica's refresh: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private static Optional<TokenPair> read(RBucket<String> result) {
        String value = result.get();
        if (value == null) {
            return Optional.empty();
        }
        int separator = value.indexOf(SEPARATOR);
        return separator < 0 ? Optional.empty()
                : Optional.of(new TokenPair(value.substring(0, separator), value.substring(separator + 1)));
    }

    private static void release(RBucket<String> lock) {
        try {
            lock.delete();
        } catch (RedisException e) {
            log.warn("Could not release the refresh lock, it expires on its own: {}", e.getMessage());
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }

    public record TokenPair(String accessToken, String refreshToken) {
        @Override
        public String toString() {
            return "TokenPair[accessToken=***, refreshToken=***]";
        }
    }
}
