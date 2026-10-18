package com.vertyll.jakartaeeapi.auth.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthSessionTest {
    private static final Instant EXPIRY = Instant.parse("2026-10-18T12:00:00Z");
    private static final AuthSession SESSION = new AuthSession(
        new Identity("subject", "ada@example.test", "ada", List.of("USER")),
        "access",
        "refresh",
        EXPIRY
    );

    @Test
    void aSessionIsRefreshedOnceItsTokenIsWithinTheSkewOfExpiring() {
        Duration skew = Duration.ofSeconds(30);

        assertThat(SESSION.needsRefreshAt(EXPIRY.minusSeconds(31), skew)).isFalse();
        assertThat(SESSION.needsRefreshAt(EXPIRY.minusSeconds(30), skew)).isTrue();
        assertThat(SESSION.needsRefreshAt(EXPIRY.plusSeconds(1), skew)).isTrue();
    }

    @Test
    void neitherTheSessionNorTheTransactionShowsItsSecrets() {
        assertThat(SESSION.toString()).doesNotContain("access,", "refresh,")
            .contains("accessToken=***", "refreshToken=***");
        assertThat(new SignInTransaction("state", "verifier"))
            .hasToString("SignInTransaction[state=***, codeVerifier=***]");
        assertThat(new TokenPair("access", "refresh")).hasToString("TokenPair[accessToken=***, refreshToken=***]");
    }
}
