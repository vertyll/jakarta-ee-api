package com.vertyll.jakartaeeapi.auth.infrastructure.keycloak;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthSettingsTest {
    private final AuthSettings settings = new AuthSettings(
        "http://keycloak.test/realms/app",
        "app",
        "secret",
        "app",
        "http://app.test/api/auth/callback",
        "http://app.test/"
    );

    @Test
    void anEndpointIsOneOfTheRealmsOpenIdConnectPaths() {
        assertThat(settings.endpoint("token"))
            .isEqualTo("http://keycloak.test/realms/app/protocol/openid-connect/token");
    }

    @Test
    void theClientSecretIsNeverShown() {
        assertThat(settings.toString()).contains("clientSecret=***").doesNotContain("secret,", "=secret");
    }
}
