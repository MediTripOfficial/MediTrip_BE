package com.meditrip.config.oauth.unlink;

import com.meditrip.config.oauth.exception.OAuth2AuthenticationProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
@Component
@Slf4j
public class GoogleUserUnlink implements OAuth2UserUnlink {

    private static final String GOOGLE_REVOKE_URL = "https://oauth2.googleapis.com/revoke";

    private final RestTemplate restTemplate;

    @Override
    public void unlink(String accessToken, String userEmail, HttpServletRequest request, HttpServletResponse response) {
        try {
            String url = GOOGLE_REVOKE_URL + "?token=" + accessToken;
            String result = restTemplate.postForObject(url, null, String.class);
            log.info("Google token revoke succeeded for user: {}", userEmail);
        } catch (RestClientException e) {
            log.error("Failed to revoke Google token for user: [{}]", userEmail, e);
            throw new OAuth2AuthenticationProcessingException("Google 계정 연결 해제 중 오류 발생", e);
        }
    }

}
