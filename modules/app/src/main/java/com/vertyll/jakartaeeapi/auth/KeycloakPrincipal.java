package com.vertyll.jakartaeeapi.auth;

import java.security.Principal;

public record KeycloakPrincipal(KeycloakIdentity identity) implements Principal {
    @Override
    public String getName() {
        return identity.email();
    }
}
