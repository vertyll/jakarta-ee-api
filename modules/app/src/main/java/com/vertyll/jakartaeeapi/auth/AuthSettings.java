package com.vertyll.jakartaeeapi.auth;

public record AuthSettings(
    String realmUrl,
    String clientId,
    String clientSecret,
    String audience,
    String callbackUrl,
    String postLoginUrl
) {

    public static AuthSettings fromEnvironment() {
        return new AuthSettings(
            requiredEnv("KEYCLOAK_REALM_URL"),
            requiredEnv("KEYCLOAK_CLIENT_ID"),
            requiredEnv("KEYCLOAK_CLIENT_SECRET"),
            requiredEnv("KEYCLOAK_AUDIENCE"),
            requiredEnv("AUTH_CALLBACK_URL"),
            requiredEnv("AUTH_POST_LOGIN_URL")
        );
    }

    public String endpoint(String name) {
        return realmUrl + "/protocol/openid-connect/" + name;
    }

    @Override
    public String toString() {
        return "AuthSettings[realmUrl=" + realmUrl + ", clientId=" + clientId + ", clientSecret=***]";
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set");
        }
        return value;
    }
}
