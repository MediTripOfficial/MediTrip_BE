package com.meditrip.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meditrip.user.domain.entity.enums.UserStatus;
import com.meditrip.user.domain.repository.UserRepository;
import com.meditrip.user.infra.redis.RedisTokenStore;
import com.meditrip.user.presentation.dto.request.LoginRequest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

class AuthV1ControllerLoginTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTokenStore redisTokenStore;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("유저가 존재하고 ACTIVE 상태이며, 비밀번호가 일치하면 로그인에 성공한다.")
    @Test
    void shouldSucceedLogin_whenUserExistsAndIsActiveAndPasswordMatches() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String password = "password1234!!";
        String encodePassword = passwordEncoder.encode(password);

        saveUser(userId, email, encodePassword, UserStatus.ACTIVE);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String json = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        String responseRefreshToken = com.jayway.jsonpath.JsonPath.read(json, "$.refreshToken");

        String storedRefreshToken = redisTokenStore.findByUserId(userId);
        assertThat(storedRefreshToken).isNotNull();
        assertThat(storedRefreshToken).isEqualTo(responseRefreshToken);
    }

    @DisplayName("유저가 존재하지만 WITHDRAWN 상태면 로그인에 실패하고 401을 반환한다.")
    @Test
    void shouldReturn401_whenUserExistsButStatusIsWithdrawn() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String password = "password1234!!";
        String encodePassword = passwordEncoder.encode(password);

        saveUser(userId, email, encodePassword, UserStatus.WITHDRAWN);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Incorrect email or password."));
    }

    @DisplayName("유저가 존재하지만 DELETED 상태면 로그인에 실패하고 401을 반환한다.")
    @Test
    void shouldReturn401_whenUserExistsButStatusIsDeleted() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String password = "password1234!!";
        String encodePassword = passwordEncoder.encode(password);

        saveUser(userId, email, encodePassword, UserStatus.DELETED);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Incorrect email or password."));
    }

    @DisplayName("유저가 존재하지 않으면 로그인에 실패하고 401을 반환한다.")
    @Test
    void shouldReturn401_whenUserDoesNotExist() throws Exception {
        //given
        String email = "test@test.com";
        String password = "password1234!!";

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Incorrect email or password."));
    }

    @DisplayName("유저가 존재하고 ACTIVE 상태지만, 비밀번호가 일치하지 않으면 로그인에 실패하고 401을 반환한다.")
    @Test
    void shouldReturn401_whenUserExistsAndIsActiveButPasswordDoesNotMatch() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String password = "password1234!!";
        String encodePassword = passwordEncoder.encode("differentPassword1234!!");

        saveUser(userId, email, encodePassword, UserStatus.ACTIVE);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Incorrect email or password."));
    }

    @DisplayName("이메일을 입력하지 않고 로그인을 시도하면 400을 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenLoginWithoutEmail() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String password = "password1234!!";
        String encodePassword = "testest1234";

        saveUser(userId, email, encodePassword, UserStatus.ACTIVE);

        LoginRequest request = LoginRequest.builder()
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is required."));

    }

    @DisplayName("비밀번호를 입력하지 않고 로그인을 시도하면 400을 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenLoginWithoutPassword() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String encodePassword = "testest1234";

        saveUser(userId, email, encodePassword, UserStatus.ACTIVE);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password is required."));

    }

    @DisplayName("이메일 형식이 정규식에 맞지 않으면 로그인에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenEmailFormatIsInvalid() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "invalid-email-format";
        String password = "password1234!!";
        String encodePassword = "testest1234";

        saveUser(userId, email, encodePassword, UserStatus.ACTIVE);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please enter a valid email address."));
    }

    @DisplayName("비밀번호가 요구된 글자수에 맞지 않으면 로그인에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenPasswordDoesNotMatchRequirements() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String password = "!Pw12";
        String encodePassword = "testest1234";

        saveUser(userId, email, encodePassword, UserStatus.ACTIVE);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be between 8 and 20 characters."));
    }

    @DisplayName("비밀번호가 요구조건(영문+숫자+특수문자)에 맞지 않으면 로그인에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenPasswordDoesNotMatchRequirements2() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        String password = "testtest123";
        String encodePassword = "testest1234";

        saveUser(userId, email, encodePassword, UserStatus.ACTIVE);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must include letters, numbers, and special characters."));
    }

    private void saveUser(UUID userId, String email, String encodedPassword, UserStatus userStatus) {
        userRepository.save(createUser(userId, email, encodedPassword, userStatus));
    }

}
