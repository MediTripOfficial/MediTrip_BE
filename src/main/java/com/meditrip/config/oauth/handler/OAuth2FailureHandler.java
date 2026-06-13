package com.meditrip.config.oauth.handler;

import com.meditrip.config.oauth.OAuthCookieRepository;
import com.meditrip.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@RequiredArgsConstructor
@Component
@Slf4j
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private final OAuthCookieRepository cookieRepository;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String targetUrl = CookieUtil.getCookie(request,
                        OAuthCookieRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse("/");

        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oAuth2Exception = (OAuth2AuthenticationException) exception;
            log.error("OAuth2 Error Code: {}", oAuth2Exception.getError().getErrorCode());
            log.error("OAuth2 Error Description: {}", oAuth2Exception.getError().getDescription());

            targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("error", "oauth2_authentication_failed")
                    .build().toUriString();
            cookieRepository.removeAuthorizationRequest(request, response);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

        log.error("Authentication failed. Exception message: {}", exception.getMessage());
        log.error("Exception type: {}", exception.getClass().getName());

        targetUrl = UriComponentsBuilder.fromUriString(targetUrl).build()
                .toUriString();

        cookieRepository.removeAuthorizationRequest(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

}
