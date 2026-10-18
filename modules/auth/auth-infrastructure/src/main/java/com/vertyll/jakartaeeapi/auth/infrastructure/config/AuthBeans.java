package com.vertyll.jakartaeeapi.auth.infrastructure.config;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import com.vertyll.jakartaeeapi.auth.application.port.outbound.TokenEndpoint;
import com.vertyll.jakartaeeapi.auth.application.port.outbound.TokenVerifier;
import com.vertyll.jakartaeeapi.auth.application.service.AuthenticationService;
import com.vertyll.jakartaeeapi.auth.contracts.SignedInAccounts;

/**
 * Wires authentication's application to its adapters: the service runs every use case, and the container gives it the
 * token endpoint, the verifier and the module that keeps the accounts.
 */
@Singleton
public class AuthBeans {

    @Produces
    @Singleton
    public AuthenticationService authenticationService(
        TokenEndpoint tokens,
        TokenVerifier verifier,
        SignedInAccounts accounts
    ) {
        return new AuthenticationService(tokens, verifier, accounts);
    }
}
