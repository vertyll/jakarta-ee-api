package com.vertyll.jakartaeeapi.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResourceTest {

    @Test
    void removingACredentialNamedByItsIdIsPassedOn() {
        assertThat(AuthResource.isAccountAction("delete_credential:0b7f6a52-3c1e-4d2a-9f5b-6e8d7c1a2b3c")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "delete_credential",
            "VERIFY_EMAIL",
            "delete_credential:0b7f6a52-3c1e-4d2a-9f5b-6e8d7c1a2b3c&prompt=none",
            "delete_credential:0b7f6a52-3c1e-4d2a-9f5b-6e8d7c1a2b3c CONFIGURE_TOTP",
            "delete_credential:0B7F6A52-3C1E-4D2A-9F5B-6E8D7C1A2B3C",
            "delete_credential:0b7f6a52-3c1e-4d2a-9f5b",
            "x delete_credential:0b7f6a52-3c1e-4d2a-9f5b-6e8d7c1a2b3c"
        }
    )
    void anActionKeycloakIsNotAskedForIsDropped(String action) {
        assertThat(AuthResource.isAccountAction(action)).isFalse();
    }

    @Test
    void theReadersLanguagesGoToKeycloakByTheirWeight() {
        assertThat(AuthResource.uiLocales("de;q=0.5, en-GB, pl;q=0.8")).hasValue("en pl de");
    }

    @Test
    void aLanguageTheReaderDoesNotAcceptIsLeftOut() {
        assertThat(AuthResource.uiLocales("pl, de;q=0")).hasValue("pl");
        assertThat(AuthResource.uiLocales("pl;Q=0, en")).hasValue("en");
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "*",
            "a&b=c",
            "{x}",
            "pl en",
            "pl;q=high",
            "pl;q=5",
            "pl;q=Infinity",
            "pl;q=0x1p0",
            "pl;q =0",
            "pl;level=1"
        }
    )
    void anEntryThatIsNoLanguageIsLeftOut(String acceptLanguage) {
        assertThat(AuthResource.uiLocales(acceptLanguage)).isEmpty();
    }

    @Test
    void noLanguageIsSentWithoutTheHeader() {
        assertThat(AuthResource.uiLocales(null)).isEmpty();
    }

    @Test
    void atMostTenLanguagesGoToKeycloak() {
        assertThat(AuthResource.uiLocales("aa, ab, ac, ad, ae, af, ag, ah, ai, aj, ak, al"))
            .hasValue("aa ab ac ad ae af ag ah ai aj");
    }
}
