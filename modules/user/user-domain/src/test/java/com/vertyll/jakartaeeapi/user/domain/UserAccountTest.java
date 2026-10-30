package com.vertyll.jakartaeeapi.user.domain;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAccountTest {
    @Test
    void anAccountKeepsTheRolesItWasGiven() {
        List<String> roles = new ArrayList<>(List.of("USER"));
        UserAccount account = new UserAccount("subject", "ada@example.test", "ada", roles);

        roles.add("ADMIN");

        assertThat(account.roles()).containsExactly("USER");
    }

    @Test
    void theRolesOfAnAccountCannotBeChanged() {
        List<String> roles = new UserAccount("subject", "ada@example.test", "ada", List.of("USER")).roles();

        assertThatThrownBy(() -> roles.add("ADMIN")).isInstanceOf(UnsupportedOperationException.class);
    }
}
