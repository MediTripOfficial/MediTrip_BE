package com.meditrip.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meditrip.common.domain.UserStatus;
import com.meditrip.common.jwt.JwtProperties;
import com.meditrip.common.jwt.JwtProvider;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.repository.UserRepository;
import com.meditrip.user.presentation.dto.request.UpdatePasswordRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserV1ControllerUpdatePasswordTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    private static final String existingPassword = "password1234!";

    @DisplayName("유저 정보가 존재하고 GUEST, ACTIVE 상태이며 기존 비밀번호가 일치하면 비밀번호 변경에 성공한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldChangePasswordSuccessfully_whenUserExistsAndIsActiveOrGuestAndPasswordMatches(UserStatus userStatus)
            throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(createUser(userId, "email@test.com", passwordEncoder.encode(existingPassword), userStatus));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isOk());

        User user = userRepository.findById(userId)
                .orElseThrow();

        assertThat(passwordEncoder.matches(request.getNewPassword(), user.getPassword())).isTrue();
    }

    @DisplayName("유저 정보가 존재하고 GUEST, ACTIVE 상태지만 기존 비밀번호가 일치하지 않으면 400을 반환하고 비밀번호 변경에 실패한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldReturn400BadRequest_whenCurrentPasswordDoesNotMatchDuringPasswordChange(UserStatus userStatus)
            throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(createUser(userId, "email@test.com", userStatus));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword + "1234asdf")
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passwords do not match"));

        User user = userRepository.findById(userId)
                .orElseThrow();

        assertThat(passwordEncoder.matches(request.getNewPassword(), user.getPassword())).isFalse();
    }

    @DisplayName("유저 정보가 존재하지만 WITHDRAWN이거나 DELETED이면 404를 반환하고, 비밀번호 변경에 실패한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldReturn404NotFound_whenUserExistsButStatusIsWithdrawnOrDeleted(UserStatus userStatus) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(createUser(userId, "email@test.com", userStatus));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User Not Found"));
    }

    @DisplayName("유저 정보가 존재하지 않으면 404를 반환하고, 비밀번호 변경에 실패한다.")
    @Test
    void shouldReturn404NotFound_whenUserDoesNotExistDuringPasswordChange() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User Not Found"));
    }

    @DisplayName("토큰이 존재하지 않으면 401을 반환하고, 비밀번호 변경에 실패한다.")
    @Test
    void shouldReturn401Unauthorized_whenTokenDoesNotExistDuringPasswordChange() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(createUser(userId, "email@test.com", UserStatus.ACTIVE));

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("토큰이 만료되었으면 401을 반환하고, 비밀번호 변경에 실패한다.")
    @Test
    void shouldReturn401Unauthorized_whenTokenIsExpiredDuringPasswordChange() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(createUser(userId, "email@test.com", UserStatus.ACTIVE));
        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .claim(jwtProperties.tokenTypeClaim(), jwtProperties.accessTokenType())
                .expiration(new Date(System.currentTimeMillis() - jwtProperties.accessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @DisplayName("기존 비밀번호가 null이면 400을 반환하고, 비밀번호 변경에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenCurrentPasswordIsNullDuringPasswordChange() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(createUser(userId, "email@test.com", UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .newPassword("asdfqwer1234!")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Existing password is required."))
                .andReturn();
    }

    @DisplayName("변경할 비밀번호가 null이면 400을 반환하고, 비밀번호 변경에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenNewPasswordIsNullDuringPasswordChange() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(createUser(userId, "email@test.com", UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password is required."))
                .andReturn();
    }

    @DisplayName("변경할 비밀번호의 형식이 유효하지 않으면 400을 반환하고, 비밀번호 변경에 실패한다.")
    @Test
    void shouldReturn400BadRequest_whenNewPasswordFormatIsInvalidDuringPasswordChange() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(createUser(userId, "email@test.com", UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .existingPassword(existingPassword)
                .newPassword("asdf1234")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Password must include letters, numbers, and special characters."));
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

}
