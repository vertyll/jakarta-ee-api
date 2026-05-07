package com.vertyll.jakartaeeapi.common.exception;

import jakarta.ws.rs.core.Response;

/** Interface for exceptions that want to specify their own HTTP status code */
@FunctionalInterface
public interface HttpStatusProvider {
    Response.Status getHttpStatus();
}
