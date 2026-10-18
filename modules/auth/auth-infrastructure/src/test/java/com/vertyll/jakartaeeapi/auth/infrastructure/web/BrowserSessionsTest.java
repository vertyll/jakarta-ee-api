package com.vertyll.jakartaeeapi.auth.infrastructure.web;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vertyll.jakartaeeapi.auth.domain.AuthSession;
import com.vertyll.jakartaeeapi.auth.domain.Identity;
import com.vertyll.jakartaeeapi.auth.domain.SignInTransaction;

import static org.assertj.core.api.Assertions.assertThat;

class BrowserSessionsTest {
    private static final AuthSession SESSION = new AuthSession(
        new Identity("subject", "ada@example.test", "ada", List.of("USER")),
        "access",
        "refresh",
        Instant.EPOCH
    );

    private final BrowserRequests browser = new BrowserRequests();

    @Test
    void aSignInInProgressIsTakenOnce() {
        SignInTransaction begun = BrowserSessions.begin(browser.request());

        assertThat(BrowserSessions.takeTransaction(browser.request())).contains(begun);
        assertThat(BrowserSessions.takeTransaction(browser.request())).isEmpty();
    }

    @Test
    void aBrowserWithoutASessionHasNoTransactionAndNoSession() {
        assertThat(BrowserSessions.takeTransaction(browser.request())).isEmpty();
        assertThat(BrowserSessions.current(browser.request())).isEmpty();
        assertThat(browser.hasSession()).isFalse();
    }

    @Test
    void establishingASessionStartsAFreshOneHoldingIt() {
        BrowserSessions.begin(browser.request());

        BrowserSessions.establish(browser.request(), SESSION);

        assertThat(browser.invalidations()).isEqualTo(1);
        assertThat(BrowserSessions.current(browser.request())).contains(SESSION);
        assertThat(BrowserSessions.takeTransaction(browser.request())).isEmpty();
    }

    @Test
    void aRefreshedSessionReplacesTheOldOneAndEndingItForgetsBoth() {
        BrowserSessions.establish(browser.request(), SESSION);
        AuthSession refreshed = new AuthSession(SESSION.identity(), "access-2", "refresh-2", Instant.EPOCH);

        BrowserSessions.replace(browser.request(), refreshed);
        assertThat(BrowserSessions.current(browser.request())).contains(refreshed);

        BrowserSessions.end(browser.request());
        assertThat(BrowserSessions.current(browser.request())).isEmpty();
        assertThat(browser.hasSession()).isFalse();
    }

    @Test
    void replacingOrEndingWithoutASessionDoesNothing() {
        BrowserSessions.replace(browser.request(), SESSION);
        BrowserSessions.end(browser.request());

        assertThat(browser.hasSession()).isFalse();
        assertThat(browser.invalidations()).isZero();
    }
}
