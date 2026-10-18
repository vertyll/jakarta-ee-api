package com.vertyll.jakartaeeapi.auth.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthExceptionTest {

    @Test
    void aRefusalNamesWhatWasRefusedAndIsNotAnOutage() {
        AuthException refused = AuthException.rejected(AuthException.SESSION_EXPIRED);

        assertThat(refused.getMessageKey()).isEqualTo(AuthException.SESSION_EXPIRED);
        assertThat(refused.isUnavailable()).isFalse();
    }

    @Test
    void anUnreachableProviderIsAnOutageKeepingItsCause() {
        IllegalStateException cause = new IllegalStateException("connection refused");

        AuthException outage = AuthException.unavailable(cause);

        assertThat(outage.getMessageKey()).isEqualTo(AuthException.IDENTITY_PROVIDER_UNAVAILABLE);
        assertThat(outage.isUnavailable()).isTrue();
        assertThat(outage).hasCause(cause);
        assertThat(AuthException.unavailable().isUnavailable()).isTrue();
    }

    @Test
    void aCopyIsTheSameRefusalCausedByTheOriginal() {
        AuthException original = AuthException.rejected(AuthException.SIGN_IN_REJECTED, new IllegalStateException());

        AuthException copy = AuthException.copyOf(original);

        assertThat(copy.getMessageKey()).isEqualTo(AuthException.SIGN_IN_REJECTED);
        assertThat(copy).hasCause(original);
    }
}
