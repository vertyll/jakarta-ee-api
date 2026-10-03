package com.vertyll.jakartaeeapi.auth;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vertyll.jakartaeeapi.user.UserService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {
    private static final AuthSession SESSION = new AuthSession(
        new KeycloakIdentity("subject", "ada@jakarta-ee-api.local", "Ada", "Lovelace", List.of("USER")),
        "access",
        "refresh",
        Instant.EPOCH
    );

    private final KeycloakTokenClient tokens = mock(KeycloakTokenClient.class);
    private final UserService users = mock(UserService.class);

    @Test
    void signingInProvisionsTheAccount() {
        when(tokens.exchange("code", "verifier")).thenReturn(SESSION);

        new SessionService(tokens, users).signIn("code", "verifier");

        verify(users).sync(SESSION.identity());
        verify(tokens, never()).revoke(any());
    }

    @Test
    void aFailedProvisioningRevokesTheNewSession() {
        when(tokens.exchange("code", "verifier")).thenReturn(SESSION);
        when(users.sync(any())).thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(() -> new SessionService(tokens, users).signIn("code", "verifier"))
            .hasMessage("database down");

        verify(tokens).revoke("refresh");
    }
}
