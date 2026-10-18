package com.vertyll.jakartaeeapi.user.infrastructure.web;

import java.util.List;

import jakarta.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.Test;

import com.vertyll.jakartaeeapi.auth.contracts.SignedInPerson;
import com.vertyll.jakartaeeapi.auth.contracts.SignedInPrincipal;
import com.vertyll.jakartaeeapi.user.application.port.inbound.MirrorAccountUseCase;
import com.vertyll.jakartaeeapi.user.application.port.inbound.MirrorAccountUseCase.MirrorAccountCommand;
import com.vertyll.jakartaeeapi.user.domain.UserAccount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserResourceTest {
    private final MirrorAccountUseCase accounts = mock(MirrorAccountUseCase.class);
    private final UserResource resource = new UserResource(accounts);
    private final SecurityContext security = mock(SecurityContext.class);

    @Test
    void theCallerGetsTheirAccountAsJustMirrored() {
        SignedInPerson person = new SignedInPerson("subject", "ada@example.test", "ada", List.of("USER"));
        when(security.getUserPrincipal()).thenReturn(new SignedInPrincipal(person));
        when(accounts.mirror(new MirrorAccountCommand("subject", "ada@example.test", "ada", List.of("USER"))))
            .thenReturn(new UserAccount("subject", "ada@example.test", "ada", List.of("USER")));

        assertThat(resource.me(security))
            .isEqualTo(new UserResponse("subject", "ada@example.test", "ada", List.of("USER")));
    }

    @Test
    void aCallerWithoutASignedInPrincipalIsABrokenInvariant() {
        assertThatThrownBy(() -> resource.me(security)).isInstanceOf(IllegalStateException.class);
    }
}
