package com.meditrip.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meditrip.user.domain.entity.Allergy;
import com.meditrip.user.domain.entity.Condition;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.entity.enums.Gender;
import com.meditrip.user.domain.entity.enums.Provider;
import com.meditrip.user.domain.entity.enums.UserStatus;
import com.meditrip.user.domain.repository.AllergyRepository;
import com.meditrip.user.domain.repository.ConditionRepository;
import com.meditrip.user.domain.repository.UserAllergyRepository;
import com.meditrip.user.domain.repository.UserConditionRepository;
import com.meditrip.user.domain.repository.UserRepository;
import com.meditrip.user.presentation.dto.request.SignupRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class AuthV1ControllerSignupTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConditionRepository conditionRepository;

    @Autowired
    private AllergyRepository allergyRepository;

    @Autowired
    private UserConditionRepository userConditionRepository;

    @Autowired
    private UserAllergyRepository userAllergyRepository;

    @AfterEach
    void tearDown() {
        userConditionRepository.deleteAllInBatch();
        userAllergyRepository.deleteAllInBatch();
        conditionRepository.deleteAllInBatch();
        allergyRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("회원가입에 성공한다.")
    @Test
    void shouldSucceedSignUp_successfully() throws Exception {
        //given
        String email = "test@test.com";
        String verifiedToken = UUID.randomUUID().toString();

        SignupRequest signupRequest = SignupRequest.builder()
                .email(email)
                .password("password1234Test!")
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender("F")
                .country("KR")
                .underlyingDisease(List.of("NONE"))
                .allergies(null)
                .isMarketingTermsAgreed(true)
                .verifiedToken(verifiedToken)
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        //then
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @DisplayName("이미 가입된 계정이 게스트, 정상 계정이면 회원가입에 실패하고, 409를 반환한다.")
    @ParameterizedTest(name = "[{index}] 계정 상태 : {0}")
    @MethodSource("provideUserStatuses")
    void shouldReturn409Conflict_whenSignUpWithAlreadyRegisteredEmail(String status, UserStatus userStatus)
            throws Exception {
        //given
        String verifiedToken = UUID.randomUUID().toString();
        String email = "test@test.com";

        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("paosdfalsfjlas")
                .name("이미 가입된 유저")
                .nickname("닉네임")
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .provider(Provider.LOCAL)
                .isMarketingTermsAgreed(true)
                .status(userStatus)
                .build());

        SignupRequest signupRequest = SignupRequest.builder()
                .email(email)
                .password("password1234Test!")
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender("F")
                .country("KR")
                .underlyingDisease(List.of("NONE"))
                .allergies(null)
                .isMarketingTermsAgreed(true)
                .verifiedToken(verifiedToken)
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists."));

        //then
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @DisplayName("이미 가입된 닉네임이 게스트, 정지, 차단, 정상 계정이면 회원가입에 실패하고, 409를 반환한다.")
    @ParameterizedTest(name = "[{index}] 계정 상태 : {0}")
    @MethodSource("provideUserStatuses")
    void shouldReturn409Conflict_whenSignUpWithAlreadyRegisteredNickname(String status, UserStatus userStatus)
            throws Exception {
        //given
        String verifiedToken = UUID.randomUUID().toString();
        String email = "test@test.com";

        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email("aaaaaaa@test.com")
                .password("paosdfalsfjlas")
                .name("이미 가입된 유저")
                .nickname("닉네임")
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .provider(Provider.LOCAL)
                .isMarketingTermsAgreed(true)
                .status(userStatus)
                .build());

        SignupRequest signupRequest = SignupRequest.builder()
                .email(email)
                .password("password1234Test!")
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender("F")
                .country("KR")
                .underlyingDisease(List.of("NONE"))
                .allergies(null)
                .isMarketingTermsAgreed(true)
                .verifiedToken(verifiedToken)
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Nickname already exists."));

        //then
        assertThat(userRepository.count()).isEqualTo(1);
    }

    private static Stream<Arguments> provideUserStatuses() {
        return Stream.of(
                Arguments.of("게스트", UserStatus.GUEST),
                Arguments.of("정상", UserStatus.ACTIVE)
        );
    }

    @DisplayName("이미 가입된 계정이 탈퇴한 계정이거나 삭제된 계정이라면 회원가입에 성공한다.")
    @ParameterizedTest(name = "[{index}] 계정 상태 : {0}")
    @MethodSource("provideWithdrawableOrDeletedStatuses")
    void shouldSucceedSignUp_whenAccountIsWithdrawnOrDeleted(String status, UserStatus userStatus) throws Exception {
        //given
        String email = "test@test.com";
        String verifiedToken = UUID.randomUUID().toString();

        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("paosdfalsfjlas")
                .name("이미 가입된 유저")
                .nickname("닉네임")
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .provider(Provider.LOCAL)
                .isMarketingTermsAgreed(true)
                .status(userStatus)
                .build());

        SignupRequest signupRequest = SignupRequest.builder()
                .email(email)
                .password("password1234Test!")
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender("F")
                .country("KR")
                .underlyingDisease(List.of("NONE"))
                .allergies(null)
                .isMarketingTermsAgreed(true)
                .verifiedToken(verifiedToken)
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        //then
        assertThat(userRepository.count()).isEqualTo(2);
    }

    private static Stream<Arguments> provideWithdrawableOrDeletedStatuses() {
        return Stream.of(
                Arguments.of("삭제", UserStatus.DELETED),
                Arguments.of("탈퇴", UserStatus.WITHDRAWN)
        );
    }

    @DisplayName("이메일이 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenEmailIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().email(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Email is required."));
    }

    @DisplayName("비밀번호가 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenPasswordIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().password(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Password is required."));
    }

    @DisplayName("이름이 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenNameIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().name(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Name is required."));
    }

    @DisplayName("닉네임이 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenNicknameIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().nickname(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Nickname is required."));
    }

    @DisplayName("생년월일이 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenBirthDateIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().birth(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Date of birth is required."));
    }

    @DisplayName("성별이 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenGenderIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().gender(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Gender is required."));
    }

    @DisplayName("국가가 null이면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenCountryIsNull() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().country(null).build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Country is required."));
    }

    @DisplayName("이메일 형식이 정규식에 맞지 않으면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenEmailFormatIsInvalid() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().email("invalid-email-format").build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Please enter a valid email address."));
    }

    @DisplayName("비밀번호가 요구조건(8~20자)에 맞지 않으면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenPasswordFormatIsInvalid1() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().password("!Pw12").build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Password must be between 8 and 20 characters."));
    }

    @DisplayName("비밀번호가 요구조건(특수문자 + 대문자 + 소문자)에 맞지 않으면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenPasswordFormatIsInvalid3() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().password("password1234").build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(
                        jsonPath("$.message").value("Password must include letters, numbers, and special characters."));
    }

    @DisplayName("이름이 2자 미만 또는 20자 초과면 회원가입에 실패하고 400 에러를 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenNameSizeIsInvalid() throws Exception {
        //given
        SignupRequest request = createValidSignupRequest().toBuilder().name("김").build();

        //when, then
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Name must be between 2 and 20 characters."));
    }

    @DisplayName("기저질환이 새로운 값이면 DB에 저장되고 유저와 매핑된다.")
    @Test
    void shouldSaveConditionAndMap_whenConditionIsNew() throws Exception {
        //given
        SignupRequest signupRequest = createValidSignupRequest().toBuilder()
                .underlyingDisease(List.of("DIABETES"))
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        //then
        assertThat(conditionRepository.count()).isEqualTo(1);
        assertThat(userConditionRepository.count()).isEqualTo(1);
    }

    @DisplayName("기저질환이 이미 존재하면 새로 저장하지 않고 매핑만 한다.")
    @Test
    void shouldNotSaveCondition_whenConditionAlreadyExists() throws Exception {
        //given
        conditionRepository.save(Condition.create("DIABETES"));

        SignupRequest signupRequest = createValidSignupRequest().toBuilder()
                .underlyingDisease(List.of("DIABETES"))
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        //then
        assertThat(conditionRepository.count()).isEqualTo(1);
        assertThat(userConditionRepository.count()).isEqualTo(1);
    }

    @DisplayName("기저질환을 여러 개 입력하면 전부 저장되고 매핑된다.")
    @Test
    void shouldSaveMultipleConditionsAndMap_whenMultipleConditionsGiven() throws Exception {
        //given
        SignupRequest signupRequest = createValidSignupRequest().toBuilder()
                .underlyingDisease(List.of("DIABETES", "ASTHMA", "CARDIOVASCULAR_DISEASE"))
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        //then
        assertThat(conditionRepository.count()).isEqualTo(3);
        assertThat(userConditionRepository.count()).isEqualTo(3);
    }

    @DisplayName("기저질환 일부가 이미 존재하면 새로운 것만 저장하고 전체 매핑한다.")
    @Test
    void shouldSaveOnlyNewConditions_whenSomeConditionsAlreadyExist() throws Exception {
        //given
        conditionRepository.save(Condition.create("DIABETES"));

        SignupRequest signupRequest = createValidSignupRequest().toBuilder()
                .underlyingDisease(List.of("DIABETES", "ASTHMA"))
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        //then
        assertThat(conditionRepository.count()).isEqualTo(2);
        assertThat(userConditionRepository.count()).isEqualTo(2);
    }

    @DisplayName("알레르기가 새로운 값이면 DB에 저장되고 유저와 매핑된다.")
    @Test
    void shouldSaveAllergyAndMap_whenAllergyIsNew() throws Exception {
        //given
        SignupRequest signupRequest = createValidSignupRequest().toBuilder()
                .allergies(List.of("Milk"))
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        //then
        assertThat(allergyRepository.count()).isEqualTo(1);
        assertThat(userAllergyRepository.count()).isEqualTo(1);
    }

    @DisplayName("알레르기가 이미 존재하면 새로 저장하지 않고 매핑만 한다.")
    @Test
    void shouldNotSaveAllergy_whenAllergyAlreadyExists() throws Exception {
        //given
        allergyRepository.save(Allergy.create("Milk"));

        SignupRequest signupRequest = createValidSignupRequest().toBuilder()
                .allergies(List.of("Milk"))
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        //then
        assertThat(allergyRepository.count()).isEqualTo(1);
        assertThat(userAllergyRepository.count()).isEqualTo(1);
    }

    @DisplayName("알레르기를 여러 개 입력하면 전부 저장되고 매핑된다.")
    @Test
    void shouldSaveMultipleAllergiesAndMap_whenMultipleAllergiesGiven() throws Exception {
        //given
        SignupRequest signupRequest = createValidSignupRequest().toBuilder()
                .allergies(List.of("Milk", "Eggs", "Nuts"))
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        //then
        assertThat(allergyRepository.count()).isEqualTo(3);
        assertThat(userAllergyRepository.count()).isEqualTo(3);
    }

    @DisplayName("알레르기가 null이면 저장하지 않고 회원가입에 성공한다.")
    @Test
    void shouldSucceedSignUp_whenAllergiesIsNull() throws Exception {
        //given
        SignupRequest signupRequest = createValidSignupRequest().toBuilder()
                .allergies(null)
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        //then
        assertThat(allergyRepository.count()).isEqualTo(0);
        assertThat(userAllergyRepository.count()).isEqualTo(0);
    }

    @DisplayName("알레르기 일부가 이미 존재하면 새로운 것만 저장하고 전체 매핑한다.")
    @Test
    void shouldSaveOnlyNewAllergies_whenSomeAllergiesAlreadyExist() throws Exception {
        //given
        allergyRepository.save(Allergy.create("Milk")); // 기존에 존재

        SignupRequest signupRequest = createValidSignupRequest().toBuilder()
                .allergies(List.of("Milk", "Eggs")) // Eggs는 신규
                .build();

        //when
        mockMvc.perform(post("/api/v1/auth/signup/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        //then
        assertThat(allergyRepository.count()).isEqualTo(2);
        assertThat(userAllergyRepository.count()).isEqualTo(2);
    }

    private SignupRequest createValidSignupRequest() {
        return SignupRequest.builder()
                .email("test@test.com")
                .password("password1234Test!")
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender("F")
                .country("KR")
                .underlyingDisease(List.of("NONE"))
                .allergies(null)
                .isMarketingTermsAgreed(true)
                .verifiedToken(UUID.randomUUID().toString())
                .build();
    }

}
