package com.vertyll.jakartaeeapi.common.response;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import jakarta.ws.rs.core.Response;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ApiResponse<T> extends BaseResponse<T> {

    /**
     * Builds response with data and message
     *
     * @param data Response payload
     * @param message Response message
     * @param status HTTP status
     * @return Response with ApiResponse entity
     */
    public static <T> Response buildResponse(
            @Nullable T data, String message, Response.Status status) {
        ApiResponse<T> response =
                ApiResponse.<T>builder()
                        .data(data)
                        .message(message)
                        .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                        .build();
        return Response.status(status).entity(response).build();
    }

    /**
     * Builds response with data, message and path
     *
     * @param data Response payload
     * @param message Response message
     * @param status HTTP status
     * @param path Request path
     * @return Response with ApiResponse entity
     */
    public static <T> Response buildResponse(
            @Nullable T data, String message, Response.Status status, @Nullable String path) {
        ApiResponse<T> response =
                ApiResponse.<T>builder()
                        .data(data)
                        .message(message)
                        .path(path)
                        .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                        .build();
        return Response.status(status).entity(response).build();
    }

    /**
     * Builds response with validation errors
     *
     * @param data Response payload (usually null for validation errors)
     * @param message Response message
     * @param status HTTP status
     * @param validationErrors Map of field errors
     * @return Response with ApiResponse entity
     */
    public static <T> Response buildResponse(
            @Nullable T data,
            String message,
            Response.Status status,
            @Nullable Map<String, String> validationErrors) {
        ApiResponse<T> response =
                ApiResponse.<T>builder()
                        .data(data)
                        .message(message)
                        .validationErrors(validationErrors)
                        .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                        .build();
        return Response.status(status).entity(response).build();
    }

    /**
     * Builds response with all parameters
     *
     * @param data Response payload
     * @param message Response message
     * @param status HTTP status
     * @param validationErrors Map of field errors
     * @param path Request path
     * @return Response with ApiResponse entity
     */
    public static <T> Response buildResponse(
            @Nullable T data,
            String message,
            Response.Status status,
            @Nullable Map<String, String> validationErrors,
            @Nullable String path) {
        ApiResponse<T> response =
                ApiResponse.<T>builder()
                        .data(data)
                        .message(message)
                        .validationErrors(validationErrors)
                        .path(path)
                        .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                        .build();
        return Response.status(status).entity(response).build();
    }
}
