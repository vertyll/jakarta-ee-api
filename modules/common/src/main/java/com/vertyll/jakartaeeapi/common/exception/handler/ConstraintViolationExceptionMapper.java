package com.vertyll.jakartaeeapi.common.exception.handler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.vertyll.jakartaeeapi.common.problem.Problems;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final String VALIDATION_FAILED = "errors.validation.failed";

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            errors.computeIfAbsent(fieldName(violation), _ -> new ArrayList<>()).add(messageKey(violation));
        }
        String path = uriInfo.getPath();
        log.warn("Validation failed at path {} with {} invalid fields", path, errors.size());
        return Problems.response(
            Problems.of(Response.Status.BAD_REQUEST, VALIDATION_FAILED, path)
                .withCode(VALIDATION_FAILED, List.of())
                .withErrors(errors)
        );
    }

    private static String messageKey(ConstraintViolation<?> violation) {
        String template = violation.getMessageTemplate();
        return template.startsWith("{") && template.endsWith("}") ? template.substring(1, template.length() - 1)
                : template;
    }

    private static String fieldName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot == -1 ? propertyPath : propertyPath.substring(lastDot + 1);
    }
}
