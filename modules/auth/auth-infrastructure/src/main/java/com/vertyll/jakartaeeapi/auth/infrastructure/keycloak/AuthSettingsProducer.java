package com.vertyll.jakartaeeapi.auth.infrastructure.keycloak;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * The realm and the client, from MicroProfile Config: {@code keycloak.realm-url} is read from
 * {@code KEYCLOAK_REALM_URL} in the environment, and so on. None has a default, so a deployment that leaves one out
 * fails to start instead of signing in against the wrong realm.
 */
@Singleton
public class AuthSettingsProducer {
    private final AuthSettings settings;

    @Inject
    public AuthSettingsProducer(
        @ConfigProperty(name = "keycloak.realm-url") String realmUrl,
        @ConfigProperty(name = "keycloak.client-id") String clientId,
        @ConfigProperty(name = "keycloak.client-secret") String clientSecret,
        @ConfigProperty(name = "keycloak.audience") String audience,
        @ConfigProperty(name = "auth.callback-url") String callbackUrl,
        @ConfigProperty(name = "auth.post-login-url") String postLoginUrl
    ) {
        this.settings = new AuthSettings(realmUrl, clientId, clientSecret, audience, callbackUrl, postLoginUrl);
    }

    @Produces
    @Singleton
    public AuthSettings authSettings() {
        return settings;
    }
}
