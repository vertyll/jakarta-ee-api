package com.vertyll.jakartaeeapi.auth;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbProperty;

import org.jspecify.annotations.Nullable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class KeycloakTokenClient {
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REUSE_WINDOW = Duration.ofSeconds(30);
    private static final String GRANT_TYPE = "grant_type";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final int CLIENT_ERROR = 400;
    private static final int SERVER_ERROR = 500;
    private static final Jsonb JSONB = JsonbBuilder.create();

    private final Map<String, Refresh> refreshes = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    private final AuthSettings settings;
    private final TokenVerifier verifier;

    @Inject
    public KeycloakTokenClient(AuthSettings settings, TokenVerifier verifier) {
        this.settings = settings;
        this.verifier = verifier;
    }

    public AuthSession exchange(String code, String codeVerifier) {
        Map<String, String> form = form();
        form.put(GRANT_TYPE, "authorization_code");
        form.put("code", code);
        form.put("code_verifier", codeVerifier);
        form.put("redirect_uri", settings.callbackUrl());
        return post(form, AuthException.SIGN_IN_REJECTED);
    }

    public AuthSession refresh(String refreshToken) {
        forgetOldRefreshes();
        Refresh mine = new Refresh();
        Refresh running = refreshes.putIfAbsent(refreshToken, mine);
        if (running != null) {
            Outcome outcome = running.await();
            AuthSession shared = outcome.session();
            if (shared != null) {
                return shared;
            }
            AuthException failure = outcome.failure();
            if (failure != null) {
                throw AuthException.copyOf(failure);
            }
            return requestRefresh(refreshToken);
        }
        try {
            AuthSession session = requestRefresh(refreshToken);
            mine.finish(new Outcome(session, null, clock.instant()));
            return session;
        } catch (AuthException e) {
            refreshes.remove(refreshToken, mine);
            mine.finish(new Outcome(null, e, clock.instant()));
            throw e;
        } finally {
            if (!mine.isFinished()) {
                refreshes.remove(refreshToken, mine);
                mine.finish(new Outcome(null, null, clock.instant()));
            }
        }
    }

    public void revoke(String refreshToken) {
        Map<String, String> form = form();
        form.put(REFRESH_TOKEN, refreshToken);
        try {
            HttpResponse<String> response = send(settings.endpoint("logout"), form);
            if (response.statusCode() >= CLIENT_ERROR) {
                log.warn("Keycloak did not end the session: {}", response.statusCode());
            }
        } catch (IOException e) {
            log.warn("Keycloak did not end the session: {}", e.getMessage());
        }
    }

    private AuthSession requestRefresh(String refreshToken) {
        Map<String, String> form = form();
        form.put(GRANT_TYPE, REFRESH_TOKEN);
        form.put(REFRESH_TOKEN, refreshToken);
        return post(form, AuthException.SESSION_EXPIRED);
    }

    private AuthSession post(Map<String, String> form, String onRejection) {
        HttpResponse<String> response;
        try {
            response = send(settings.endpoint("token"), form);
        } catch (IOException e) {
            throw AuthException.unavailable(e);
        }
        if (response.statusCode() >= SERVER_ERROR) {
            throw AuthException.unavailable();
        }
        if (response.statusCode() >= CLIENT_ERROR) {
            throw AuthException.rejected(onRejection);
        }
        TokenResponse tokens = parse(response.body());
        String accessToken = tokens.accessToken();
        String refreshToken = tokens.refreshToken();
        if (accessToken == null || refreshToken == null) {
            throw AuthException.unavailable();
        }
        TokenVerifier.VerifiedToken verified;
        try {
            verified = verifier.verify(accessToken);
        } catch (AuthException e) {
            throw AuthException.rejected(onRejection, e);
        }
        return new AuthSession(verified.identity(), accessToken, refreshToken, verified.expiresAt());
    }

    private HttpResponse<String> send(String uri, Map<String, String> form) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
            .timeout(TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(encode(form)))
            .build();
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while calling Keycloak", e);
        }
    }

    private Map<String, String> form() {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", settings.clientId());
        form.put("client_secret", settings.clientSecret());
        return form;
    }

    private void forgetOldRefreshes() {
        Instant oldest = clock.instant().minus(REUSE_WINDOW);
        refreshes.values().removeIf(refresh -> refresh.finishedBefore(oldest));
    }

    private static String encode(Map<String, String> form) {
        return form.entrySet()
            .stream()
            .map(
                e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)
            )
            .collect(Collectors.joining("&"));
    }

    private static TokenResponse parse(String body) {
        try {
            return JSONB.fromJson(body, TokenResponse.class);
        } catch (JsonbException e) {
            throw AuthException.unavailable(e);
        }
    }

    public record TokenResponse(
        @JsonbProperty("access_token") @Nullable String accessToken,
        @JsonbProperty("refresh_token") @Nullable String refreshToken
    ) {
    }

    private record Outcome(@Nullable AuthSession session, @Nullable AuthException failure, Instant at) {
    }

    private static final class Refresh {
        private final CompletableFuture<Outcome> result = new CompletableFuture<>();

        void finish(Outcome outcome) {
            result.complete(outcome);
        }

        boolean isFinished() {
            return result.isDone();
        }

        Outcome await() {
            return result.join();
        }

        boolean finishedBefore(Instant instant) {
            Outcome outcome = result.getNow(null);
            return outcome != null && outcome.at().isBefore(instant);
        }
    }
}
