package com.vertyll.jakartaeebaseapi.common.response;

import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

public interface IResponse<T> {
    @Nullable T getData();

    @Nullable String getMessage();

    LocalDateTime getTimestamp();
}
