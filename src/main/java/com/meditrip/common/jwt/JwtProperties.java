package com.meditrip.common.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiration,
        long refreshTokenExpiration,
        String tokenTypeClaim,
        String accessTokenType,
        String refreshTokenType,
        String redisPrefixRefresh
) {}
