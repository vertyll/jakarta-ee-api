package com.vertyll.jakartaeeapi.auth.infrastructure.web;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.jspecify.annotations.Nullable;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A browser's request whose HTTP session lives in a map, created on demand and ended by {@code invalidate()}. */
final class BrowserRequests {
    private final Map<String, Object> attributes = new HashMap<>();
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private @Nullable HttpSession session;
    private int invalidations;

    BrowserRequests() {
        when(request.getSession(anyBoolean())).thenAnswer(call -> {
            boolean create = call.getArgument(0);
            if (session == null && create) {
                session = newSession();
            }
            return session;
        });
    }

    HttpServletRequest request() {
        return request;
    }

    @Nullable
    Object attribute(String name) {
        return attributes.get(name);
    }

    boolean hasSession() {
        return session != null;
    }

    int invalidations() {
        return invalidations;
    }

    private HttpSession newSession() {
        HttpSession created = mock(HttpSession.class);
        when(created.getAttribute(anyString())).thenAnswer(call -> attributes.get(call.<String>getArgument(0)));
        doAnswer(call -> attributes.put(call.getArgument(0), call.getArgument(1))).when(created)
            .setAttribute(anyString(), org.mockito.ArgumentMatchers.any());
        doAnswer(call -> attributes.remove(call.<String>getArgument(0))).when(created).removeAttribute(anyString());
        doAnswer(call -> {
            attributes.clear();
            session = null;
            invalidations++;
            return null;
        }).when(created).invalidate();
        return created;
    }
}
