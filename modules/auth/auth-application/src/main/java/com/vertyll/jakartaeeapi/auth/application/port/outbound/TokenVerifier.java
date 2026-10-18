package com.vertyll.jakartaeeapi.auth.application.port.outbound;

import java.time.Instant;

import com.vertyll.jakartaeeapi.auth.domain.Identity;

/** The verification of an access token against the identity provider's keys, issuer and audience. */
@FunctionalInterface
public interface TokenVerifier {

    /**
     * Who the token says its holder is, and when it expires.
     *
     * @throws com.vertyll.jakartaeeapi.auth.domain.AuthException {@code TOKEN_INVALID} when it does not verify
     */
    VerifiedToken verify(String accessToken);

    record VerifiedToken(Identity identity, Instant expiresAt) {
    }
}
