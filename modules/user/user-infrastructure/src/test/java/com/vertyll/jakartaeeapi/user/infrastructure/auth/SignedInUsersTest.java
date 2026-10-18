package com.vertyll.jakartaeeapi.user.infrastructure.auth;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vertyll.jakartaeeapi.auth.api.SignedInPerson;
import com.vertyll.jakartaeeapi.user.application.port.inbound.MirrorAccountUseCase;
import com.vertyll.jakartaeeapi.user.application.port.inbound.MirrorAccountUseCase.MirrorAccountCommand;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SignedInUsersTest {

    @Test
    void aSignInMirrorsThePersonsAccount() {
        MirrorAccountUseCase accounts = mock(MirrorAccountUseCase.class);

        new SignedInUsers(accounts)
            .signedIn(new SignedInPerson("subject", "ada@jakarta-ee-api.local", "ada", List.of("USER")));

        verify(accounts)
            .mirror(new MirrorAccountCommand("subject", "ada@jakarta-ee-api.local", "ada", List.of("USER")));
    }
}
