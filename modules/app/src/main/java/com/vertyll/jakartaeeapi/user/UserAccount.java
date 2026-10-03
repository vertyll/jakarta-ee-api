package com.vertyll.jakartaeeapi.user;

import java.util.List;

public record UserAccount(String keycloakId, String email, String firstName, String lastName, List<String> roles) {
    public UserAccount {
        roles = List.copyOf(roles);
    }
}
