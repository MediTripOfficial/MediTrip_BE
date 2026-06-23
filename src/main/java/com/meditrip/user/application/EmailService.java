package com.meditrip.user.application;

import com.meditrip.common.exception.TooManyRequestsException;
import com.meditrip.common.util.RateLimiter;
import com.meditrip.common.util.SecurityUtils;
import com.meditrip.user.application.dto.request.VerifyEmailApplicationRequest;
import com.meditrip.user.application.dto.response.VerifyEmailResponse;
import com.meditrip.user.domain.exception.EmailSendRateLimitException;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final EmailAuthCodeStore emailAuthCodeStore;
    private final EmailAsyncSender emailAsyncSender;
    private final RateLimiter rateLimiter;

    private static final String IP_RATE_LIMIT_PREFIX = "mediTrip:rateLimit:email:ip:";
    private static final int IP_MAX_REQUEST = 5;
    private static final Duration IP_WINDOW = Duration.ofMinutes(1);

    public void sendVerifyEmail(String email, String clientIp) {
        if (!rateLimiter.tryAcquire(IP_RATE_LIMIT_PREFIX + clientIp, IP_MAX_REQUEST, IP_WINDOW)) {
            log.warn("이메일 발송 IP 제한 초과. IP : [{}]", clientIp);
            throw new TooManyRequestsException("Too many requests. Please try again later.");
        }

        if (!emailAuthCodeStore.tryAcquireSendCooldown(email)) {
            log.info("이메일 발송 요청 제한. 이메일 : [{}]", SecurityUtils.convertToMaskedEmail(email));
            throw new EmailSendRateLimitException("Please wait before requesting another code.");
        }

        emailAsyncSender.send(email);
    }

    public VerifyEmailResponse verifyEmail(VerifyEmailApplicationRequest request) {
        String authCode = emailAuthCodeStore.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification code."));

        if (!request.getAuthCode().equals(authCode)) {
            throw new IllegalArgumentException("Invalid verification code.");
        }

        emailAuthCodeStore.deleteByEmail(request.getEmail());

        String verifyToken = UUID.randomUUID().toString();
        emailAuthCodeStore.saveVerifiedToken(request.getEmail(), verifyToken, 5);

        return new VerifyEmailResponse(verifyToken);
    }

}
