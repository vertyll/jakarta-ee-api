package com.vertyll.jakartaeeapi.user.application.port.inbound;

import java.util.List;

import com.vertyll.jakartaeeapi.user.domain.UserAccount;

/** Brings a person's account in step with what their verified token says. */
@FunctionalInterface
public interface MirrorAccountUseCase {

    /** Creates or updates the account of the person the command describes. */
    UserAccount mirror(MirrorAccountCommand command);

    record MirrorAccountCommand(String keycloakId, String email, String username, List<String> roles) {
        public MirrorAccountCommand {
            roles = List.copyOf(roles);
        }
    }
}
