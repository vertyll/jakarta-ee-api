package com.vertyll.jakartaeeapi.auth.infrastructure.web;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.vertyll.jakartaeeapi.auth.api.SignedInPrincipal;
import com.vertyll.jakartaeeapi.auth.application.port.inbound.SessionUseCase;
import com.vertyll.jakartaeeapi.auth.application.port.inbound.TokenAuthenticationUseCase;
import com.vertyll.jakartaeeapi.auth.domain.AuthException;
import com.vertyll.jakartaeeapi.auth.domain.AuthSession;
import com.vertyll.jakartaeeapi.auth.domain.Identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationFilterTest {
    private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");
    private static final Identity IDENTITY = new Identity("subject", "ada@example.test", "ada", List.of("USER"));

    private final TokenAuthenticationUseCase tokens = mock(TokenAuthenticationUseCase.class);
    private final SessionUseCase sessions = mock(SessionUseCase.class);
    private final BrowserRequests browser = new BrowserRequests();
    private final ContainerRequestContext context = mock(ContainerRequestContext.class);
    private final AuthenticationFilter filter =
            new AuthenticationFilter(tokens, sessions, Clock.fixed(NOW, ZoneOffset.UTC), browser.request());

    @BeforeEach
    void requests() {
        when(context.getMethod()).thenReturn("GET");
        when(tokens.authenticate(any())).thenReturn(IDENTITY);
    }

    @Test
    void aBearerTokenMakesItsHolderTheCaller() {
        when(context.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token");

        filter.filter(context);

        verify(tokens).authenticate("token");
        assertThat(caller().person().email()).isEqualTo("ada@example.test");
    }

    @Test
    void aRequestFromThisSiteIsAuthorizedByItsSessionsToken() {
        when(context.getHeaderString(FetchMetadata.FETCH_SITE_HEADER)).thenReturn("same-origin");
        BrowserSessions.establish(browser.request(), session("access", NOW.plusSeconds(300)));

        filter.filter(context);

        verify(tokens).authenticate("access");
        verify(sessions, never()).refresh(any());
    }

    @Test
    void aChangeSentFromAnotherSiteIsNotAuthorizedByTheSession() {
        when(context.getMethod()).thenReturn("POST");
        when(context.getHeaderString(FetchMetadata.FETCH_SITE_HEADER)).thenReturn("cross-site");
        BrowserSessions.establish(browser.request(), session("access", NOW.plusSeconds(300)));

        filter.filter(context);

        verify(tokens, never()).authenticate(any());
        verify(context, never()).setSecurityContext(any());
    }

    @Test
    void aSessionAboutToExpireIsRefreshedAndKept() {
        when(context.getHeaderString(FetchMetadata.FETCH_SITE_HEADER)).thenReturn("same-origin");
        AuthSession expiring = session("access", NOW.plusSeconds(10));
        AuthSession refreshed = session("access-2", NOW.plusSeconds(300));
        BrowserSessions.establish(browser.request(), expiring);
        when(sessions.refresh(expiring)).thenReturn(refreshed);

        filter.filter(context);

        verify(tokens).authenticate("access-2");
        assertThat(BrowserSessions.current(browser.request())).contains(refreshed);
    }

    @Test
    void aSessionKeycloakNoLongerRefreshesIsEndedAndTheRequestGoesOnWithoutACaller() {
        when(context.getHeaderString(FetchMetadata.FETCH_SITE_HEADER)).thenReturn("same-origin");
        AuthSession expiring = session("access", NOW.plusSeconds(10));
        BrowserSessions.establish(browser.request(), expiring);
        when(sessions.refresh(expiring)).thenThrow(AuthException.rejected(AuthException.SESSION_EXPIRED));

        filter.filter(context);

        assertThat(BrowserSessions.current(browser.request())).isEmpty();
        verify(context, never()).setSecurityContext(any());
    }

    @Test
    void aSessionThatCannotBeRefreshedForNowIsKeptForTheNextRequest() {
        when(context.getHeaderString(FetchMetadata.FETCH_SITE_HEADER)).thenReturn("same-origin");
        AuthSession expiring = session("access", NOW.plusSeconds(10));
        BrowserSessions.establish(browser.request(), expiring);
        when(sessions.refresh(expiring)).thenThrow(AuthException.unavailable());

        filter.filter(context);

        assertThat(BrowserSessions.current(browser.request())).contains(expiring);
        verify(context, never()).setSecurityContext(any());
    }

    @Test
    void aRequestWithNeitherATokenNorASessionHasNoCaller() {
        filter.filter(context);

        verify(context, never()).setSecurityContext(any());
    }

    private SignedInPrincipal caller() {
        ArgumentCaptor<SecurityContext> security = ArgumentCaptor.forClass(SecurityContext.class);
        verify(context).setSecurityContext(security.capture());
        return (SignedInPrincipal) security.getValue().getUserPrincipal();
    }

    private static AuthSession session(String accessToken, Instant expiresAt) {
        return new AuthSession(IDENTITY, accessToken, "refresh", expiresAt);
    }
}
