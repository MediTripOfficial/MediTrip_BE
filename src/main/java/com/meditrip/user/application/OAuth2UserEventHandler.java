package com.meditrip.user.application;

import com.meditrip.common.event.OAuth2LoginRequestEvent;
import com.meditrip.common.event.OAuth2SaveTokenEvent;
import com.meditrip.common.event.OAuth2UnlinkRequestEvent;
import com.meditrip.common.util.SecurityUtils;
import com.meditrip.config.oauth.user.OAuth2Provider;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.entity.enums.Provider;
import com.meditrip.user.domain.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserEventHandler {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    @EventListener
    @Transactional
    public void handleLogin(OAuth2LoginRequestEvent event) {
        String maskedEmail = SecurityUtils.convertToMaskedEmail(event.getEmail());
        userRepository.findByEmail(event.getEmail())
                .ifPresentOrElse(
                        user -> {
                            log.info("소셜 로그인 기존 유저. email: [{}]", maskedEmail);
                            event.setResult(user.getId().toString(), user.getStatus().name());
                        },
                        () -> {
                            log.info("소셜 로그인 신규 유저. email: [{}]", maskedEmail);
                            UUID userId = UUID.randomUUID();
                            Provider provider = resolveProvider(event.getProvider());
                            User newUser = User.oauthSignupUser(
                                    userId,
                                    event.getEmail(),
                                    event.getName(),
                                    provider
                            );
                            userRepository.save(newUser);
                            event.setResult(userId.toString(), newUser.getStatus().name());
                        }
                );
    }

    @EventListener
    @Transactional
    public void handleUnlink(OAuth2UnlinkRequestEvent event) {
        String maskedEmail = SecurityUtils.convertToMaskedEmail(event.getEmail());
        userRepository.findByEmail(event.getEmail()).ifPresent(user -> {
            tokenService.deleteRefreshToken(user.getId());
            user.withdrawn();
            log.info("소셜 연동 해제 완료. email: [{}]", maskedEmail);
            event.setResult(user.getId().toString());
        });
    }

    @EventListener
    public void handleSaveToken(OAuth2SaveTokenEvent event) {
        UUID userId = UUID.fromString(event.userId());
        tokenService.saveRefreshToken(userId, event.refreshToken());
        log.debug("OAuth2 Refresh Token 저장 완료. User Id: [{}]", userId);
    }

    private Provider resolveProvider(OAuth2Provider provider) {
        return switch (provider) {
            case GOOGLE -> Provider.GOOGLE;
        };
    }

}
