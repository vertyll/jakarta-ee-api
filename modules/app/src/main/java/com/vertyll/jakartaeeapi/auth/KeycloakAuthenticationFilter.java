package com.vertyll.jakartaeeapi.auth;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

import org.jspecify.annotations.Nullable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
@Priority(Priorities.AUTHENTICATION)
public class KeycloakAuthenticationFilter implements ContainerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Duration REFRESH_SKEW = Duration.ofSeconds(30);

    private final Clock clock = Clock.systemUTC();

    @Inject
    private TokenVerifier verifier;

    @Inject
    private SessionService sessions;

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext context) {
        Optional<String> token = bearerToken(context).or(() -> sessionToken(context));
        token.ifPresent(value -> {
            KeycloakIdentity identity = verifier.verify(value).identity();
            boolean secure = context.getSecurityContext() != null && context.getSecurityContext().isSecure();
            context.setSecurityContext(new KeycloakSecurityContext(new KeycloakPrincipal(identity), secure));
        });
    }

    private static Optional<String> bearerToken(ContainerRequestContext context) {
        String authorization = context.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(authorization.substring(BEARER_PREFIX.length()));
    }

    private Optional<String> sessionToken(ContainerRequestContext context) {
        if (!FetchMetadata
            .sentFromThisOrigin(context.getMethod(), context.getHeaderString(FetchMetadata.FETCH_SITE_HEADER))) {
            return Optional.empty();
        }
        return BrowserSessions.current(request).map(this::fresh).map(AuthSession::accessToken);
    }

    private @Nullable AuthSession fresh(AuthSession current) {
        if (!current.needsRefreshAt(clock.instant(), REFRESH_SKEW)) {
            return current;
        }
        try {
            AuthSession refreshed = sessions.refresh(current);
            BrowserSessions.replace(request, refreshed);
            return refreshed;
        } catch (AuthException e) {
            if (AuthException.SESSION_EXPIRED.equals(e.getMessageKey())) {
                BrowserSessions.end(request);
            } else {
                log.warn("Could not refresh the session of {}: {}", current.identity().keycloakId(), e.getMessageKey());
            }
            return null;
        }
    }
}
