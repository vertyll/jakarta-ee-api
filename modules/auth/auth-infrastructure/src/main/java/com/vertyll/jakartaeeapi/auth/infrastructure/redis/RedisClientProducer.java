package com.vertyll.jakartaeeapi.auth.infrastructure.redis;

import java.util.Optional;

import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

/**
 * The Redis client, at {@code redis.address} ({@code REDIS_ADDRESS}), with {@code redis.password}
 * ({@code REDIS_PASSWORD}) where the server asks for one. The address has no default, so a deployment that leaves it
 * out fails to start.
 */
@Singleton
public class RedisClientProducer {
    private final String address;
    private final Optional<String> password;

    @Inject
    public RedisClientProducer(
        @ConfigProperty(name = "redis.address") String address,
        @ConfigProperty(name = "redis.password") Optional<String> password
    ) {
        this.address = address;
        this.password = password;
    }

    @Produces
    @Singleton
    public RedissonClient redisClient() {
        Config config = new Config().setLazyInitialization(true);
        SingleServerConfig server = config.useSingleServer().setAddress(address);
        password.filter(value -> !value.isBlank()).ifPresent(server::setPassword);
        return Redisson.create(config);
    }

    public void close(@Disposes RedissonClient client) {
        client.shutdown();
    }
}
