package com.vertyll.jakartaeeapi.auth;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JOSEException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenVerifierTest {
    private final TestTokens tokens;
    private final TokenVerifier verifier;

    TokenVerifierTest() throws JOSEException {
        tokens = new TestTokens();
        verifier = tokens.verifier();
    }

    @Test
    void acceptsATokenForThisApiAndKeepsOnlyApplicationRoles() throws JOSEException {
        TokenVerifier.VerifiedToken verified = verifier.verify(tokens.validToken());

        assertThat(verified.identity().keycloakId()).isEqualTo(TestTokens.SUBJECT);
        assertThat(verified.identity().roles()).containsExactly("USER");
        assertThat(verified.identity().lastName()).isEmpty();
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
