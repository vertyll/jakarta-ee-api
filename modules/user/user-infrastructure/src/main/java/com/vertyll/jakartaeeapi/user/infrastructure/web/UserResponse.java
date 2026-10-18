package com.vertyll.jakartaeeapi.user.infrastructure.web;

import java.util.List;

import com.vertyll.jakartaeeapi.user.domain.UserAccount;

/** The caller's account, as {@code GET /users/me} answers it. */
public record UserResponse(String keycloakId, String email, String username, List<String> roles) {
    public UserResponse {
        roles = List.copyOf(roles);
    }

    static UserResponse from(UserAccount account) {
        return new UserResponse(account.keycloakId(), account.email(), account.username(), account.roles());
    }
}
