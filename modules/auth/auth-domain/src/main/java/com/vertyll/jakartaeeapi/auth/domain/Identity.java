package com.vertyll.jakartaeeapi.auth.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** Who a verified access token says its holder is: the Keycloak subject, the e-mail, the username and the realm roles. */
public record Identity(String keycloakId, String email, String username, List<String> roles) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public Identity {
        roles = List.copyOf(roles);
    }
}
