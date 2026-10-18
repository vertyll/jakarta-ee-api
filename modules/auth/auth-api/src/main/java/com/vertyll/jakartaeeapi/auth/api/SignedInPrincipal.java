package com.vertyll.jakartaeeapi.auth.api;

import java.security.Principal;

/**
 * The principal of a request authorized by a token: a resource reads the person from its {@code SecurityContext}.
 */
public record SignedInPrincipal(SignedInPerson person) implements Principal {
    @Override
    public String getName() {
        return person.email();
    }
}
