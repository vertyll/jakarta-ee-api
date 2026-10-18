package com.vertyll.jakartaeeapi.auth.application.port.inbound;

import com.vertyll.jakartaeeapi.auth.domain.Identity;

/** A request authorized by an access token, from the {@code Authorization} header or the browser's session. */
@FunctionalInterface
public interface TokenAuthenticationUseCase {

    /**
     * Who the token's holder is.
     *
     * @throws com.vertyll.jakartaeeapi.auth.domain.AuthException {@code TOKEN_INVALID} when the token does not verify
     */
    Identity authenticate(String accessToken);
}
