package com.vertyll.jakartaeeapi.user;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.vertyll.jakartaeeapi.auth.KeycloakIdentity;
import com.vertyll.jakartaeeapi.auth.SignedInAccounts;

@Singleton
public class SignedInUsers implements SignedInAccounts {
    private final UserService users;

    @Inject
    public SignedInUsers(UserService users) {
        this.users = users;
    }

    @Override
    public void signedIn(KeycloakIdentity identity) {
        users.sync(identity);
    }
}
