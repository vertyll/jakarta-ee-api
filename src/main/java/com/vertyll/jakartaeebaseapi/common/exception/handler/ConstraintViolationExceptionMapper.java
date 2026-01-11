package com.vertyll.jakartaeebaseapi.common.exception.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

import com.vertyll.jakartaeebaseapi.common.response.ApiResponse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Slf4j
@Provider
public class ConstraintViolationExceptionMapper
        implements ExceptionMapper<ConstraintViolationException> {

    @Context private UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String path = uriInfo != null ? uriInfo.getPath() : null;

        Map<String, String> validationErrors = new ConcurrentHashMap<>();

        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            String fieldName = getFieldName(violation);
            String message = violation.getMessage();
            validationErrors.put(fieldName, message);
        }

        log.warn("Validation failed at path: {} with {} violations", path, validationErrors.size());

        return ApiResponse.buildResponse(
                null, "Validation failed", Response.Status.BAD_REQUEST, validationErrors, path);
    }

    private String getFieldName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();

        // Extract field name from path like "createStation.stationDto.name"
        int lastDot = propertyPath.lastIndexOf('.');
        if (lastDot != -1) {
            return propertyPath.substring(lastDot + 1);
        }

        return propertyPath;
    }
}
