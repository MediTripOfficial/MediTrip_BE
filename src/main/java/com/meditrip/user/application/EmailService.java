package com.meditrip.user.application;

import com.meditrip.common.util.SecurityUtils;
import com.meditrip.user.application.dto.request.VerifyEmailApplicationRequest;
import com.meditrip.user.application.dto.response.VerifyEmailResponse;
import java.security.SecureRandom;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final EmailAuthCodeStore emailAuthCodeStore;
    private final EmailSender emailSender;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Async("emailExecutor")
    public void sendVerifyEmail(String email) {
        String authCode = generateAuthCode();
        emailAuthCodeStore.save(email, authCode);
        try {
            emailSender.send(email, authCode);
        } catch (Exception e) {
            log.error("이메일 발송 실패 : {}", SecurityUtils.convertToMaskedEmail(email), e);
            emailAuthCodeStore.deleteByEmail(email);
        }
    }

    private String generateAuthCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (SECURE_RANDOM.nextBoolean()) {
                sb.append((char) (SECURE_RANDOM.nextInt(10) + '0'));
            } else {
                sb.append((char) (SECURE_RANDOM.nextInt(26) + 'A'));
            }
        }
        return sb.toString();
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
