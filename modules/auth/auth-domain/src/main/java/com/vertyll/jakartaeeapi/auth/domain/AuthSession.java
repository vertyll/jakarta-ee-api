package com.vertyll.jakartaeeapi.auth.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

/** A signed-in session: who it is, and the tokens Keycloak issued for it. Its tokens are never shown. */
public record AuthSession(Identity identity, String accessToken, String refreshToken, Instant accessTokenExpiresAt)
    implements
    Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public boolean needsRefreshAt(Instant now, Duration skew) {
        return !now.plus(skew).isBefore(accessTokenExpiresAt);
    }

    @Override
    public String toString() {
        return "AuthSession[identity=" + identity + ", accessToken=***, refreshToken=***, accessTokenExpiresAt="
                + accessTokenExpiresAt + "]";
    }
}
