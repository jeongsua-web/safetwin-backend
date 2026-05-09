package com.safetwin.common.exception;

import lombok.Getter;

@Getter
public class SafeTwinException extends RuntimeException {

    private final ErrorCode errorCode;

    public SafeTwinException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
