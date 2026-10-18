package com.vertyll.jakartaeeapi.user.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.vertyll.jakartaeeapi.user.application.port.inbound.MirrorAccountUseCase.MirrorAccountCommand;
import com.vertyll.jakartaeeapi.user.domain.UserAccount;
import com.vertyll.jakartaeeapi.user.domain.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountServiceTest {
    private final InMemoryUsers users = new InMemoryUsers();
    private final UserAccountService service = new UserAccountService(users);

    @Test
    void aFirstSignInCreatesTheAccount() {
        UserAccount account =
                service.mirror(new MirrorAccountCommand("subject", "ada@jakarta-ee-api.local", "ada", List.of("USER")));

        assertThat(users.findByKeycloakId("subject")).contains(account);
    }

    @Test
    void aLaterSignInBringsTheAccountInStepWithTheToken() {
        service.mirror(new MirrorAccountCommand("subject", "ada@jakarta-ee-api.local", "ada", List.of("USER")));

        service.mirror(new MirrorAccountCommand("subject", "ada@example.test", "ada", List.of("USER", "ADMIN")));

        assertThat(users.findByKeycloakId("subject"))
            .contains(new UserAccount("subject", "ada@example.test", "ada", List.of("USER", "ADMIN")));
    }

    private static final class InMemoryUsers implements UserRepository {
        private final Map<String, UserAccount> accounts = new HashMap<>();

        @Override
        public UserAccount save(UserAccount account) {
            accounts.put(account.keycloakId(), account);
            return account;
        }

        @Override
        public Optional<UserAccount> findByKeycloakId(String keycloakId) {
            return Optional.ofNullable(accounts.get(keycloakId));
        }
    }
}
