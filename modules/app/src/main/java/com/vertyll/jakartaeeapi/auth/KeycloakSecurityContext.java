package com.vertyll.jakartaeeapi.auth;

import java.security.Principal;

import jakarta.ws.rs.core.SecurityContext;

public record KeycloakSecurityContext(KeycloakPrincipal principal, boolean secure) implements SecurityContext {
    private static final String BEARER = "Bearer";

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return principal.identity().roles().contains(role);
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        return BEARER;
    }
}
