package com.vertyll.jakartaeeapi.user;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.vertyll.jakartaeeapi.auth.KeycloakIdentity;

@Singleton
public class UserService {
    private final UserRepository repository;

    @Inject
    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public UserAccount sync(KeycloakIdentity identity) {
        return repository.save(
            new UserAccount(
                identity.keycloakId(),
                identity.email(),
                identity.firstName(),
                identity.lastName(),
                identity.roles()
            )
        );
    }
}
