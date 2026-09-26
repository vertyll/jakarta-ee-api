package com.vertyll.jakartaeeapi.common.exception;

import jakarta.ws.rs.core.Response;

@FunctionalInterface
public interface HttpStatusProvider {
    Response.Status getHttpStatus();
}
