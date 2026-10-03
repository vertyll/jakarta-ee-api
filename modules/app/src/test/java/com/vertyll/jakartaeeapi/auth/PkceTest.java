package com.vertyll.jakartaeeapi.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PkceTest {
    @Test
    void theChallengeIsTheRfc7636ExampleForItsVerifier() {
        assertThat(Pkce.challengeOf("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
            .isEqualTo("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
    }

    @Test
    void everyTransactionIsNew() {
        assertThat(Pkce.newTransaction().state()).isNotEqualTo(Pkce.newTransaction().state());
    }

    @Test
    void comparesStatesExactly() {
        assertThat(Pkce.sameState("abc", "abc")).isTrue();
        assertThat(Pkce.sameState("abc", "abd")).isFalse();
    }
}
