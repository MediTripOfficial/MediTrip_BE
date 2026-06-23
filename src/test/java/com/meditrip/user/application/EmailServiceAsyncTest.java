package com.meditrip.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class EmailServiceAsyncTest {

    @Autowired
    private EmailService emailService;

    @MockitoBean
    private EmailSender emailSender;

    @MockitoBean
    private EmailAuthCodeStore emailAuthCodeStore;

    @DisplayName("이메일 발송이 느려도 호출자는 기다리지 않고 즉시 반환받는다.")
    @Test
    void shouldReturnImmediately_whenEmailSendingIsSlow() {
        //given
        doAnswer(invocation -> {
            Thread.sleep(2000);
            return null;
        }).when(emailSender).send(any(), any());

        //when
        long start = System.currentTimeMillis();
        emailService.sendVerifyEmail("test@example.com");
        long elapsed = System.currentTimeMillis() - start;

        //then
        assertThat(elapsed).isLessThan(500);

        await()
                .atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(emailSender, times(1)).send(any(), any()));
    }

    @DisplayName("비동기 발송에 실패하면 별도 스레드에서 저장된 인증 코드가 삭제된다.")
    @Test
    void shouldDeleteSavedAuthCode_whenAsyncSendingFails() {
        //given
        doAnswer(invocation -> {
            throw new RuntimeException("메일 서버 장애");
        }).when(emailSender).send(any(), any());

        //when
        emailService.sendVerifyEmail("test@example.com");

        //then
        await()
                .atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(emailAuthCodeStore, times(1)).deleteByEmail(any()));
    }

}
