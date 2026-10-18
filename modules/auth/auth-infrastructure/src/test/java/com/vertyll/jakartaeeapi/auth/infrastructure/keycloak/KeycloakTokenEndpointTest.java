package com.vertyll.jakartaeeapi.auth.infrastructure.keycloak;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vertyll.jakartaeeapi.auth.domain.AuthException;
import com.vertyll.jakartaeeapi.auth.domain.TokenPair;
import com.vertyll.jakartaeeapi.auth.infrastructure.redis.SharedRefreshes;

import com.nimbusds.jose.JOSEException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeycloakTokenEndpointTest {
    private static final int OK = 200;
    private static final int BAD_REQUEST = 400;
    private static final int SERVER_ERROR = 500;

    private final AtomicInteger tokenCalls = new AtomicInteger();
    private final AtomicInteger status = new AtomicInteger(OK);
    private final AtomicReference<String> revoked = new AtomicReference<>("");
    private final AtomicBoolean slow = new AtomicBoolean();
    private final CountDownLatch entered = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private HttpServer keycloak;

    @BeforeEach
    void start() throws IOException, JOSEException {
        String accessToken = new TestTokens().validToken();
        keycloak = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        keycloak.createContext("/realms/jakarta-ee-api/protocol/openid-connect/token", exchange -> {
            tokenCalls.incrementAndGet();
            entered.countDown();
            if (slow.get()) {
                awaitQuietly(release);
            }
            respond(
                exchange,
                status.get(),
                "{\"access_token\":\"" + accessToken + "\",\"refresh_token\":\"refresh-2\"}"
            );
        });
        keycloak.createContext("/realms/jakarta-ee-api/protocol/openid-connect/logout", exchange -> {
            revoked.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, status.get(), "");
        });
        keycloak.start();
    }

    @AfterEach
    void stop() {
        keycloak.stop(0);
    }

    @Test
    void concurrentRefreshesOfOneTokenReachKeycloakOnce() throws InterruptedException, ExecutionException, TimeoutException {
        slow.set(true);
        KeycloakTokenEndpoint client = client();

        CompletableFuture<TokenPair> first = CompletableFuture.supplyAsync(() -> client.refresh("refresh-1"));
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        CompletableFuture<TokenPair> second = CompletableFuture.supplyAsync(() -> client.refresh("refresh-1"));
        release.countDown();

        assertThat(first.get(5, TimeUnit.SECONDS).refreshToken()).isEqualTo("refresh-2");
        assertThat(second.get(5, TimeUnit.SECONDS).refreshToken()).isEqualTo("refresh-2");
        assertThat(tokenCalls).hasValue(1);
    }

    @Test
    void aSessionReadBeforeTheRefreshGetsTheTokensAlreadyIssued() {
        KeycloakTokenEndpoint client = client();

        client.refresh("refresh-1");

        assertThat(client.refresh("refresh-1").refreshToken()).isEqualTo("refresh-2");
        assertThat(tokenCalls).hasValue(1);
    }

    @Test
    void aRefusedRefreshEndsTheSessionAndIsNotRemembered() {
        status.set(BAD_REQUEST);
        KeycloakTokenEndpoint client = client();

        assertThatThrownBy(() -> client.refresh("refresh-1")).isInstanceOfSatisfying(
            AuthException.class,
            e -> assertThat(e.getMessageKey()).isEqualTo(AuthException.SESSION_EXPIRED)
        );
        assertThatThrownBy(() -> client.refresh("refresh-1")).isInstanceOf(AuthException.class);
        assertThat(tokenCalls).hasValue(2);
    }

    @Test
    void aCodeIsRedeemedForTheTokensKeycloakIssues() {
        TokenPair pair = client().exchange("code", "verifier");

        assertThat(pair.refreshToken()).isEqualTo("refresh-2");
        assertThat(tokenCalls).hasValue(1);
    }

    @Test
    void aRefusedCodeRejectsTheSignInAndAnUnreachableKeycloakIsAnOutage() {
        status.set(BAD_REQUEST);
        assertThatThrownBy(() -> client().exchange("code", "verifier")).isInstanceOfSatisfying(
            AuthException.class,
            e -> assertThat(e.getMessageKey()).isEqualTo(AuthException.SIGN_IN_REJECTED)
        );

        status.set(SERVER_ERROR);
        assertThatThrownBy(() -> client().exchange("code", "verifier"))
            .isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.isUnavailable()).isTrue());
    }

    @Test
    void revokingEndsTheKeycloakSessionOfTheRefreshToken() {
        client().revoke("refresh-1");

        assertThat(revoked.get()).contains("refresh_token=refresh-1", "client_id=jakarta-ee-api");
    }

    @Test
    void aSessionKeycloakWillNotEndIsOnlyLogged() {
        status.set(BAD_REQUEST);

        client().revoke("refresh-1");

        assertThat(revoked.get()).contains("refresh_token=refresh-1");
    }

    private KeycloakTokenEndpoint client() {
        String base = "http://127.0.0.1:" + keycloak.getAddress().getPort();
        AuthSettings settings = new AuthSettings(
            base + "/realms/jakarta-ee-api",
            "jakarta-ee-api",
            "secret",
            TestTokens.AUDIENCE,
            "http://app.test/api/auth/callback",
            "http://app.test/"
        );
        return new KeycloakTokenEndpoint(settings, SharedRefreshes.inProcessOnly());
    }

    private static void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
