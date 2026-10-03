package com.vertyll.jakartaeeapi.auth;

import java.io.Serial;

import jakarta.ws.rs.core.Response;

import com.vertyll.jakartaeeapi.common.exception.BaseBusinessException;
import com.vertyll.jakartaeeapi.common.exception.HttpStatusProvider;

public final class AuthException extends BaseBusinessException implements HttpStatusProvider {
    public static final String SIGN_IN_REJECTED = "errors.auth.signInRejected";
    public static final String SESSION_EXPIRED = "errors.auth.sessionExpired";
    public static final String TOKEN_INVALID = "errors.auth.tokenInvalid";
    public static final String IDENTITY_PROVIDER_UNAVAILABLE = "errors.auth.identityProviderUnavailable";

    @Serial
    private static final long serialVersionUID = 1L;

    private final Response.Status status;

    private AuthException(String messageKey, Response.Status status, Throwable cause) {
        super(messageKey, cause);
        this.status = status;
    }

    private AuthException(String messageKey, Response.Status status) {
        super(messageKey);
        this.status = status;
    }

    public static AuthException rejected(String messageKey, Throwable cause) {
        return new AuthException(messageKey, Response.Status.UNAUTHORIZED, cause);
    }

    public static AuthException rejected(String messageKey) {
        return new AuthException(messageKey, Response.Status.UNAUTHORIZED);
    }

    public static AuthException unavailable(Throwable cause) {
        return new AuthException(IDENTITY_PROVIDER_UNAVAILABLE, Response.Status.SERVICE_UNAVAILABLE, cause);
    }

    public static AuthException unavailable() {
        return new AuthException(IDENTITY_PROVIDER_UNAVAILABLE, Response.Status.SERVICE_UNAVAILABLE);
    }

    public static AuthException copyOf(AuthException failure) {
        return new AuthException(failure.getMessageKey(), failure.status, failure);
    }

    @Override
    public Response.Status getHttpStatus() {
        return status;
    }
}
