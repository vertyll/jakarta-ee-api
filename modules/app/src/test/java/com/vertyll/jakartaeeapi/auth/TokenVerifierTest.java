package com.vertyll.jakartaeeapi.auth;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JOSEException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenVerifierTest {
    private final TestTokens tokens;

    TokenVerifierTest() throws JOSEException {
        tokens = new TestTokens();
    }

    @Test
    void acceptsATokenForThisApiAndKeepsOnlyApplicationRoles() throws JOSEException {
        TokenVerifier.VerifiedToken verified = tokens.verifier().verify(tokens.validToken());

        assertThat(verified.identity().keycloakId()).isEqualTo(TestTokens.SUBJECT);
        assertThat(verified.identity().roles()).containsExactly("USER");
        assertThat(verified.identity().lastName()).isEmpty();
    }

    @Test
    void rejectsATokenIssuedForAnotherClient() throws JOSEException {
        String foreign = tokens.token(TestTokens.ISSUER, "other-api", List.of("USER"));

        assertThatThrownBy(() -> tokens.verifier().verify(foreign)).isInstanceOf(AuthException.class);
    }

    @Test
    void rejectsATokenFromAnotherIssuer() throws JOSEException {
        String foreign = tokens.token("http://evil.test/realms/jakarta-ee-api", TestTokens.AUDIENCE, List.of("USER"));

        assertThatThrownBy(() -> tokens.verifier().verify(foreign)).isInstanceOf(AuthException.class);
    }

    @Test
    void rejectsSomethingThatIsNotAToken() {
        assertThatThrownBy(() -> tokens.verifier().verify("not-a-token")).isInstanceOf(AuthException.class);
    }
}
