package com.meditrip.common.jwt;

import com.meditrip.common.exception.JwtAuthenticationException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .claim(jwtProperties.tokenTypeClaim(), jwtProperties.accessTokenType())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.accessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .claim(jwtProperties.tokenTypeClaim(), jwtProperties.refreshTokenType())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.refreshTokenExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    public String getUserId(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateAccessToken(String token) {
        try {
            String tokenType = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get(jwtProperties.tokenTypeClaim(), String.class);
            if (!jwtProperties.accessTokenType().equals(tokenType)) {
                throw new JwtAuthenticationException();
            }
            return true;
        } catch (JwtAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtAuthenticationException();
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            String tokenType = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get(jwtProperties.tokenTypeClaim(), String.class);

            if (!jwtProperties.refreshTokenType().equals(tokenType)) {
                throw new JwtAuthenticationException("Invalid refresh token.");
            }
            return true;
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException("Refresh token has expired.");
        } catch (Exception e) {
            throw new JwtAuthenticationException("Invalid refresh token.");
        }
    }

}
