package com.vertyll.jakartaeeapi.common.exception;

import java.io.Serial;

import lombok.Getter;

@Getter
public abstract class BaseBusinessException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final Object[] NO_ARGS = {};

    private final String messageKey;

    private final transient Object[] args;

    protected BaseBusinessException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = NO_ARGS;
    }

    protected BaseBusinessException(String messageKey, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args.clone();
    }

    protected BaseBusinessException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
        this.args = NO_ARGS;
    }

    protected BaseBusinessException(String messageKey, Throwable cause, Object... args) {
        super(messageKey, cause);
        this.messageKey = messageKey;
        this.args = args.clone();
    }

    public Object[] getArgs() {
        return args.clone();
    }
}
