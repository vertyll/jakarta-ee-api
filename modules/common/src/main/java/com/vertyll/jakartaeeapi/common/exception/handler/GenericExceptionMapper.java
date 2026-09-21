package com.vertyll.jakartaeeapi.common.exception.handler;

import java.util.Objects;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.vertyll.jakartaeeapi.common.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Exception exception) {
        String path = uriInfo.getPath();

        // Handle JAX-RS WebApplicationException separately
        if (exception instanceof WebApplicationException webEx) {
            Response.StatusType status = webEx.getResponse().getStatusInfo();
            String message = Objects.requireNonNullElse(webEx.getMessage(), status.getReasonPhrase());
            log.warn("WebApplicationException: {} at path: {}", message, path);
            return ApiResponse.buildResponse(null, message, status, null, path);
        }

        // Log unexpected errors
        log.error("Unexpected error occurred at path: {}", path, exception);

        return ApiResponse.buildResponse(
            null,
            "An unexpected error occurred. Please try again later.",
            Response.Status.INTERNAL_SERVER_ERROR,
            null,
            path
        );
    }
}
