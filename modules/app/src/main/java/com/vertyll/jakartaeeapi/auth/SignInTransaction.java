package com.vertyll.jakartaeeapi.auth;

import java.io.Serial;
import java.io.Serializable;

public record SignInTransaction(String state, String codeVerifier) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "SignInTransaction[state=***, codeVerifier=***]";
    }
}
