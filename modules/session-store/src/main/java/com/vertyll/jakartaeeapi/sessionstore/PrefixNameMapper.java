package com.vertyll.jakartaeeapi.sessionstore;

import org.jspecify.annotations.Nullable;
import org.redisson.config.NameMapper;

public final class PrefixNameMapper implements NameMapper {
    private static final String KEY_PREFIX_VARIABLE = "REDIS_KEY_PREFIX";
    private static final String DEFAULT_KEY_PREFIX = "jakarta-ee-api";
    private static final String SESSION_SEGMENT = ":session:";

    private final String prefix;

    public PrefixNameMapper() {
        this(System.getenv(KEY_PREFIX_VARIABLE));
    }

    PrefixNameMapper(@Nullable String application) {
        String name = application == null || application.isBlank() ? DEFAULT_KEY_PREFIX : application;
        this.prefix = name + SESSION_SEGMENT;
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
