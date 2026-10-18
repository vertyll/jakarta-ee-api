package com.vertyll.jakartaeeapi.auth.contracts;

/**
 * The application's own record of the people who sign in, which a sign-in brings in step with the verified token
 * before it completes. Authentication owns this contract; the module that keeps the accounts implements it, so the
 * dependency runs from accounts to authentication and never back.
 */
@FunctionalInterface
public interface SignedInAccounts {

    /**
     * Creates or updates the account of the person who signed in.
     *
     * @throws RuntimeException when the account cannot be stored, which ends the sign-in
     */
    void signedIn(SignedInPerson person);
}
