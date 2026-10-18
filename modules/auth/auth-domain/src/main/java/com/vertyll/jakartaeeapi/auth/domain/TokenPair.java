package com.vertyll.jakartaeeapi.auth.domain;

/** An access token and the refresh token issued with it. Neither is ever shown. */
public record TokenPair(String accessToken, String refreshToken) {
    @Override
    public String toString() {
        return "TokenPair[accessToken=***, refreshToken=***]";
    }
}
