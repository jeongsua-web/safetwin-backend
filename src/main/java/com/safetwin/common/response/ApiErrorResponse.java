package com.safetwin.common.response;

import com.safetwin.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class ApiErrorResponse {

    private final boolean success = false;
    private final String code;
    private final String message;

    private ApiErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ApiErrorResponse of(ErrorCode errorCode) {
        return new ApiErrorResponse(errorCode.name(), errorCode.getMessage());
    }

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(code, message);
    }
}
