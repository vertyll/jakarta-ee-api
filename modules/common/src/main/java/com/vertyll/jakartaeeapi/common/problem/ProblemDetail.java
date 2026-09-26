package com.vertyll.jakartaeeapi.common.problem;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

public record ProblemDetail(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    @Nullable String code,
    @Nullable List<Object> args,
    @Nullable Map<String, List<String>> errors
) {

    public static final String MEDIA_TYPE = "application/problem+json";

    private static final String DEFAULT_TYPE = "about:blank";

    public ProblemDetail {
        args = args == null ? null : List.copyOf(args);
        errors = errors == null ? null : Map.copyOf(errors);
    }

    public static ProblemDetail of(int status, String title, String detail, String instance) {
        return new ProblemDetail(DEFAULT_TYPE, title, status, detail, instance, null, null, null);
    }

    public ProblemDetail withCode(String newCode, List<Object> newArgs) {
        return new ProblemDetail(
            type,
            title,
            status,
            detail,
            instance,
            newCode,
            newArgs.isEmpty() ? null : newArgs,
            errors
        );
    }

    public ProblemDetail withErrors(Map<String, List<String>> newErrors) {
        return new ProblemDetail(type, title, status, detail, instance, code, args, newErrors);
    }
}
