package com.vertyll.jakartaeeapi.sessionstore;

import org.eclipse.microprofile.config.ConfigProvider;
import org.redisson.config.NameMapper;

/**
 * Puts the session caches under the application's key prefix, {@code redis.key-prefix} ({@code REDIS_KEY_PREFIX}),
 * the same the application's own keys use. Redisson creates it from {@code redisson-jcache.yaml}, outside CDI, so it
 * reads the prefix from MicroProfile Config directly.
 */
public final class PrefixNameMapper implements NameMapper {
    private static final String KEY_PREFIX_PROPERTY = "redis.key-prefix";
    private static final String DEFAULT_KEY_PREFIX = "jakarta-ee-api";
    private static final String SESSION_SEGMENT = ":session:";

    private final String prefix;

    public PrefixNameMapper() {
        this(
            ConfigProvider.getConfig()
                .getOptionalValue(KEY_PREFIX_PROPERTY, String.class)
                .filter(value -> !value.isBlank())
                .orElse(DEFAULT_KEY_PREFIX)
        );
    }

    PrefixNameMapper(String application) {
        this.prefix = application + SESSION_SEGMENT;
    }

    @Override
    public String map(String name) {
        return prefix + name;
    }

    @Override
    public String unmap(String name) {
        return name.startsWith(prefix) ? name.substring(prefix.length()) : name;
    }
}
