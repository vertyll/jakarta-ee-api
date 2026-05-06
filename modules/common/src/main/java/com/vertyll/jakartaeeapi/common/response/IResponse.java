package com.vertyll.jakartaeeapi.common.response;

import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

public interface IResponse<T> {
    @Nullable T getData();

    @Nullable String getMessage();

    LocalDateTime getTimestamp();
}
