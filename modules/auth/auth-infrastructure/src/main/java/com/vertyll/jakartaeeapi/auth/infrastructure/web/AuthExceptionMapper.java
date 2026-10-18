package com.vertyll.jakartaeeapi.auth.infrastructure.web;

import java.util.Arrays;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.vertyll.jakartaeeapi.auth.domain.AuthException;
import com.vertyll.jakartaeeapi.common.problem.Problems;

import lombok.extern.slf4j.Slf4j;

/**
 * Answers a refused sign-in, session or token as an RFC 9457 problem: {@code 401} when the person was refused, {@code
 * 503} when Keycloak could not be reached.
 */
@Slf4j
@Provider
public class AuthExceptionMapper implements ExceptionMapper<AuthException> {
    @Context
    private UriInfo uriInfo;

    // The container injects the request's @Context field after construction.
    @SuppressWarnings("java:S2637")
    public AuthExceptionMapper() {
        // The container injects the request's URI.
    }

    AuthExceptionMapper(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    @Override
    public Response toResponse(AuthException exception) {
        String path = uriInfo.getPath();
        Response.Status status =
                exception.isUnavailable() ? Response.Status.SERVICE_UNAVAILABLE : Response.Status.UNAUTHORIZED;
        if (exception.isUnavailable()) {
            log.error("Keycloak unavailable at path {}", path, exception);
        } else {
            log.warn("Authentication refused {} at path {}", exception.getMessageKey(), path);
        }
        return Problems.response(
            Problems.of(status, exception.getMessageKey(), path)
                .withCode(exception.getMessageKey(), Arrays.asList(exception.getArgs()))
        );
    }
}
