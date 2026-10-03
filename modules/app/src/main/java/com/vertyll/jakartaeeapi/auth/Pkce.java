package com.vertyll.jakartaeeapi.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class Pkce {
    public static final String CHALLENGE_METHOD = "S256";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final int STATE_BYTES = 32;
    private static final int VERIFIER_BYTES = 64;

    private Pkce() {
    }

    public static SignInTransaction newTransaction() {
        return new SignInTransaction(randomString(STATE_BYTES), randomString(VERIFIER_BYTES));
    }

    public static String challengeOf(String codeVerifier) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return ENCODER.encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }

    public static boolean sameState(String expected, String received) {
        return MessageDigest
            .isEqual(expected.getBytes(StandardCharsets.US_ASCII), received.getBytes(StandardCharsets.US_ASCII));
    }

    private static String randomString(int bytes) {
        byte[] random = new byte[bytes];
        RANDOM.nextBytes(random);
        return ENCODER.encodeToString(random);
    }
}
