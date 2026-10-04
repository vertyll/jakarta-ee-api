package com.vertyll.jakartaeeapi.auth;

import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

@Singleton
public class RedisClientProducer {

    @Produces
    @Singleton
    public RedissonClient redisClient() {
        Config config = new Config().setLazyInitialization(true);
        SingleServerConfig server = config.useSingleServer().setAddress(RedisSettings.address());
        String password = RedisSettings.password();
        if (password != null) {
            server.setPassword(password);
        }
        return Redisson.create(config);
    }

    public void close(@Disposes RedissonClient client) {
        client.shutdown();
    }
}
