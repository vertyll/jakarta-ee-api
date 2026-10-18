package com.vertyll.jakartaeeapi.auth.application.port.inbound;

import com.vertyll.jakartaeeapi.auth.domain.AuthSession;

/** A browser's session: signing in with the code Keycloak returned, keeping its tokens fresh and signing out. */
public interface SessionUseCase {

    /**
     * Redeems the authorization code, verifies the access token and brings the person's account in step.
     *
     * @throws com.vertyll.jakartaeeapi.auth.domain.AuthException {@code SIGN_IN_REJECTED} when Keycloak refuses the
     *     code or issues a token that does not verify, {@code IDENTITY_PROVIDER_UNAVAILABLE} when it cannot be reached
     */
    AuthSession signIn(String code, String codeVerifier);

    /**
     * Rotates the session's tokens.
     *
     * @throws com.vertyll.jakartaeeapi.auth.domain.AuthException {@code SESSION_EXPIRED} when Keycloak refuses the
     *     refresh token, {@code IDENTITY_PROVIDER_UNAVAILABLE} when it cannot be reached
     */
    AuthSession refresh(AuthSession session);

    /** Ends the Keycloak session behind this one, best effort. */
    void signOut(AuthSession session);
}
