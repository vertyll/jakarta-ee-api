package com.vertyll.jakartaeeapi.auth.infrastructure.web;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vertyll.jakartaeeapi.auth.contracts.SignedInPerson;
import com.vertyll.jakartaeeapi.auth.contracts.SignedInPrincipal;

import static org.assertj.core.api.Assertions.assertThat;

class CallerSecurityContextTest {

    @Test
    void theCallerHasTheRolesTheirTokenCarries() {
        SignedInPrincipal principal =
                new SignedInPrincipal(new SignedInPerson("subject", "ada@example.test", "ada", List.of("USER")));

        CallerSecurityContext context = new CallerSecurityContext(principal, true);

        assertThat(context.getUserPrincipal()).isEqualTo(principal);
        assertThat(context.getUserPrincipal().getName()).isEqualTo("ada@example.test");
        assertThat(context.isUserInRole("USER")).isTrue();
        assertThat(context.isUserInRole("ADMIN")).isFalse();
        assertThat(context.isSecure()).isTrue();
        assertThat(context.getAuthenticationScheme()).isEqualTo("Bearer");
    }
}
