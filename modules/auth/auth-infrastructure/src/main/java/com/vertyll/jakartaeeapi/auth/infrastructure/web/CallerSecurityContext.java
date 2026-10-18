package com.vertyll.jakartaeeapi.auth.infrastructure.web;

import java.security.Principal;

import jakarta.ws.rs.core.SecurityContext;

import com.vertyll.jakartaeeapi.auth.contracts.SignedInPrincipal;

/** The security context of a request authorized by a token: its principal, and the realm roles its token carries. */
public record CallerSecurityContext(SignedInPrincipal principal, boolean secure) implements SecurityContext {
    private static final String BEARER = "Bearer";

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return principal.person().roles().contains(role);
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
