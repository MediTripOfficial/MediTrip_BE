package com.meditrip.user.application;

import com.meditrip.common.util.SecurityUtils;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailAsyncSender {

    private final EmailAuthCodeStore emailAuthCodeStore;
    private final EmailSender emailSender;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Async("emailExecutor")
    public void send(String email) {
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

}
