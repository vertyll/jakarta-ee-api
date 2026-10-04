package com.vertyll.jakartaeeapi.auth;

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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JOSEException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeycloakTokenClientTest {
    private static final int OK = 200;
    private static final int BAD_REQUEST = 400;

    private final AtomicInteger tokenCalls = new AtomicInteger();
    private final AtomicInteger status = new AtomicInteger(OK);
    private final AtomicBoolean slow = new AtomicBoolean();
    private final CountDownLatch entered = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private TestTokens tokens;
    private HttpServer keycloak;

    @BeforeEach
    void start() throws IOException, JOSEException {
        tokens = new TestTokens();
        String accessToken = tokens.validToken();
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
        keycloak.start();
    }

    @AfterEach
    void stop() {
        keycloak.stop(0);
    }

    @Test
    void concurrentRefreshesOfOneTokenReachKeycloakOnce() throws InterruptedException, ExecutionException, TimeoutException {
        slow.set(true);
        KeycloakTokenClient client = client();

        CompletableFuture<AuthSession> first = CompletableFuture.supplyAsync(() -> client.refresh("refresh-1"));
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        CompletableFuture<AuthSession> second = CompletableFuture.supplyAsync(() -> client.refresh("refresh-1"));
        release.countDown();

        assertThat(first.get(5, TimeUnit.SECONDS).refreshToken()).isEqualTo("refresh-2");
        assertThat(second.get(5, TimeUnit.SECONDS).refreshToken()).isEqualTo("refresh-2");
        assertThat(tokenCalls).hasValue(1);
    }

    @Test
    void aSessionReadBeforeTheRefreshGetsTheTokensAlreadyIssued() {
        KeycloakTokenClient client = client();

        client.refresh("refresh-1");

        assertThat(client.refresh("refresh-1").refreshToken()).isEqualTo("refresh-2");
        assertThat(tokenCalls).hasValue(1);
    }

    @Test
    void aRefusedRefreshEndsTheSessionAndIsNotRemembered() {
        status.set(BAD_REQUEST);
        KeycloakTokenClient client = client();

        assertThatThrownBy(() -> client.refresh("refresh-1")).isInstanceOfSatisfying(
            AuthException.class,
            e -> assertThat(e.getMessageKey()).isEqualTo(AuthException.SESSION_EXPIRED)
        );
        assertThatThrownBy(() -> client.refresh("refresh-1")).isInstanceOf(AuthException.class);
        assertThat(tokenCalls).hasValue(2);
    }

    private KeycloakTokenClient client() {
        String base = "http://127.0.0.1:" + keycloak.getAddress().getPort();
        AuthSettings settings = new AuthSettings(
            base + "/realms/jakarta-ee-api",
            "jakarta-ee-api",
            "secret",
            TestTokens.AUDIENCE,
            "http://app.test/api/auth/callback",
            "http://app.test/"
        );
        return new KeycloakTokenClient(settings, tokens.verifier(), SharedRefreshes.inProcessOnly());
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
