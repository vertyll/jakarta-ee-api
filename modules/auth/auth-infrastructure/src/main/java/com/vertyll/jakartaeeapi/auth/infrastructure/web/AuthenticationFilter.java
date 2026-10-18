package com.vertyll.jakartaeeapi.auth.infrastructure.web;

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

import com.vertyll.jakartaeeapi.auth.api.SignedInPerson;
import com.vertyll.jakartaeeapi.auth.api.SignedInPrincipal;
import com.vertyll.jakartaeeapi.auth.application.port.inbound.SessionUseCase;
import com.vertyll.jakartaeeapi.auth.application.port.inbound.TokenAuthenticationUseCase;
import com.vertyll.jakartaeeapi.auth.domain.AuthException;
import com.vertyll.jakartaeeapi.auth.domain.AuthSession;
import com.vertyll.jakartaeeapi.auth.domain.Identity;

import lombok.extern.slf4j.Slf4j;

/**
 * Makes the caller of a request known: from a bearer token, or from the browser's session when the request comes from
 * this site, refreshing the session's tokens shortly before they expire.
 */
@Slf4j
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Duration REFRESH_SKEW = Duration.ofSeconds(30);

    private Clock clock = Clock.systemUTC();

    // RESTEasy in Liberty builds a provider with its public no-argument constructor and then lets CDI fill the fields,
    // so the use cases arrive as fields rather than through a constructor.
    @Inject
    @SuppressWarnings("java:S6813")
    private TokenAuthenticationUseCase tokens;

    @Inject
    @SuppressWarnings("java:S6813")
    private SessionUseCase sessions;

    @Context
    private HttpServletRequest request;

    @SuppressWarnings("java:S2637")
    public AuthenticationFilter() {
        // The container injects every field.
    }

    // For tests, which have no container to fill the fields.
    @SuppressWarnings("UnnecessaryAssignment")
    AuthenticationFilter(
        TokenAuthenticationUseCase tokens,
        SessionUseCase sessions,
        Clock clock,
        HttpServletRequest request
    ) {
        this.tokens = tokens;
        this.sessions = sessions;
        this.clock = clock;
        this.request = request;
    }

    @Override
    public void filter(ContainerRequestContext context) {
        Optional<String> token = bearerToken(context).or(() -> sessionToken(context));
        token.ifPresent(value -> {
            Identity identity = tokens.authenticate(value);
            boolean secure = context.getSecurityContext() != null && context.getSecurityContext().isSecure();
            context.setSecurityContext(new CallerSecurityContext(new SignedInPrincipal(person(identity)), secure));
        });
    }

    private static SignedInPerson person(Identity identity) {
        return new SignedInPerson(identity.keycloakId(), identity.email(), identity.username(), identity.roles());
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
