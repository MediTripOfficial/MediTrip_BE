package com.meditrip.common.constant;

public class PublicPaths {

    private PublicPaths() {}

    public static final String[] PERMIT_ALL = {
            "/api/v1/auth/signup/local",
            "/api/v1/auth/login",
            "/api/v1/auth/login/google",
            "/api/v1/auth/refresh",
            "/api/v1/users/email/verification",
            "/api/v1/users/email/check",
            "/api/v1/users/nickname/check",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

}
