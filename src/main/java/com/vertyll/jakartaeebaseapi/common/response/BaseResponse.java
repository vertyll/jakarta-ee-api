package com.vertyll.jakartaeebaseapi.common.response;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public abstract class BaseResponse<T> implements IResponse<T> {
    protected @Nullable T data;
    @Nullable protected String message;
    @Builder.Default protected LocalDateTime timestamp = LocalDateTime.now(ZoneOffset.UTC);
    @Nullable protected String path;
    @Nullable protected Map<String, String> validationErrors;
}
