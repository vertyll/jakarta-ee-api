package com.vertyll.jakartaeeapi.auth;

import jakarta.inject.Singleton;

@Singleton
public class AuthSettings {
    private final String serverUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final String audience;
    private final String callbackUrl;
    private final String postLoginUrl;

    public AuthSettings() {
        this(
            requiredEnv("KEYCLOAK_SERVER_URL"),
            requiredEnv("KEYCLOAK_REALM"),
            requiredEnv("KEYCLOAK_CLIENT_ID"),
            requiredEnv("KEYCLOAK_CLIENT_SECRET"),
            requiredEnv("KEYCLOAK_AUDIENCE"),
            requiredEnv("AUTH_CALLBACK_URL"),
            requiredEnv("AUTH_POST_LOGIN_URL")
        );
    }

    @SuppressWarnings("java:S107")
    public AuthSettings(
        String serverUrl,
        String realm,
        String clientId,
        String clientSecret,
        String audience,
        String callbackUrl,
        String postLoginUrl
    ) {
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.audience = audience;
        this.callbackUrl = callbackUrl;
        this.postLoginUrl = postLoginUrl;
    }

    public String realmUrl() {
        return serverUrl + "/realms/" + realm;
    }

    public String endpoint(String name) {
        return realmUrl() + "/protocol/openid-connect/" + name;
    }

    public String clientId() {
        return clientId;
    }

    public String clientSecret() {
        return clientSecret;
    }

    public String audience() {
        return audience;
    }

    public String callbackUrl() {
        return callbackUrl;
    }

    public String postLoginUrl() {
        return postLoginUrl;
    }

    @Override
    public String toString() {
        return "AuthSettings[realmUrl=" + realmUrl() + ", clientId=" + clientId + ", clientSecret=***]";
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set");
        }
        return value;
    }
}
