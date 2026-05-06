package com.vertyll.jakartaeeapi.common.exception;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Interface for exceptions that contain validation errors
 */
@FunctionalInterface
public interface ValidationErrorProvider {
    @Nullable Map<String, String> getValidationErrors();
}
