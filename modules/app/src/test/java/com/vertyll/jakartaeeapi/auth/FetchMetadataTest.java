package com.vertyll.jakartaeeapi.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FetchMetadataTest {

    @Test
    void readsAreAllowedFromAnywhere() {
        assertThat(FetchMetadata.sentFromThisOrigin("GET", "cross-site")).isTrue();
    }

    @Test
    void writesAreAllowedFromThisOriginOrWithoutTheHeader() {
        assertThat(FetchMetadata.sentFromThisOrigin("POST", "same-origin")).isTrue();
        assertThat(FetchMetadata.sentFromThisOrigin("POST", "none")).isTrue();
        assertThat(FetchMetadata.sentFromThisOrigin("POST", null)).isTrue();
    }

    @Test
    void writesFromAnotherSiteOrSubdomainAreRefused() {
        assertThat(FetchMetadata.sentFromThisOrigin("POST", "cross-site")).isFalse();
        assertThat(FetchMetadata.sentFromThisOrigin("POST", "same-site")).isFalse();
    }
}
