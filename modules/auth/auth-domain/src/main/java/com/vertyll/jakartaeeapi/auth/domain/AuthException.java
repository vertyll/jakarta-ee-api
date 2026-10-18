package com.vertyll.jakartaeeapi.auth.domain;

import java.io.Serial;

import com.vertyll.jakartaeeapi.common.exception.BaseBusinessException;

/**
 * Why a sign-in, a refresh or a token was refused, by the message key a client renders: the person's credentials or
 * session were refused, or Keycloak could not be reached. How each is answered over HTTP is the web adapter's concern.
 */
public final class AuthException extends BaseBusinessException {
    public static final String SIGN_IN_REJECTED = "errors.auth.signInRejected";
    public static final String SESSION_EXPIRED = "errors.auth.sessionExpired";
    public static final String TOKEN_INVALID = "errors.auth.tokenInvalid";
    public static final String IDENTITY_PROVIDER_UNAVAILABLE = "errors.auth.identityProviderUnavailable";

    @Serial
    private static final long serialVersionUID = 1L;

    private AuthException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }

    private AuthException(String messageKey) {
        super(messageKey);
    }

    public static AuthException rejected(String messageKey, Throwable cause) {
        return new AuthException(messageKey, cause);
    }

    public static AuthException rejected(String messageKey) {
        return new AuthException(messageKey);
    }

    public static AuthException unavailable(Throwable cause) {
        return new AuthException(IDENTITY_PROVIDER_UNAVAILABLE, cause);
    }

    public static AuthException unavailable() {
        return new AuthException(IDENTITY_PROVIDER_UNAVAILABLE);
    }

    /** The same refusal, raised again for a caller who waited on another's refresh. */
    public static AuthException copyOf(AuthException failure) {
        return new AuthException(failure.getMessageKey(), failure);
    }

    /** Whether Keycloak could not be reached, rather than having refused the person. */
    public boolean isUnavailable() {
        return IDENTITY_PROVIDER_UNAVAILABLE.equals(getMessageKey());
    }
}
