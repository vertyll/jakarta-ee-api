package com.vertyll.jakartaeeapi.user;

import java.util.List;

public record UserAccount(String keycloakId, String email, String username, List<String> roles) {
    public UserAccount {
        roles = List.copyOf(roles);
    }
}
