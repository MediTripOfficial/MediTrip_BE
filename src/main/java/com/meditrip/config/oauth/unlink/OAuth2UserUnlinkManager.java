package com.meditrip.config.oauth.unlink;

import com.meditrip.config.oauth.exception.OAuth2AuthenticationProcessingException;
import com.meditrip.config.oauth.user.OAuth2Provider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OAuth2UserUnlinkManager {

    private final GoogleUserUnlink googleUserUnlink;

    public void unlink(OAuth2Provider provider, String accessToken, String userEmail, HttpServletRequest request,
                       HttpServletResponse response) {
        if (OAuth2Provider.GOOGLE.equals(provider)) {
            googleUserUnlink.unlink(accessToken, userEmail, request, response);
        } else {
            throw new OAuth2AuthenticationProcessingException(
                    "Unlink with " + provider.getRegistrationId() + " is not supported");
        }
    }

}
