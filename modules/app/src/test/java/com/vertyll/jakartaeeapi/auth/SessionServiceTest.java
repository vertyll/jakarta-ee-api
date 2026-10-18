package com.vertyll.jakartaeeapi.auth;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {
    private static final AuthSession SESSION = new AuthSession(
        new KeycloakIdentity("subject", "ada@jakarta-ee-api.local", "ada", List.of("USER")),
        "access",
        "refresh",
        Instant.EPOCH
    );

    private final KeycloakTokenClient tokens = mock(KeycloakTokenClient.class);
    private final SignedInAccounts accounts = mock(SignedInAccounts.class);

    @Test
    void signingInProvisionsTheAccount() {
        when(tokens.exchange("code", "verifier")).thenReturn(SESSION);

        new SessionService(tokens, accounts).signIn("code", "verifier");

        verify(accounts).signedIn(SESSION.identity());
        verify(tokens, never()).revoke(any());
    }

    @Test
    void aFailedProvisioningRevokesTheNewSession() {
        when(tokens.exchange("code", "verifier")).thenReturn(SESSION);
        doThrow(new IllegalStateException("database down")).when(accounts).signedIn(any());

        assertThatThrownBy(() -> new SessionService(tokens, accounts).signIn("code", "verifier"))
            .hasMessage("database down");

        verify(tokens).revoke("refresh");
    }
}
