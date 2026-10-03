package com.vertyll.jakartaeeapi.user;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import com.vertyll.jakartaeeapi.auth.KeycloakPrincipal;

@Path("/users")
public class UserResource {
    @Inject
    private UserService users;

    @Context
    private SecurityContext security;

    @GET
    @Path("/me")
    @RolesAllowed(
        {
            "USER",
            "ADMIN"
        }
    )
    @Produces(MediaType.APPLICATION_JSON)
    public UserAccount me() {
        if (!(security.getUserPrincipal() instanceof KeycloakPrincipal principal)) {
            throw new IllegalStateException("A role check passed without a Keycloak principal");
        }
        return users.sync(principal.identity());
    }
}
