package com.vertyll.jakartaeeapi.auth.application.port.outbound;

import com.vertyll.jakartaeeapi.auth.domain.TokenPair;

/** The identity provider's token endpoint (RFC 6749, 3.2), as the sign-in needs it. */
public interface TokenEndpoint {

    /**
     * Redeems an authorization code with its PKCE verifier.
     *
     * @throws com.vertyll.jakartaeeapi.auth.domain.AuthException {@code SIGN_IN_REJECTED} when the provider refuses
     *     it, {@code IDENTITY_PROVIDER_UNAVAILABLE} when it cannot be reached
     */
    TokenPair exchange(String code, String codeVerifier);

    /**
     * Rotates a refresh token; concurrent calls with the same token share one request.
     *
     * @throws com.vertyll.jakartaeeapi.auth.domain.AuthException {@code SESSION_EXPIRED} when the provider refuses
     *     it, {@code IDENTITY_PROVIDER_UNAVAILABLE} when it cannot be reached
     */
    TokenPair refresh(String refreshToken);

    /** Ends the provider's session behind a refresh token, best effort. */
    void revoke(String refreshToken);
}
