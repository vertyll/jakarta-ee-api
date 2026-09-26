package com.vertyll.jakartaeeapi.common.exception;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface ValidationErrorProvider {
    Map<String, List<String>> getValidationErrors();
}
