package com.vertyll.jakartaeeapi.auth;

import java.util.Set;

import org.jspecify.annotations.Nullable;

public final class FetchMetadata {
    public static final String FETCH_SITE_HEADER = "Sec-Fetch-Site";

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final Set<String> TRUSTED_FETCH_SITES = Set.of("same-origin", "none");

    private FetchMetadata() {
    }

    public static boolean sentFromThisOrigin(String method, @Nullable String fetchSite) {
        return SAFE_METHODS.contains(method) || fetchSite == null || TRUSTED_FETCH_SITES.contains(fetchSite);
    }
}
