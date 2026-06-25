package com.meditrip.user.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.common.exception.TooManyRequestsException;
import com.meditrip.common.util.RateLimiter;
import com.meditrip.user.application.dto.request.VerifyEmailApplicationRequest;
import com.meditrip.user.domain.exception.EmailSendRateLimitException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailAuthCodeStore emailAuthCodeStore;

    @Mock
    private EmailAsyncSender emailAsyncSender;

    @Mock
    private RateLimiter rateLimiter;

    @InjectMocks
    private EmailService emailService;

    private final String testEmail = "test@example.com";
    private final String testIp = "1.2.3.4";

    @DisplayName("IP, 이메일 쿨다운 모두 통과하면 비동기 발송을 호출한다.")
    @Test
    void shouldDispatchAsyncSend_whenRateLimitsPass() {
        //given
        given(rateLimiter.tryAcquire(any(), anyInt(), any(Duration.class))).willReturn(true);
        given(emailAuthCodeStore.tryAcquireSendCooldown(testEmail)).willReturn(true);

        //when
        emailService.sendVerifyEmail(testEmail, testIp);

        //then
        verify(emailAsyncSender, times(1)).send(eq(testEmail));
    }

    @DisplayName("IP 요청 횟수 제한에 걸리면 예외를 던지고 이메일 쿨다운은 체크하지 않는다.")
    @Test
    void shouldThrowTooManyRequests_whenIpRateLimitExceeded() {
        //given
        given(rateLimiter.tryAcquire(any(), anyInt(), any(Duration.class))).willReturn(false);

        //when, then
        assertThatThrownBy(() -> emailService.sendVerifyEmail(testEmail, testIp))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessage("Too many requests. Please try again later.");

        verify(emailAuthCodeStore, never()).tryAcquireSendCooldown(any());
        verify(emailAsyncSender, never()).send(any());
    }

    @DisplayName("IP 제한은 통과했지만 이메일 쿨다운 중이면 예외를 던지고 발송하지 않는다.")
    @Test
    void shouldThrowEmailSendRateLimit_whenEmailCooldownActive() {
        //given
        given(rateLimiter.tryAcquire(any(), anyInt(), any(Duration.class))).willReturn(true);
        given(emailAuthCodeStore.tryAcquireSendCooldown(testEmail)).willReturn(false);

        //when, then
        assertThatThrownBy(() -> emailService.sendVerifyEmail(testEmail, testIp))
                .isInstanceOf(EmailSendRateLimitException.class)
                .hasMessage("Please wait before requesting another code.");

        verify(emailAsyncSender, never()).send(any());
    }

    @DisplayName("IP별로 사용한 레이트리밋 키에 해당 IP가 포함된다.")
    @Test
    void shouldUseClientIpInRateLimitKey() {
        //given
        given(rateLimiter.tryAcquire(any(), anyInt(), any(Duration.class))).willReturn(true);
        given(emailAuthCodeStore.tryAcquireSendCooldown(testEmail)).willReturn(true);

        //when
        emailService.sendVerifyEmail(testEmail, testIp);

        //then
        verify(rateLimiter, times(1)).tryAcquire(
                eq("mediTrip:rateLimit:email:ip:" + testIp), eq(5), eq(Duration.ofMinutes(1)));
    }

    @DisplayName("인증 코드가 일치하면 이메일 인증에 성공한다.")
    @Test
    void shouldSucceedVerification_whenAuthCodeMatches() {
        //given
        VerifyEmailApplicationRequest request = new VerifyEmailApplicationRequest("test@test.com", "ABC123");
        given(emailAuthCodeStore.findByEmail("test@test.com")).willReturn(Optional.of("ABC123"));

        //when, then
        assertThatNoException().isThrownBy(() -> emailService.verifyEmail(request));
        verify(emailAuthCodeStore).deleteByEmail("test@test.com");
    }

    @DisplayName("인증 코드가 불일치하면 이메일 인증에 실패한다.")
    @Test
    void shouldFailVerification_whenAuthCodeNotMatches() {
        //given
        VerifyEmailApplicationRequest request = new VerifyEmailApplicationRequest("test@test.com", "WRONG1");
        given(emailAuthCodeStore.findByEmail("test@test.com")).willReturn(Optional.of("ABC123"));

        //when, then
        assertThatThrownBy(() -> emailService.verifyEmail(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid verification code.");

        verify(emailAuthCodeStore, never()).deleteByEmail(any());
    }

    @DisplayName("인증 코드가 존재하지 않으면 이메일 인증에 실패한다.")
    @Test
    void shouldFailVerification_whenAuthCodeNotFound() {
        //given
        VerifyEmailApplicationRequest request = new VerifyEmailApplicationRequest("test@test.com", "ABC123");
        given(emailAuthCodeStore.findByEmail("test@test.com"))
                .willThrow(new IllegalArgumentException("Invalid verification code."));

        //when, then
        assertThatThrownBy(() -> emailService.verifyEmail(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid verification code.");
    }

}
