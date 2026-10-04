package com.vertyll.jakartaeeapi.auth;

import org.jspecify.annotations.Nullable;

final class RedisSettings {
    private static final String DEFAULT_KEY_PREFIX = "jakarta-ee-api";
    private static final String DEFAULT_ADDRESS = "redis://localhost:6379";

    private RedisSettings() {
    }

    static String address() {
        String address = System.getenv("REDIS_ADDRESS");
        return address == null || address.isBlank() ? DEFAULT_ADDRESS : address;
    }

    static @Nullable String password() {
        String password = System.getenv("REDIS_PASSWORD");
        return password == null || password.isBlank() ? null : password;
    }

    static String keyPrefix() {
        String prefix = System.getenv("REDIS_KEY_PREFIX");
        return prefix == null || prefix.isBlank() ? DEFAULT_KEY_PREFIX : prefix;
    }
}
