package com.meditrip.user.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meditrip.common.domain.UserStatus;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.entity.enums.Gender;
import com.meditrip.user.domain.entity.enums.Provider;
import com.meditrip.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

class UserV1ControllerNicknameDuplicationTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("닉네임 중복 검사시 이메일이 존재하지 않으면 200을 반환한다.")
    @Test
    void shouldReturn200Ok_whenNicknameDoesNotExistDuringDuplicateCheck() throws Exception {
        //when, then
        mockMvc.perform(get("/api/v1/users/nickname/check")
                        .param("nickname", "하이"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("사용 가능"));
    }

    @DisplayName("닉네임 중복 검사 시 가입된 닉네임이 존재하고 ACTIVE, GUEST 상태라면 409를 반환한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldReturn409Conflict_whenNicknameExistsWithActiveOrRestrictedStatus(UserStatus status) throws Exception {
        //given
        String nickname = "하잉";
        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email("email@test.com")
                .password("password")
                .name("테스트 유저")
                .nickname(nickname)
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .provider(Provider.LOCAL)
                .isMarketingTermsAgreed(true)
                .status(status)
                .build());

        //when, then
        mockMvc.perform(get("/api/v1/users/nickname/check")
                        .param("nickname", nickname))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Nickname already exists."));
    }

    @DisplayName("이메일 중복 검사 시 가입된 이메일이 존재하지만 DELETED, WITHDRAWN 상태라면 200을 반환한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"DELETED", "WITHDRAWN"})
    void shouldReturn200Ok_whenNicknameExistsButStatusIsDeletedOrWithdrawn(UserStatus status) throws Exception {
        //given
        String nickname = "하잉";
        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email("email@test.com")
                .password("password")
                .name("테스트 유저")
                .nickname(nickname)
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .provider(Provider.LOCAL)
                .isMarketingTermsAgreed(true)
                .status(status)
                .build());

        //when, then
        mockMvc.perform(get("/api/v1/users/nickname/check")
                        .param("nickname", nickname))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("사용 가능"));
    }

    @DisplayName("닉네임이 null이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenNicknameIsNull() throws Exception {
        //when, then
        mockMvc.perform(get("/api/v1/users/nickname/check"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Nickname is required."));
    }

}
