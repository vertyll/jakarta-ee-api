package com.vertyll.jakartaeeapi.auth.contracts;

import java.util.List;

/**
 * A person signed in through Keycloak, as other modules see them: the subject Keycloak gives the account, the e-mail
 * address, the username and the realm roles their token carries.
 */
public record SignedInPerson(String keycloakId, String email, String username, List<String> roles) {
    public SignedInPerson {
        roles = List.copyOf(roles);
    }
}
