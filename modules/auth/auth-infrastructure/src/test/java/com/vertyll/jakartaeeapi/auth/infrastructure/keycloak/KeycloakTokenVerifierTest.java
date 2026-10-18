package com.vertyll.jakartaeeapi.auth.infrastructure.keycloak;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vertyll.jakartaeeapi.auth.application.port.outbound.TokenVerifier;
import com.vertyll.jakartaeeapi.auth.domain.AuthException;

import com.nimbusds.jose.JOSEException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeycloakTokenVerifierTest {
    private final TestTokens tokens;
    private final KeycloakTokenVerifier verifier;

    KeycloakTokenVerifierTest() throws JOSEException {
        tokens = new TestTokens();
        verifier = tokens.verifier();
    }

    @Test
    void acceptsATokenForThisApiAndKeepsOnlyApplicationRoles() throws JOSEException {
        TokenVerifier.VerifiedToken verified = verifier.verify(tokens.validToken());

        assertThat(verified.identity().keycloakId()).isEqualTo(TestTokens.SUBJECT);
        assertThat(verified.identity().roles()).containsExactly("USER");
        assertThat(verified.identity().username()).isEqualTo("ada");
    }

    @Test
    void rejectsATokenIssuedForAnotherClient() throws JOSEException {
        String foreign = tokens.token(TestTokens.ISSUER, "other-api", List.of("USER"));

        assertThatThrownBy(() -> verifier.verify(foreign)).isInstanceOf(AuthException.class);
    }

    @Test
    void rejectsATokenFromAnotherIssuer() throws JOSEException {
        String foreign = tokens.token("http://evil.test/realms/jakarta-ee-api", TestTokens.AUDIENCE, List.of("USER"));

        assertThatThrownBy(() -> verifier.verify(foreign)).isInstanceOf(AuthException.class);
    }

    @Test
    void rejectsSomethingThatIsNotAToken() {
        assertThatThrownBy(() -> verifier.verify("not-a-token")).isInstanceOf(AuthException.class);
    }
}
