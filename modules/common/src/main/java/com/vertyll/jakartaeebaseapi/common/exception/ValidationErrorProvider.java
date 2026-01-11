package com.vertyll.jakartaeebaseapi.common.exception;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/** Interface for exceptions that contain validation errors */
@FunctionalInterface
public interface ValidationErrorProvider {
    @Nullable Map<String, String> getValidationErrors();
}
