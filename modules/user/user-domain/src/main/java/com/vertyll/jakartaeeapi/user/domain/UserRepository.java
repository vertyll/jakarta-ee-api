package com.vertyll.jakartaeeapi.user.domain;

import java.util.Optional;

/** Where accounts are kept, one per Keycloak subject. */
public interface UserRepository {

    /** Stores the account, replacing the one with the same Keycloak subject. */
    UserAccount save(UserAccount account);

    Optional<UserAccount> findByKeycloakId(String keycloakId);
}
