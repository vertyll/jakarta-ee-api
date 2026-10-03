package com.vertyll.jakartaeeapi.auth;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.ParseException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

@Singleton
public class TokenVerifier {
    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";
    private static final String DEFAULT_ROLES_PREFIX = "default-roles-";
    private static final Set<String> BUILT_IN_ROLES = Set.of("offline_access", "uma_authorization");

    private final ConfigurableJWTProcessor<SecurityContext> processor;

    @Inject
    public TokenVerifier(AuthSettings settings) {
        this(remoteKeys(settings), settings.realmUrl(), settings.audience());
    }

    public TokenVerifier(JWKSource<SecurityContext> keys, String issuer, String audience) {
        this.processor = processor(keys, issuer, audience);
    }

    public VerifiedToken verify(String token) {
        try {
            JWTClaimsSet claims = processor.process(token, null);
            return new VerifiedToken(identity(claims), expiry(claims));
        } catch (ParseException | BadJOSEException | JOSEException e) {
            throw AuthException.rejected(AuthException.TOKEN_INVALID, e);
        }
    }

    private static JWKSource<SecurityContext> remoteKeys(AuthSettings settings) {
        try {
            return JWKSourceBuilder.create(URI.create(settings.endpoint("certs")).toURL()).build();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("The Keycloak realm URL is not valid", e);
        }
    }

    private static ConfigurableJWTProcessor<SecurityContext> processor(
        JWKSource<SecurityContext> keys,
        String issuer,
        String audience
    ) {
        DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keys));
        jwtProcessor.setJWTClaimsSetVerifier(
            new DefaultJWTClaimsVerifier<>(
                audience,
                new JWTClaimsSet.Builder().issuer(issuer).build(),
                Set.of("sub", "exp", "email")
            )
        );
        return jwtProcessor;
    }

    private static KeycloakIdentity identity(JWTClaimsSet claims) throws ParseException {
        return new KeycloakIdentity(
            claims.getSubject(),
            claims.getStringClaim("email"),
            orEmpty(claims.getStringClaim("given_name")),
            orEmpty(claims.getStringClaim("family_name")),
            roles(claims.getClaim(REALM_ACCESS))
        );
    }

    private static Instant expiry(JWTClaimsSet claims) {
        return claims.getExpirationTime().toInstant();
    }

    private static List<String> roles(@Nullable Object realmAccess) {
        if (!(realmAccess instanceof Map<?, ?> access) || !(access.get(ROLES) instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .filter(role -> !BUILT_IN_ROLES.contains(role) && !role.startsWith(DEFAULT_ROLES_PREFIX))
            .toList();
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    public record VerifiedToken(KeycloakIdentity identity, Instant expiresAt) {
    }
}
