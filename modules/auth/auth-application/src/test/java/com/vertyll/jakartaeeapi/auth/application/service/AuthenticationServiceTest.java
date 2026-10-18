package com.vertyll.jakartaeeapi.auth.application.service;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vertyll.jakartaeeapi.auth.application.port.outbound.TokenEndpoint;
import com.vertyll.jakartaeeapi.auth.application.port.outbound.TokenVerifier;
import com.vertyll.jakartaeeapi.auth.contracts.SignedInAccounts;
import com.vertyll.jakartaeeapi.auth.contracts.SignedInPerson;
import com.vertyll.jakartaeeapi.auth.domain.AuthException;
import com.vertyll.jakartaeeapi.auth.domain.AuthSession;
import com.vertyll.jakartaeeapi.auth.domain.Identity;
import com.vertyll.jakartaeeapi.auth.domain.TokenPair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {
    private static final Identity IDENTITY =
            new Identity("subject", "ada@jakarta-ee-api.local", "ada", List.of("USER"));
    private static final TokenPair ISSUED = new TokenPair("access", "refresh");
    private static final Instant EXPIRY = Instant.parse("2026-10-18T12:00:00Z");

    private final TokenEndpoint tokens = mock(TokenEndpoint.class);
    private final TokenVerifier verifier = mock(TokenVerifier.class);
    private final SignedInAccounts accounts = mock(SignedInAccounts.class);
    private final AuthenticationService service = new AuthenticationService(tokens, verifier, accounts);

    @Test
    void signingInVerifiesTheTokenAndProvisionsTheAccount() {
        when(tokens.exchange("code", "verifier")).thenReturn(ISSUED);
        when(verifier.verify("access")).thenReturn(new TokenVerifier.VerifiedToken(IDENTITY, EXPIRY));

        AuthSession session = service.signIn("code", "verifier");

        assertThat(session).isEqualTo(new AuthSession(IDENTITY, "access", "refresh", EXPIRY));
        verify(accounts).signedIn(new SignedInPerson("subject", "ada@jakarta-ee-api.local", "ada", List.of("USER")));
        verify(tokens, never()).revoke(any());
    }

    @Test
    void aFailedProvisioningRevokesTheNewSession() {
        when(tokens.exchange("code", "verifier")).thenReturn(ISSUED);
        when(verifier.verify("access")).thenReturn(new TokenVerifier.VerifiedToken(IDENTITY, EXPIRY));
        doThrow(new IllegalStateException("database down")).when(accounts).signedIn(any());

        assertThatThrownBy(() -> service.signIn("code", "verifier")).hasMessage("database down");

        verify(tokens).revoke("refresh");
    }

    @Test
    void aTokenThatDoesNotVerifyRejectsTheSignInBeforeAnyAccountIsTouched() {
        when(tokens.exchange("code", "verifier")).thenReturn(ISSUED);
        when(verifier.verify("access")).thenThrow(AuthException.rejected(AuthException.TOKEN_INVALID));

        assertThatThrownBy(() -> service.signIn("code", "verifier")).isInstanceOfSatisfying(
            AuthException.class,
            e -> assertThat(e.getMessageKey()).isEqualTo(AuthException.SIGN_IN_REJECTED)
        );
        verify(accounts, never()).signedIn(any());
    }

    @Test
    void aRefreshedTokenThatDoesNotVerifyEndsTheSession() {
        when(tokens.refresh("refresh")).thenReturn(new TokenPair("access-2", "refresh-2"));
        when(verifier.verify("access-2")).thenThrow(AuthException.rejected(AuthException.TOKEN_INVALID));
        AuthSession session = new AuthSession(IDENTITY, "access", "refresh", EXPIRY);

        assertThatThrownBy(() -> service.refresh(session)).isInstanceOfSatisfying(
            AuthException.class,
            e -> assertThat(e.getMessageKey()).isEqualTo(AuthException.SESSION_EXPIRED)
        );
    }

    @Test
    void signingOutRevokesTheRefreshToken() {
        service.signOut(new AuthSession(IDENTITY, "access", "refresh", EXPIRY));

        verify(tokens).revoke("refresh");
    }

    @Test
    void aTokenIsAuthenticatedAsTheIdentityItCarries() {
        when(verifier.verify("access")).thenReturn(new TokenVerifier.VerifiedToken(IDENTITY, EXPIRY));

        assertThat(service.authenticate("access")).isEqualTo(IDENTITY);
    }
}
