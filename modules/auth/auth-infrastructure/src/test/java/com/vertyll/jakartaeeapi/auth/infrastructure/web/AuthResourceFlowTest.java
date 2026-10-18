package com.vertyll.jakartaeeapi.auth.infrastructure.web;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.vertyll.jakartaeeapi.auth.application.port.inbound.SessionUseCase;
import com.vertyll.jakartaeeapi.auth.domain.AuthException;
import com.vertyll.jakartaeeapi.auth.domain.AuthSession;
import com.vertyll.jakartaeeapi.auth.domain.Identity;
import com.vertyll.jakartaeeapi.auth.domain.SignInTransaction;
import com.vertyll.jakartaeeapi.auth.infrastructure.keycloak.AuthSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The sign-in, the session and the sign-out, through the resource as Keycloak and the browser drive them. */
class AuthResourceFlowTest {
    private static final AuthSettings SETTINGS = new AuthSettings(
        "http://keycloak.test/realms/app",
        "app",
        "secret",
        "app",
        "http://app.test/api/auth/callback",
        "http://app.test/"
    );
    private static final AuthSession SESSION = new AuthSession(
        new Identity("subject", "ada@example.test", "ada", List.of("USER")),
        "access",
        "refresh",
        Instant.EPOCH
    );

    private final SessionUseCase sessions = mock(SessionUseCase.class);
    private final AuthResource resource = new AuthResource(sessions, SETTINGS);
    private final BrowserRequests browser = new BrowserRequests();
    private final HttpHeaders headers = mock(HttpHeaders.class);

    @Test
    void signingInSendsTheBrowserToKeycloakWithPkceStateAndItsLanguages() {
        when(headers.getHeaderString(HttpHeaders.ACCEPT_LANGUAGE)).thenReturn("pl-PL,en;q=0.8");

        String location;
        try (Response response = resource.authorize("CONFIGURE_TOTP", true, browser.request(), headers)) {
            assertThat(response.getStatus()).isEqualTo(303);
            location = response.getLocation().toString();
        }
        SignInTransaction transaction = BrowserSessions.takeTransaction(browser.request()).orElseThrow();
        assertThat(location).startsWith("http://keycloak.test/realms/app/protocol/openid-connect/auth?")
            .contains("client_id=app", "response_type=code", "code_challenge_method=S256")
            .contains("state=" + transaction.state(), "ui_locales=pl+en", "kc_action=CONFIGURE_TOTP", "prompt=create");
    }

    @Test
    void anActionKeycloakIsNotAskedForIsLeftOut() {
        try (Response response = resource.authorize("DROP_TABLES", false, browser.request(), headers)) {
            assertThat(response.getLocation().toString()).doesNotContain("kc_action", "prompt", "ui_locales");
        }
    }

    @Test
    void aCallbackWithTheIssuedStateSignsInAndLandsOnTheApplication() {
        SignInTransaction transaction = BrowserSessions.begin(browser.request());
        when(sessions.signIn("code", transaction.codeVerifier())).thenReturn(SESSION);

        assertThat(location(resource.callback("code", transaction.state(), null, browser.request())))
            .isEqualTo(URI.create("http://app.test/"));
        assertThat(BrowserSessions.current(browser.request())).contains(SESSION);
    }

    @Test
    void aCallbackWithAStateThisBrowserWasNotGivenIsRefused() {
        BrowserSessions.begin(browser.request());

        assertThat(location(resource.callback("code", "forged", null, browser.request())))
            .isEqualTo(URI.create("http://app.test/?error=state_mismatch"));
        verify(sessions, never()).signIn("code", "forged");
    }

    @Test
    void anErrorFromKeycloakOrARefusedCodeEndsTheSignIn() {
        assertThat(location(resource.callback(null, null, "access_denied", browser.request())))
            .isEqualTo(URI.create("http://app.test/?error=sign_in_failed"));

        SignInTransaction transaction = BrowserSessions.begin(browser.request());
        when(sessions.signIn("code", transaction.codeVerifier()))
            .thenThrow(AuthException.rejected(AuthException.SIGN_IN_REJECTED));
        assertThat(location(resource.callback("code", transaction.state(), null, browser.request())))
            .isEqualTo(URI.create("http://app.test/?error=sign_in_failed"));
        assertThat(BrowserSessions.current(browser.request())).isEmpty();
    }

    @Test
    void theSessionShowsWhoIsSignedInOrNothing() {
        try (Response nobody = resource.session(browser.request())) {
            assertThat(nobody.getStatus()).isEqualTo(204);
        }

        BrowserSessions.establish(browser.request(), SESSION);
        try (Response response = resource.session(browser.request())) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity())
                .isEqualTo(new AuthResource.SessionResponse("subject", "ada@example.test", List.of("USER")));
        }
    }

    @Test
    void signingOutFromThisSiteEndsTheSessionInKeycloakAndHere() {
        BrowserSessions.establish(browser.request(), SESSION);
        when(browser.request().getMethod()).thenReturn("POST");
        when(headers.getHeaderString(FetchMetadata.FETCH_SITE_HEADER)).thenReturn("same-origin");

        try (Response response = resource.logout(browser.request(), headers)) {
            assertThat(response.getStatus()).isEqualTo(204);
        }
        verify(sessions).signOut(SESSION);
        assertThat(BrowserSessions.current(browser.request())).isEmpty();
    }

    @Test
    void signingOutFromAnotherSiteIsForbidden() {
        BrowserSessions.establish(browser.request(), SESSION);
        when(browser.request().getMethod()).thenReturn("POST");
        when(headers.getHeaderString(FetchMetadata.FETCH_SITE_HEADER)).thenReturn("cross-site");

        try (Response response = resource.logout(browser.request(), headers)) {
            assertThat(response.getStatus()).isEqualTo(403);
        }
        assertThat(BrowserSessions.current(browser.request())).contains(SESSION);
    }

    private static URI location(Response response) {
        try (response) {
            return response.getLocation();
        }
    }
}
