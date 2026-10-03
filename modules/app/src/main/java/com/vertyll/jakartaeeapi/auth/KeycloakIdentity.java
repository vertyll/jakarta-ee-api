package com.vertyll.jakartaeeapi.auth;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record KeycloakIdentity(String keycloakId, String email, String firstName, String lastName, List<String> roles)
    implements
    Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public KeycloakIdentity {
        roles = List.copyOf(roles);
    }
}
