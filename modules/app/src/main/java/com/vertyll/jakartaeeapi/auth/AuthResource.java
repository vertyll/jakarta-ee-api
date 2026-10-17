package com.vertyll.jakartaeeapi.auth;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private static final Pattern LANGUAGE = Pattern.compile("[A-Za-z]{2,8}");
    private static final Pattern ENTRY = Pattern.compile(",");
    private static final Pattern PARAMETER = Pattern.compile(";");
    /** A weight as RFC 9110, 12.4.2 writes it: from 0 to 1, with at most three decimals. */
    private static final Pattern WEIGHT = Pattern.compile("0(\\.\\d{0,3})?|1(\\.0{0,3})?");
    private static final int MOST_LANGUAGES = 10;
    private static final Set<String> ACCOUNT_ACTIONS = Set.of("CONFIGURE_TOTP", "UPDATE_PASSWORD");
    // Keycloak removes a credential only when named by its id, and asks the person to confirm first.
    private static final Pattern DELETE_CREDENTIAL =
            Pattern.compile("delete_credential:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

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
        uiLocales(headers.getHeaderString(HttpHeaders.ACCEPT_LANGUAGE))
            .ifPresent(languages -> uri.queryParam("ui_locales", languages));
        if (kcAction != null && isAccountAction(kcAction)) {
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

    static boolean isAccountAction(String action) {
        return ACCOUNT_ACTIONS.contains(action) || DELETE_CREDENTIAL.matcher(action).matches();
    }

    /**
     * The reader's languages, most preferred first, for Keycloak, which shows the first one the realm supports.
     * Only plain language subtags of ASCII letters pass, as sent, so nothing else reaches the redirect. A language
     * weighted {@code q=0} is one the reader does not accept (RFC 9110, 12.4.2), and an entry whose weight cannot be
     * read is left out.
     */
    static Optional<String> uiLocales(@Nullable String acceptLanguage) {
        if (acceptLanguage == null) {
            return Optional.empty();
        }
        String languages = ENTRY.splitAsStream(acceptLanguage)
            .map(AuthResource::weighted)
            .flatMap(Optional::stream)
            .filter(language -> language.quality() > 0)
            .sorted(Comparator.comparingDouble(WeightedLanguage::quality).reversed())
            .map(WeightedLanguage::language)
            .distinct()
            .limit(MOST_LANGUAGES)
            .collect(Collectors.joining(" "));
        return languages.isEmpty() ? Optional.empty() : Optional.of(languages);
    }

    private static Optional<WeightedLanguage> weighted(String entry) {
        String[] parts = PARAMETER.split(entry, -1);
        String tag = parts[0].trim();
        String language = tag.contains("-") ? tag.substring(0, tag.indexOf('-')) : tag;
        if (!LANGUAGE.matcher(language).matches()) {
            return Optional.empty();
        }
        double quality = 1;
        for (int i = 1; i < parts.length; i++) {
            String parameter = parts[i].trim();
            // A language range takes no parameter but its weight, so anything else leaves the entry out.
            if (parameter.length() < 2 || (parameter.charAt(0) != 'q' && parameter.charAt(0) != 'Q')
                    || parameter.charAt(1) != '=') {
                return Optional.empty();
            }
            Matcher weight = WEIGHT.matcher(parameter.substring(2));
            if (!weight.matches()) {
                return Optional.empty();
            }
            quality = Double.parseDouble(weight.group());
        }
        return Optional.of(new WeightedLanguage(language, quality));
    }

    private record WeightedLanguage(String language, double quality) {
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
