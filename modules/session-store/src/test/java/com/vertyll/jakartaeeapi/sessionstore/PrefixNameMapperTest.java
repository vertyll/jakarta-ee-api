package com.vertyll.jakartaeeapi.sessionstore;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrefixNameMapperTest {

    @Test
    void namesTheSessionCachesUnderTheApplication() {
        PrefixNameMapper mapper = new PrefixNameMapper("jakarta-ee-api");

        assertThat(mapper.map("com.ibm.ws.session.attr.default_host%2F"))
            .isEqualTo("jakarta-ee-api:session:com.ibm.ws.session.attr.default_host%2F");
        assertThat(mapper.unmap("jakarta-ee-api:session:com.ibm.ws.session.attr.default_host%2F"))
            .isEqualTo("com.ibm.ws.session.attr.default_host%2F");
    }

    @Test
    void fallsBackToTheApplicationName() {
        assertThat(new PrefixNameMapper(" ").map("cache")).isEqualTo("jakarta-ee-api:session:cache");
    }
}
