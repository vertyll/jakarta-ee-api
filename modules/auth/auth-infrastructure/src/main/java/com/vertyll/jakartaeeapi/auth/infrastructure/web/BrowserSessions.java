package com.vertyll.jakartaeeapi.auth.infrastructure.web;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import com.vertyll.jakartaeeapi.auth.domain.AuthSession;
import com.vertyll.jakartaeeapi.auth.domain.Pkce;
import com.vertyll.jakartaeeapi.auth.domain.SignInTransaction;

/** What a browser's HTTP session holds: the sign-in in progress, then the signed-in session. */
public final class BrowserSessions {
    private static final String TRANSACTION = BrowserSessions.class.getName() + ".transaction";
    private static final String SESSION = BrowserSessions.class.getName() + ".session";

    private BrowserSessions() {
    }

    public static SignInTransaction begin(HttpServletRequest request) {
        SignInTransaction transaction = Pkce.newTransaction();
        request.getSession(true).setAttribute(TRANSACTION, transaction);
        return transaction;
    }

    public static Optional<SignInTransaction> takeTransaction(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(TRANSACTION) instanceof SignInTransaction transaction)) {
            return Optional.empty();
        }
        session.removeAttribute(TRANSACTION);
        return Optional.of(transaction);
    }

    public static void establish(HttpServletRequest request, AuthSession authSession) {
        end(request);
        request.getSession(true).setAttribute(SESSION, authSession);
    }

    public static Optional<AuthSession> current(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(SESSION) instanceof AuthSession authSession)) {
            return Optional.empty();
        }
        return Optional.of(authSession);
    }

    public static void replace(HttpServletRequest request, AuthSession authSession) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(SESSION, authSession);
        }
    }

    public static void end(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
