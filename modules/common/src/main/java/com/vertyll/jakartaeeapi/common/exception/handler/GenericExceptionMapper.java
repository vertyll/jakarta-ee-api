package com.vertyll.jakartaeeapi.common.exception.handler;

import java.util.List;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.vertyll.jakartaeeapi.common.problem.Problems;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    private static final String UNEXPECTED_ERROR = "errors.unexpected";
    private static final String STATUS_PREFIX = "errors.status.";

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Exception exception) {
        String path = uriInfo.getPath();
        if (exception instanceof WebApplicationException webException) {
            Response.StatusType status = webException.getResponse().getStatusInfo();
            String key = STATUS_PREFIX + status.getStatusCode();
            log.warn("Request to {} failed with {}", path, status.getStatusCode());
            return Problems.response(Problems.of(status, key, path).withCode(key, List.of()));
        }
        log.error("Unexpected error at path {}", path, exception);
        return Problems.response(
            Problems.of(Response.Status.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR, path)
                .withCode(UNEXPECTED_ERROR, List.of())
        );
    }
}
