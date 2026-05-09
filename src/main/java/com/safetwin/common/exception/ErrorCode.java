package com.safetwin.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    // Site / Worker / Risk
    SITE_NOT_FOUND(HttpStatus.NOT_FOUND, "사업장을 찾을 수 없습니다."),
    WORKER_NOT_FOUND(HttpStatus.NOT_FOUND, "근로자를 찾을 수 없습니다."),
    RISK_NOT_FOUND(HttpStatus.NOT_FOUND, "위험 요소를 찾을 수 없습니다."),

    // Common
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INVALID_BIZ_NUMBER(HttpStatus.BAD_REQUEST, "유효하지 않은 사업자 번호입니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 허용 한도를 초과했습니다."),
    ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "분석 처리 중 오류가 발생했습니다."),
    PUBLIC_API_ERROR(HttpStatus.BAD_GATEWAY, "공공 API 호출 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
