package com.vertyll.jakartaeeapi.auth;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class SessionService {
    private final KeycloakTokenClient tokens;
    private final SignedInAccounts accounts;

    @Inject
    public SessionService(KeycloakTokenClient tokens, SignedInAccounts accounts) {
        this.tokens = tokens;
        this.accounts = accounts;
    }

    public AuthSession signIn(String code, String codeVerifier) {
        AuthSession session = tokens.exchange(code, codeVerifier);
        boolean provisioned = false;
        try {
            accounts.signedIn(session.identity());
            provisioned = true;
        } finally {
            if (!provisioned) {
                tokens.revoke(session.refreshToken());
            }
        }
        log.info("User {} signed in", session.identity().keycloakId());
        return session;
    }

    public AuthSession refresh(AuthSession session) {
        return tokens.refresh(session.refreshToken());
    }

    public void signOut(AuthSession session) {
        tokens.revoke(session.refreshToken());
        log.info("User {} signed out", session.identity().keycloakId());
    }
}
