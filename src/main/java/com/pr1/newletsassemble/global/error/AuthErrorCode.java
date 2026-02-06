package com.pr1.newletsassemble.global.error;

import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode{
    //인증 / 토큰
    AUTH_BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED,"Bad credentials"),
    AUTH_ACCESS_INVALID(HttpStatus.UNAUTHORIZED,"Invalid access token"),
    AUTH_ACCESS_EXPIRED(HttpStatus.UNAUTHORIZED,"Access token expired"),
    AUTH_REFRESH_INVALID(HttpStatus.UNAUTHORIZED,"Invalid refresh token"),
    AUTH_REFRESH_EXPIRED(HttpStatus.UNAUTHORIZED,"Refresh token expired"),
    AUTH_REFRESH_REUSE_DETECTED(HttpStatus.UNAUTHORIZED,"Refresh token reuse detected"),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED,"Invalid token"),

    // 세션 / 기기
    AUTH_DEVICE_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND,"Device session not found"),
    AUTH_DEVICE_SESSION_REVOKED(HttpStatus.UNAUTHORIZED,"Device session revoked"),
    AUTH_DEVICE_KEY_MISSING(HttpStatus.BAD_REQUEST,"Device key missing"),
    AUTH_DEVICE_KEY_INVALID(HttpStatus.BAD_REQUEST,"Invalid device key"),
    AUTH_DEVICE_KEY_MISMATCH(HttpStatus.FORBIDDEN,"Device key mismatch"),
    AUTH_DEVICE_KEY_MISMATCH_LOGOUT_ALL(HttpStatus.FORBIDDEN,"Device key mismatch threshold exceeded. All sessions revoked."),

    // 유저 상태 ( 인증 플로우에서 차단 되는 것 )
    AUTH_ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN,"Account suspended"),
    AUTH_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND,"Account not found"),
    AUTH_LOGIN_CONFLICT(HttpStatus.CONFLICT,"Login conflict. Please retry"),
    AUTH_LOGIN_IN_PROGRESS(HttpStatus.CONFLICT,"Login is being processed. Please retry"),
    AUTH_LOGIN_REDIS_UNAVAILABLE(HttpStatus.CONFLICT,"Login temporarily unavailable. Please retry"),
    // 인가
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN,"Forbidden"),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"Unauthorized");

    private final HttpStatus status;
    private final String message;
    AuthErrorCode(HttpStatus status, String message){
        this.status = status;
        this.message = message;
    }
    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }

}
