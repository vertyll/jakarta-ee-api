package com.vertyll.jakartaeeapi.user.infrastructure.web;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import com.vertyll.jakartaeeapi.auth.contracts.SignedInPerson;
import com.vertyll.jakartaeeapi.auth.contracts.SignedInPrincipal;
import com.vertyll.jakartaeeapi.user.application.port.inbound.MirrorAccountUseCase;
import com.vertyll.jakartaeeapi.user.application.port.inbound.MirrorAccountUseCase.MirrorAccountCommand;

@Path("/users")
public class UserResource {
    private final MirrorAccountUseCase accounts;

    @Inject
    public UserResource(MirrorAccountUseCase accounts) {
        this.accounts = accounts;
    }

    @GET
    @Path("/me")
    @RolesAllowed(
        {
            "USER",
            "ADMIN"
        }
    )
    @Produces(MediaType.APPLICATION_JSON)
    public UserResponse me(@Context SecurityContext security) {
        if (!(security.getUserPrincipal() instanceof SignedInPrincipal(SignedInPerson person))) {
            throw new IllegalStateException("A role check passed without a signed-in principal");
        }
        return UserResponse.from(
            accounts.mirror(
                new MirrorAccountCommand(person.keycloakId(), person.email(), person.username(), person.roles())
            )
        );
    }
}
