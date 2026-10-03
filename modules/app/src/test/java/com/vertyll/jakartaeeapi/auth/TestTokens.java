package com.vertyll.jakartaeeapi.auth;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

final class TestTokens {
    static final String ISSUER = "http://keycloak.test/realms/jakarta-ee-api";
    static final String AUDIENCE = "jakarta-ee-api";
    static final String SUBJECT = "7d1c9a52-3a43-4c34-9a8f-2f5d7c6f1b10";

    private final RSAKey key;

    TestTokens() throws JOSEException {
        this.key = new RSAKeyGenerator(2048).keyID("test").generate();
    }

    TokenVerifier verifier() {
        return new TokenVerifier(new ImmutableJWKSet<>(new JWKSet(key.toPublicJWK())), ISSUER, AUDIENCE);
    }

    @SuppressWarnings(
        {
            "JavaUtilDate",
            "PMD.ReplaceJavaUtilDate"
        }
    )
    String token(String issuer, String audience, List<String> roles) throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(issuer)
            .audience(List.of(audience, "account"))
            .subject(SUBJECT)
            .claim("email", "ada@jakarta-ee-api.local")
            .claim("given_name", "Ada")
            .claim("realm_access", Map.of("roles", roles))
            .issueTime(new Date())
            .expirationTime(Date.from(Instant.now().plusSeconds(300)))
            .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test").build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    String validToken() throws JOSEException {
        return token(ISSUER, AUDIENCE, List.of("USER", "offline_access", "default-roles-jakarta-ee-api"));
    }
}
