package com.vertyll.jakartaeeapi.auth;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.jspecify.annotations.Nullable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@PermitAll
@Path("/auth")
public class AuthResource {
    private static final String SCOPE = "openid profile email";
    private static final String ERROR_PARAM = "error";
    private static final String SIGN_IN_FAILED = "sign_in_failed";
    private static final String STATE_MISMATCH = "state_mismatch";
    private static final Set<String> ALLOWED_ACTIONS = Set.of("CONFIGURE_TOTP", "UPDATE_PASSWORD", "delete_credential");
    private static final Set<String> UI_LOCALES = Set.of("pl", "en");

    private final SessionService sessions;
    private final AuthSettings settings;

    @Inject
    public AuthResource(SessionService sessions, AuthSettings settings) {
        this.sessions = sessions;
        this.settings = settings;
    }

    @GET
    @Path("/authorize")
    public Response authorize(
        @QueryParam("kc_action") @Nullable String kcAction,
        @QueryParam("register") @DefaultValue("false") boolean register,
        @Context HttpServletRequest request,
        @Context HttpHeaders headers
    ) {
        SignInTransaction transaction = BrowserSessions.begin(request);
        UriBuilder uri = UriBuilder.fromUri(settings.endpoint("auth"))
            .queryParam("client_id", settings.clientId())
            .queryParam("redirect_uri", settings.callbackUrl())
            .queryParam("response_type", "code")
            .queryParam("scope", SCOPE)
            .queryParam("state", transaction.state())
            .queryParam("code_challenge", Pkce.challengeOf(transaction.codeVerifier()))
            .queryParam("code_challenge_method", Pkce.CHALLENGE_METHOD);
        language(headers).ifPresent(language -> uri.queryParam("ui_locales", language));
        if (kcAction != null && ALLOWED_ACTIONS.contains(kcAction)) {
            uri.queryParam("kc_action", kcAction);
        }
        if (register) {
            uri.queryParam("prompt", "create");
        }
        return Response.seeOther(uri.build()).build();
    }

    @GET
    @Path("/callback")
    public Response callback(
        @QueryParam("code") @Nullable String code,
        @QueryParam("state") @Nullable String state,
        @QueryParam(ERROR_PARAM) @Nullable String error,
        @Context HttpServletRequest request
    ) {
        Optional<SignInTransaction> transaction = BrowserSessions.takeTransaction(request);
        if (error != null) {
            log.debug("Keycloak returned an authorization error: {}", error);
            return redirectToApp(SIGN_IN_FAILED);
        }
        if (code == null || state == null || transaction.isEmpty()
                || !Pkce.sameState(transaction.get().state(), state)) {
            log.warn("Rejecting a sign-in callback whose state was not issued to this browser");
            return redirectToApp(STATE_MISMATCH);
        }
        try {
            BrowserSessions.establish(request, sessions.signIn(code, transaction.get().codeVerifier()));
            return redirectToApp(null);
        } catch (AuthException e) {
            log.warn("Sign-in could not be completed: {}", e.getMessageKey());
            return redirectToApp(SIGN_IN_FAILED);
        }
    }

    @GET
    @Path("/session")
    @Produces(MediaType.APPLICATION_JSON)
    public Response session(@Context HttpServletRequest request) {
        return BrowserSessions.current(request)
            .map(session -> Response.ok(SessionResponse.from(session)).build())
            .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/logout")
    public Response logout(@Context HttpServletRequest request, @Context HttpHeaders headers) {
        if (!FetchMetadata
            .sentFromThisOrigin(request.getMethod(), headers.getHeaderString(FetchMetadata.FETCH_SITE_HEADER))) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        BrowserSessions.current(request).ifPresent(sessions::signOut);
        BrowserSessions.end(request);
        return Response.noContent().build();
    }

    private static Optional<String> language(HttpHeaders headers) {
        List<Locale> accepted = headers.getAcceptableLanguages();
        return accepted.stream().map(Locale::getLanguage).filter(UI_LOCALES::contains).findFirst();
    }

    private Response redirectToApp(@Nullable String errorCode) {
        UriBuilder uri = UriBuilder.fromUri(settings.postLoginUrl());
        if (errorCode != null) {
            uri.queryParam(ERROR_PARAM, errorCode);
        }
        URI location = uri.build();
        return Response.seeOther(location).build();
    }

    public record SessionResponse(String userId, String email, List<String> roles) {
        static SessionResponse from(AuthSession session) {
            KeycloakIdentity identity = session.identity();
            return new SessionResponse(identity.keycloakId(), identity.email(), identity.roles());
        }
    }
}
