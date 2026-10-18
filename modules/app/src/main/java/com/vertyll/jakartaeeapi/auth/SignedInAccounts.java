package com.vertyll.jakartaeeapi.auth;

/**
 * The application's own record of the people who sign in, which a sign-in brings in step with the verified token
 * before it completes. The sign-in owns this contract; the package that keeps the accounts implements it, so the
 * dependency runs from accounts to the sign-in and never back.
 */
@FunctionalInterface
public interface SignedInAccounts {

    /**
     * Creates or updates the account of the person the identity describes.
     *
     * @throws RuntimeException when the account cannot be stored, which ends the sign-in
     */
    void signedIn(KeycloakIdentity identity);
}
