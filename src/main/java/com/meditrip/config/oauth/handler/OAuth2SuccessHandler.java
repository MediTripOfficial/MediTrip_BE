package com.meditrip.config.oauth.handler;

import com.meditrip.common.event.OAuth2LoginRequestEvent;
import com.meditrip.common.event.OAuth2SaveTokenEvent;
import com.meditrip.common.event.OAuth2UnlinkRequestEvent;
import com.meditrip.common.jwt.JwtProvider;
import com.meditrip.config.oauth.OAuth2UserPrincipal;
import com.meditrip.config.oauth.OAuthCookieRepository;
import com.meditrip.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final OAuthCookieRepository cookieRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.oauth2.default-redirect-uri:http://localhost:3000/oauth/callback}")
    private String defaultRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);
        if (response.isCommitted()) {
            return;
        }
        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        String redirectUri = CookieUtil.getCookie(request, OAuthCookieRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(defaultRedirectUri);

        String mode = CookieUtil.getCookie(request, OAuthCookieRepository.MODE_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse("login");

        OAuth2UserPrincipal principal = extractPrincipal(authentication);
        if (principal == null) {
            log.error("OAuth2 principal을 찾을 수 없습니다.");
            return buildErrorUrl(redirectUri, "login_failed");
        }

        if ("login".equalsIgnoreCase(mode)) {
            return handleLogin(request, response, redirectUri, principal);
        } else if ("unlink".equalsIgnoreCase(mode)) {
            return handleUnlink(request, response, redirectUri, principal);
        }

        return buildErrorUrl(redirectUri, "unknown_mode");
    }

    private String handleLogin(HttpServletRequest request, HttpServletResponse response,
                               String redirectUri, OAuth2UserPrincipal principal) {
        OAuth2LoginRequestEvent loginEvent = new OAuth2LoginRequestEvent(
                principal.getUserInfo().getEmail(),
                principal.getUserInfo().getName(),
                principal.getUserInfo().getProvider()
        );
        eventPublisher.publishEvent(loginEvent);

        if (!loginEvent.isHandled()) {
            log.error("OAuth2 로그인 이벤트가 처리되지 않았습니다. email: [{}]", principal.getUserInfo().getEmail());
            return buildErrorUrl(redirectUri, "Login processing failed");
        }

        String userId = loginEvent.getUserId();
        String userStatus = loginEvent.getUserStatus();

        String accessToken = jwtProvider.generateAccessToken(userId);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        OAuth2SaveTokenEvent saveTokenEvent = new OAuth2SaveTokenEvent(userId, refreshToken);
        eventPublisher.publishEvent(saveTokenEvent);

        CookieUtil.addSecureCookie(response, "refresh_token", refreshToken, (int) Duration.ofDays(14).toSeconds());

        log.info("OAuth2 로그인 성공. userId: [{}], userStatus: [{}]", userId, userStatus);

        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("access_token", accessToken)
                .queryParam("status", userStatus)
                .build().toUriString();
    }

    private String handleUnlink(HttpServletRequest request, HttpServletResponse response,
                                String redirectUri, OAuth2UserPrincipal principal) {
        OAuth2UnlinkRequestEvent unlinkEvent = new OAuth2UnlinkRequestEvent(principal.getUserInfo().getEmail());
        eventPublisher.publishEvent(unlinkEvent);

        CookieUtil.deleteCookie(request, response, "access_token");
        CookieUtil.deleteCookie(request, response, "refresh_token");
        log.info("OAuth2 연동 해제 완료. email: [{}]", principal.getUserInfo().getEmail());

        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("status", "unlinked")
                .build().toUriString();
    }

    private OAuth2UserPrincipal extractPrincipal(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2UserPrincipal p) {
            return p;
        }
        return null;
    }

    private String buildErrorUrl(String targetUrl, String message) {
        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", message)
                .build().toUriString();
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        cookieRepository.removeAuthorizationRequestCookies(request, response);
    }

}
