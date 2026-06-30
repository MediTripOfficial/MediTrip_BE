package com.meditrip.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meditrip.common.domain.UserStatus;
import com.meditrip.common.jwt.JwtProperties;
import com.meditrip.common.jwt.JwtProvider;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.repository.UserRepository;
import com.meditrip.user.presentation.dto.request.WithdrawnRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserV1ControllerWithdrawnTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @DisplayName("유저가 ACTIVE 상태거나 GUEST 상태이고 비밀번호가 일치하면 탈퇴에 성공한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldWithdrawSuccessfully_whenUserIsActiveOrGuestAndPasswordMatches(UserStatus status) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode("password1234!");

        User user = createUser(userId, "email@test.com", encodedPassword, status);

        userRepository.save(user);

        WithdrawnRequest request = new WithdrawnRequest("password1234!");

        String token = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        //then
        User after = userRepository.findById(userId).orElseThrow();

        assertThat(after.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
    }

    @DisplayName("유저가 ACTIVE 상태거나 GUEST 상태지만 비밀번호가 일치하지 않으면 400을 반환하고, 탈퇴에 실패한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldReturn400BadRequest_whenPasswordDoesNotMatchDuringWithdrawal(UserStatus status) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode("password1234!");

        User user = createUser(userId, "email@test.com", encodedPassword, status);

        userRepository.save(user);

        WithdrawnRequest request = new WithdrawnRequest("asdfqwer1234!");

        String token = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passwords do not match"));

        User after = userRepository.findById(userId)
                .orElseThrow();

        assertThat(after.getStatus()).isEqualTo(status);
    }

    @DisplayName("유저가 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404NotFound_whenUserDoesNotExistDuringWithdrawal() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        WithdrawnRequest request = new WithdrawnRequest("asdfqwer1234!");

        String token = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User Not Found"));
    }

    @DisplayName("유저가 WITHDRAWN 상태거나 DELETED 상태면 404를 반환하고, 탈퇴에 실패한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldReturn404NotFound_whenUserIsAlreadyWithdrawnOrDeletedDuringWithdrawal(UserStatus status)
            throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode("password1234!");

        User user = createUser(userId, "email@test.com", encodedPassword, status);

        userRepository.save(user);

        WithdrawnRequest request = new WithdrawnRequest("password1234!");

        String token = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User Not Found"));

        User after = userRepository.findById(userId)
                .orElseThrow();

        assertThat(after.getStatus()).isEqualTo(status);
    }

    @DisplayName("토큰이 존재하지 않으면 401을 반환하고, 유저 탈퇴에 실패한다.")
    @Test
    void shouldReturn401Unauthorized_whenTokenDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode("password1234!");
        User user = createUser(userId, "email@test.com", encodedPassword, UserStatus.ACTIVE);
        userRepository.save(user);

        //when
        mockMvc.perform(delete("/api/v1/users/me"))
                //then
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("토큰이 만료되었으면 401을 반환하고, 유저 탈퇴에 실패한다.")
    @Test
    void shouldReturn401Unauthorized_whenTokenIsExpired() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode("password1234!");
        User user = createUser(userId, "email@test.com", encodedPassword, UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .claim(jwtProperties.tokenTypeClaim(), jwtProperties.accessTokenType())
                .expiration(new Date(System.currentTimeMillis() - jwtProperties.accessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();

        //when
        mockMvc.perform(delete("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

}
