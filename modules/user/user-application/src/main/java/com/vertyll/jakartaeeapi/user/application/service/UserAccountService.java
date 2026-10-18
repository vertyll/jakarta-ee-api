package com.vertyll.jakartaeeapi.user.application.service;

import com.vertyll.jakartaeeapi.user.application.port.inbound.MirrorAccountUseCase;
import com.vertyll.jakartaeeapi.user.domain.UserAccount;
import com.vertyll.jakartaeeapi.user.domain.UserRepository;

public final class UserAccountService implements MirrorAccountUseCase {
    private final UserRepository repository;

    public UserAccountService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserAccount mirror(MirrorAccountCommand command) {
        return repository
            .save(new UserAccount(command.keycloakId(), command.email(), command.username(), command.roles()));
    }
}
