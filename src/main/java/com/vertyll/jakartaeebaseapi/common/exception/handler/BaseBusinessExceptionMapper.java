package com.vertyll.jakartaeebaseapi.common.exception.handler;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import com.vertyll.jakartaeebaseapi.common.exception.BaseBusinessException;
import com.vertyll.jakartaeebaseapi.common.exception.HttpStatusProvider;
import com.vertyll.jakartaeebaseapi.common.exception.ValidationErrorProvider;
import com.vertyll.jakartaeebaseapi.common.response.ApiResponse;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Slf4j
@Provider
public class BaseBusinessExceptionMapper implements ExceptionMapper<BaseBusinessException> {

    @Context private UriInfo uriInfo;

    @Override
    public Response toResponse(BaseBusinessException exception) {
        String path = uriInfo != null ? uriInfo.getPath() : null;

        // Determine HTTP status
        Response.Status status = Response.Status.BAD_REQUEST; // default
        if (exception instanceof HttpStatusProvider statusProvider) {
            status = statusProvider.getHttpStatus();
        }

        // Get validation errors if present
        Map<String, String> validationErrors = null;
        if (exception instanceof ValidationErrorProvider validationProvider) {
            validationErrors = validationProvider.getValidationErrors();
        }

        // Log the exception
        if (status.getFamily() == Response.Status.Family.SERVER_ERROR) {
            log.error("Server error occurred: {}", exception.getMessageKey(), exception);
        } else {
            log.warn(
                    "Business exception occurred: {} at path: {}", exception.getMessageKey(), path);
        }

        return ApiResponse.buildResponse(
                null, exception.getMessageKey(), status, validationErrors, path);
    }
}
