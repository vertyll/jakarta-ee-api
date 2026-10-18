package com.vertyll.jakartaeeapi.user.domain;

import java.util.List;

/**
 * A person's account as this application keeps it, mirrored from Keycloak at every sign-in: Keycloak owns the
 * credentials, and the subject it gives the account joins the two.
 */
public record UserAccount(String keycloakId, String email, String username, List<String> roles) {
    public UserAccount {
        roles = List.copyOf(roles);
    }
}
