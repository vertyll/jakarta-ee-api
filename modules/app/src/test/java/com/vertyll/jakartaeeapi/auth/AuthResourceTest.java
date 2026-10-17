package com.vertyll.jakartaeeapi.auth;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResourceTest {

    @Test
    void removingACredentialNamedByItsIdIsPassedOn() {
        assertThat(AuthResource.isAccountAction("delete_credential:0b7f6a52-3c1e-4d2a-9f5b-6e8d7c1a2b3c")).isTrue();
    }

    @Test
    void anActionKeycloakIsNotAskedForIsDropped() {
        assertThat(AuthResource.isAccountAction("delete_credential")).isFalse();
        assertThat(AuthResource.isAccountAction("VERIFY_EMAIL")).isFalse();
    }

    @Test
    void theReadersLanguagesGoToKeycloakInTheirOrder() {
        assertThat(AuthResource.uiLocales(List.of(Locale.GERMAN, Locale.forLanguageTag("en-GB"), Locale.of("pl"))))
            .hasValue("de en pl");
    }

    @Test
    void noLanguageIsSentWhenTheReaderNamesNone() {
        assertThat(AuthResource.uiLocales(List.of(Locale.of("*")))).isEmpty();
    }
}
