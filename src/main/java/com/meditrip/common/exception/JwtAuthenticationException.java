package com.meditrip.common.exception;

public class JwtAuthenticationException extends RuntimeException {
    public JwtAuthenticationException(String message) {
        super(message);
    }

    public JwtAuthenticationException() {
        super("올바르지 않은 토큰입니다.");
    }

}
