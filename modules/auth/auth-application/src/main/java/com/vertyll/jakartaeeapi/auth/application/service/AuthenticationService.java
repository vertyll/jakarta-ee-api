package com.vertyll.jakartaeeapi.auth.application.service;

import com.vertyll.jakartaeeapi.auth.api.SignedInAccounts;
import com.vertyll.jakartaeeapi.auth.api.SignedInPerson;
import com.vertyll.jakartaeeapi.auth.application.port.inbound.SessionUseCase;
import com.vertyll.jakartaeeapi.auth.application.port.inbound.TokenAuthenticationUseCase;
import com.vertyll.jakartaeeapi.auth.application.port.outbound.TokenEndpoint;
import com.vertyll.jakartaeeapi.auth.application.port.outbound.TokenVerifier;
import com.vertyll.jakartaeeapi.auth.domain.AuthException;
import com.vertyll.jakartaeeapi.auth.domain.AuthSession;
import com.vertyll.jakartaeeapi.auth.domain.Identity;
import com.vertyll.jakartaeeapi.auth.domain.TokenPair;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AuthenticationService implements SessionUseCase, TokenAuthenticationUseCase {
    private final TokenEndpoint tokens;
    private final TokenVerifier verifier;
    private final SignedInAccounts accounts;

    public AuthenticationService(TokenEndpoint tokens, TokenVerifier verifier, SignedInAccounts accounts) {
        this.tokens = tokens;
        this.verifier = verifier;
        this.accounts = accounts;
    }

    @Override
    public AuthSession signIn(String code, String codeVerifier) {
        AuthSession session = toSession(tokens.exchange(code, codeVerifier), AuthException.SIGN_IN_REJECTED);
        boolean provisioned = false;
        try {
            accounts.signedIn(person(session.identity()));
            provisioned = true;
        } finally {
            if (!provisioned) {
                tokens.revoke(session.refreshToken());
            }
        }
        log.info("User {} signed in", session.identity().keycloakId());
        return session;
    }

    @Override
    public AuthSession refresh(AuthSession session) {
        return toSession(tokens.refresh(session.refreshToken()), AuthException.SESSION_EXPIRED);
    }

    @Override
    public void signOut(AuthSession session) {
        tokens.revoke(session.refreshToken());
        log.info("User {} signed out", session.identity().keycloakId());
    }

    @Override
    public Identity authenticate(String accessToken) {
        return verifier.verify(accessToken).identity();
    }

    /** A token the provider issued but that does not verify is refused as the step that asked for it. */
    private AuthSession toSession(TokenPair pair, String onRejection) {
        TokenVerifier.VerifiedToken verified;
        try {
            verified = verifier.verify(pair.accessToken());
        } catch (AuthException e) {
            throw AuthException.rejected(onRejection, e);
        }
        return new AuthSession(verified.identity(), pair.accessToken(), pair.refreshToken(), verified.expiresAt());
    }

    private static SignedInPerson person(Identity identity) {
        return new SignedInPerson(identity.keycloakId(), identity.email(), identity.username(), identity.roles());
    }
}
