package com.meditrip.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailServiceAsyncTest {

    @Mock
    private EmailAuthCodeStore emailAuthCodeStore;

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private EmailAsyncSender emailAsyncSender;

    private final String testEmail = "test@example.com";

    @DisplayName("인증 코드를 생성하고, 저장소에 저장한 뒤 이메일로 발송한다.")
    @Test
    void shouldSaveAndSendSameAuthCode_whenSendingSucceeds() {
        //given, when
        emailAsyncSender.send(testEmail);

        //then
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailAuthCodeStore, times(1)).save(eq(testEmail), codeCaptor.capture());
        verify(emailSender, times(1)).send(eq(testEmail), codeCaptor.capture());

        String savedCode = codeCaptor.getAllValues().get(0);
        String sentCode = codeCaptor.getAllValues().get(1);
        assertThat(savedCode).isEqualTo(sentCode);
    }

    @DisplayName("생성된 인증 코드는 6자리이며 영문 대문자와 숫자로만 구성된다.")
    @Test
    void shouldGenerateSixDigitAlphanumericCode() {
        //given, when
        emailAsyncSender.send(testEmail);

        //then
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(testEmail), codeCaptor.capture());
        String authCode = codeCaptor.getValue();

        assertThat(authCode).hasSize(6);
        assertThat(authCode).matches("^[0-9A-Z]{6}$");
    }

    @DisplayName("호출할 때마다 다른 인증 코드가 생성된다.")
    @Test
    void shouldGenerateDifferentCode_whenCalledMultipleTimes() {
        //given, when
        emailAsyncSender.send(testEmail);
        emailAsyncSender.send(testEmail);

        //then
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender, times(2)).send(eq(testEmail), codeCaptor.capture());

        String firstCode = codeCaptor.getAllValues().get(0);
        String secondCode = codeCaptor.getAllValues().get(1);
        assertThat(firstCode).isNotEqualTo(secondCode);
    }

    @DisplayName("이메일 발송에 실패하면 예외를 던지지 않고 저장된 인증 코드를 삭제한다.")
    @Test
    void shouldDeleteSavedAuthCode_whenSendingFails() {
        //given
        willThrow(new RuntimeException("메일 서버 장애")).given(emailSender).send(any(), any());

        //when, then
        assertDoesNotThrow(() -> emailAsyncSender.send(testEmail));

        verify(emailAuthCodeStore, times(1)).save(eq(testEmail), any());
        verify(emailAuthCodeStore, times(1)).deleteByEmail(eq(testEmail));
    }

    @DisplayName("이메일 발송에 성공하면 인증 코드를 삭제하지 않는다.")
    @Test
    void shouldNotDeleteAuthCode_whenSendingSucceeds() {
        //given, when
        emailAsyncSender.send(testEmail);

        //then
        verify(emailAuthCodeStore, never()).deleteByEmail(any());
    }
}
