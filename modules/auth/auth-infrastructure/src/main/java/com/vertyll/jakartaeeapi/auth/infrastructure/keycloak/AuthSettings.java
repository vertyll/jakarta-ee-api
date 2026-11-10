package com.vertyll.jakartaeeapi.auth.infrastructure.keycloak;

public record AuthSettings(
    String realmUrl,
    String clientId,
    String clientSecret,
    String audience,
    String callbackUrl,
    String postLoginUrl
) {

    public String endpoint(String name) {
        return realmUrl + "/protocol/openid-connect/" + name;
    }

    @Override
    public String toString() {
        return "AuthSettings[realmUrl=" + realmUrl + ", clientId=" + clientId + ", clientSecret=***]";
    }
}
