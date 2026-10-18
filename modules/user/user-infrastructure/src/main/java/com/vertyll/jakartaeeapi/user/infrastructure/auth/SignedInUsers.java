package com.vertyll.jakartaeeapi.user.infrastructure.auth;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.vertyll.jakartaeeapi.auth.api.SignedInAccounts;
import com.vertyll.jakartaeeapi.auth.api.SignedInPerson;
import com.vertyll.jakartaeeapi.user.application.port.inbound.MirrorAccountUseCase;
import com.vertyll.jakartaeeapi.user.application.port.inbound.MirrorAccountUseCase.MirrorAccountCommand;

/** The user module's side of a sign-in: the port authentication calls, translated into the module's own use case. */
@Singleton
public class SignedInUsers implements SignedInAccounts {
    private final MirrorAccountUseCase accounts;

    @Inject
    public SignedInUsers(MirrorAccountUseCase accounts) {
        this.accounts = accounts;
    }

    @Override
    public void signedIn(SignedInPerson person) {
        accounts
            .mirror(new MirrorAccountCommand(person.keycloakId(), person.email(), person.username(), person.roles()));
    }
}
