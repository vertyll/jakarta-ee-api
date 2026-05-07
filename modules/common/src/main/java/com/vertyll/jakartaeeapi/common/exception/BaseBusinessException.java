package com.vertyll.jakartaeeapi.common.exception;

import java.io.Serial;

import org.jspecify.annotations.Nullable;

import lombok.Getter;

@Getter
public abstract class BaseBusinessException extends RuntimeException {
    @Serial private static final long serialVersionUID = 1L;

    private final String messageKey;

    @Nullable private final transient Object[] args;

    protected BaseBusinessException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = null;
    }

    protected BaseBusinessException(String messageKey, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args != null ? args.clone() : null;
    }

    protected BaseBusinessException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
        this.args = null;
    }

    protected BaseBusinessException(String messageKey, Throwable cause, Object... args) {
        super(messageKey, cause);
        this.messageKey = messageKey;
        this.args = args != null ? args.clone() : null;
    }
}
