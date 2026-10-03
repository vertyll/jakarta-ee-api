package com.vertyll.jakartaeeapi.auth;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
public class AuthSettingsProducer {

    @Produces
    @Singleton
    public AuthSettings authSettings() {
        return AuthSettings.fromEnvironment();
    }
}
