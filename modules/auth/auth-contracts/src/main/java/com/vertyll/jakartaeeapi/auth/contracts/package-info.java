/**
 * What other modules may use of authentication: the person signed in ({@link
 * com.vertyll.jakartaeeapi.auth.contracts.SignedInPerson}), found as the request's {@link
 * com.vertyll.jakartaeeapi.auth.contracts.SignedInPrincipal}, and the port a module implements to keep its record of people in
 * step with every sign-in ({@link com.vertyll.jakartaeeapi.auth.contracts.SignedInAccounts}). The rest of the module is its
 * own.
 */
@NullMarked
package com.vertyll.jakartaeeapi.auth.contracts;

import org.jspecify.annotations.NullMarked;
