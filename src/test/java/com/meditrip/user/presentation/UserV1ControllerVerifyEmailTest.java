package com.meditrip.user.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meditrip.user.application.EmailService;
import com.meditrip.user.application.dto.response.VerifyEmailResponse;
import com.meditrip.user.presentation.dto.request.VerifyEmailRequest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class UserV1ControllerVerifyEmailTest extends ControllerTestSupport {

    @MockitoBean
    private EmailService emailService;

    @DisplayName("올바른 인증 코드를 입력하면 이메일 인증에 성공한다.")
    @Test
    void shouldSucceedVerification_whenAuthCodeIsValid() throws Exception {
        //given
        UUID verifiedToken = UUID.randomUUID();

        VerifyEmailRequest request = new VerifyEmailRequest("test@test.com", "ABC123");

        given(emailService.verifyEmail(any())).willReturn(new VerifyEmailResponse(verifiedToken.toString()));

        //when, then
        mockMvc.perform(patch("/api/v1/users/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verifiedToken").value(verifiedToken.toString()));
    }

    @DisplayName("인증 코드가 만료되면 이메일 인증에 실패한다.")
    @Test
    void shouldFailVerification_whenAuthCodeExpired() throws Exception {
        //given
        VerifyEmailRequest request = new VerifyEmailRequest("test@test.com", "ABC123");
        doThrow(new IllegalArgumentException("The verification code has expired."))
                .when(emailService).verifyEmail(any());

        //when, then
        mockMvc.perform(patch("/api/v1/users/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The verification code has expired."));
    }

    @DisplayName("인증 코드가 일치하지 않으면 이메일 인증에 실패한다.")
    @Test
    void shouldFailVerification_whenAuthCodeInvalid() throws Exception {
        //given
        VerifyEmailRequest request = new VerifyEmailRequest("test@test.com", "WRONG1");
        doThrow(new IllegalArgumentException("Invalid verification code."))
                .when(emailService).verifyEmail(any());

        //when, then
        mockMvc.perform(patch("/api/v1/users/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid verification code."));
    }

}
