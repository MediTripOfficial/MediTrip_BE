package com.meditrip.user.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meditrip.common.domain.UserStatus;
import com.meditrip.user.domain.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

class UserV1ControllerEmailDuplicationTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("이메일 중복 검사시 이메일이 존재하지 않으면 200을 반환한다.")
    @Test
    void shouldReturn200Ok_whenEmailDoesNotExistDuringDuplicateCheck() throws Exception {
        //when, then
        mockMvc.perform(get("/api/v1/users/email/check")
                        .param("email", "test@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("사용 가능"));
    }

    @DisplayName("이메일 중복 검사 시 가입된 이메일이 존재하고 ACTIVE, GUEST 상태라면 409를 반환한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldReturn409Conflict_whenEmailExistsWithActiveOrRestrictedStatus(UserStatus status) throws Exception {
        //given
        String email = "test@test.com";
        userRepository.save(createUser(UUID.randomUUID(), email, status));

        //when, then
        mockMvc.perform(get("/api/v1/users/email/check")
                        .param("email", email))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already exists."));
    }

    @DisplayName("이메일 중복 검사 시 가입된 이메일이 존재하지만 DELETED, WITHDRAWN 상태라면 200을 반환한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"DELETED", "WITHDRAWN"})
    void shouldReturn200Ok_whenEmailExistsButStatusIsDeletedOrWithdrawn(UserStatus status) throws Exception {
        //given
        String email = "test@test.com";
        userRepository.save(createUser(UUID.randomUUID(), email, status));

        //when, then
        mockMvc.perform(get("/api/v1/users/email/check")
                        .param("email", "test@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("사용 가능"));
    }

    @DisplayName("이메일이 null이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenEmailIsNull() throws Exception {
        //when, then
        mockMvc.perform(get("/api/v1/users/email/check"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Email is required."));
    }

}
