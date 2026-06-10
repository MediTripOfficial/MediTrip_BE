package com.meditrip.user.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.user.application.dto.request.SignupApplicationRequest;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceSignupTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AllergyRepository allergyRepository;

    @Mock
    private UserAllergyRepository userAllergyRepository;

    @Mock
    private ConditionRepository conditionRepository;

    @Mock
    private UserConditionRepository userConditionRepository;

    @DisplayName("이미 가입된 유저가 아니라면 로컬 회원가입에 성공한다.")
    @Test
    void shouldSucceedLocalSignUp_withStatusLocalAndUserTypeActive_whenUserIsNotRegistered() {
        //given
        String encodedPassword = "ENCODED_PASSWORD";

        SignupApplicationRequest request = createSignupApplicationRequest();

        User user = mock(User.class);
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());
        given(passwordEncoder.encode(request.getPassword())).willReturn(encodedPassword);
        given(userRepository.save(any())).willReturn(user);

        //when, then
        assertDoesNotThrow(() -> authService.signup(request));

        verify(userRepository, times(1)).findByEmail(any());
        verify(passwordEncoder, times(1)).encode(request.getPassword());
        verify(userRepository, times(1)).save(any());
    }

    @DisplayName("이미 가입된 계정이 게스트, 정상 계정이면 회원가입에 실패하고, 409를 반환한다.")
    @ParameterizedTest(name = "[{index}] 계정 상태 : {0}")
    @MethodSource("provideUserStatuses")
    void shouldThrowException_whenSignUpWithAlreadyRegisteredEmail(String status, UserStatus userStatus)
            throws Exception {
        //given
        SignupApplicationRequest request = createSignupApplicationRequest();

        User user = mock(User.class);

        given(user.getStatus()).willReturn(userStatus);
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessage("Email already exists.");

        verify(userRepository, times(1)).findByEmail(any());
        verify(passwordEncoder, never()).encode(request.getPassword());
        verify(userRepository, never()).save(any());
    }

    @DisplayName("이미 가입된 닉네임이 게스트, 정상 계정이면 회원가입에 실패하고, 409를 반환한다.")
    @ParameterizedTest(name = "[{index}] 계정 상태 : {0}")
    @MethodSource("provideUserStatuses")
    void shouldThrowException_whenSignUpWithAlreadyRegisteredNickname(String status, UserStatus userStatus)
            throws Exception {
        //given
        SignupApplicationRequest request = createSignupApplicationRequest();

        User user = mock(User.class);

        given(user.getStatus()).willReturn(userStatus);
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());
        given(userRepository.findByNickname(request.getNickname())).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessage("Nickname already exists.");

        verify(userRepository, times(1)).findByEmail(any());
        verify(passwordEncoder, never()).encode(request.getPassword());
        verify(userRepository, never()).save(any());
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
        String encodedPassword = "ENCODED_PASSWORD";

        SignupApplicationRequest request = createSignupApplicationRequest();

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test@test.com")
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
                .build();

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.encode(request.getPassword())).willReturn(encodedPassword);
        given(userRepository.save(any())).willReturn(user);

        //when, then
        assertDoesNotThrow(() -> authService.signup(request));

        verify(userRepository, times(1)).findByEmail(eq("test@test.com"));
        verify(passwordEncoder, times(1)).encode(request.getPassword());
        verify(userRepository, times(1)).save(any());
    }

    @DisplayName("기저질환이 모두 새로운 값이면 전부 새로 저장되고 매핑된다.")
    @Test
    void shouldSaveNewConditions_whenAllConditionsAreNew() {
        //given
        SignupApplicationRequest request = createSignupApplicationRequest();
        User user = mock(User.class);

        Condition savedCondition = mock(Condition.class);
        given(savedCondition.getId()).willReturn(1L);

        given(userRepository.findByEmail(any())).willReturn(Optional.empty());
        given(userRepository.findByNickname(any())).willReturn(Optional.empty());
        given(passwordEncoder.encode(any())).willReturn("ENCODED");
        given(userRepository.save(any())).willReturn(user);
        given(conditionRepository.findAllByNameIn(any())).willReturn(List.of());
        given(conditionRepository.saveAll(any())).willReturn(List.of(savedCondition));

        //when
        authService.signup(request);

        //then
        verify(conditionRepository, times(1)).findAllByNameIn(any());
        verify(conditionRepository, times(1)).saveAll(any());
        verify(userConditionRepository, times(1)).saveAll(any());
    }

    @DisplayName("기저질환이 이미 존재하면 새로 저장하지 않고 매핑만 한다.")
    @Test
    void shouldNotSaveConditions_whenAllConditionsAlreadyExist() {
        //given
        Condition existingCondition = mock(Condition.class);
        given(existingCondition.getName()).willReturn("none");
        given(existingCondition.getId()).willReturn(1L);

        SignupApplicationRequest request = createSignupApplicationRequest();
        User user = mock(User.class);

        given(userRepository.findByEmail(any())).willReturn(Optional.empty());
        given(userRepository.findByNickname(any())).willReturn(Optional.empty());
        given(passwordEncoder.encode(any())).willReturn("ENCODED");
        given(userRepository.save(any())).willReturn(user);
        given(conditionRepository.findAllByNameIn(any())).willReturn(List.of(existingCondition));

        //when
        authService.signup(request);

        //then
        verify(conditionRepository, never()).saveAll(any());
        verify(userConditionRepository, times(1)).saveAll(any());
    }

    @DisplayName("기저질환 일부가 새로운 값이면 새로운 것만 저장하고 전체 매핑한다.")
    @Test
    void shouldSaveOnlyNewConditions_whenSomeConditionsAlreadyExist() {
        //given
        Condition existingCondition = mock(Condition.class);
        given(existingCondition.getName()).willReturn("DIABETES");
        given(existingCondition.getId()).willReturn(1L);

        Condition newSavedCondition = mock(Condition.class);
        given(newSavedCondition.getId()).willReturn(2L);

        SignupApplicationRequest request = SignupApplicationRequest.builder()
                .email("test@test.com")
                .password("password1234Test!")
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .underlyingDisease(List.of("DIABETES", "ASTHMA"))
                .allergies(null)
                .isMarketingTermsAgreed(true)
                .build();

        User user = mock(User.class);

        given(userRepository.findByEmail(any())).willReturn(Optional.empty());
        given(userRepository.findByNickname(any())).willReturn(Optional.empty());
        given(passwordEncoder.encode(any())).willReturn("ENCODED");
        given(userRepository.save(any())).willReturn(user);
        given(conditionRepository.findAllByNameIn(any())).willReturn(List.of(existingCondition));
        given(conditionRepository.saveAll(any())).willReturn(List.of(newSavedCondition));

        //when
        authService.signup(request);

        //then
        verify(conditionRepository, times(1)).saveAll(any());
        verify(userConditionRepository, times(1)).saveAll(any());
    }

    @DisplayName("기저질환 목록이 null이면 저장하지 않는다.")
    @Test
    void shouldNotSaveConditions_whenUnderlyingDiseaseIsNull() {
        //given
        SignupApplicationRequest request = SignupApplicationRequest.builder()
                .email("test@test.com")
                .password("password1234Test!")
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .underlyingDisease(null)
                .allergies(null)
                .isMarketingTermsAgreed(true)
                .build();

        User user = mock(User.class);

        given(userRepository.findByEmail(any())).willReturn(Optional.empty());
        given(userRepository.findByNickname(any())).willReturn(Optional.empty());
        given(passwordEncoder.encode(any())).willReturn("ENCODED");
        given(userRepository.save(any())).willReturn(user);

        //when
        authService.signup(request);

        //then
        verify(conditionRepository, never()).findAllByNameIn(any());
        verify(conditionRepository, never()).saveAll(any());
        verify(userConditionRepository, never()).saveAll(any());
    }

    @DisplayName("알레르기가 모두 새로운 값이면 전부 새로 저장되고 매핑된다.")
    @Test
    void shouldSaveNewAllergies_whenAllAllergiesAreNew() {
        //given
        Condition savedCondition = mock(Condition.class);
        given(savedCondition.getId()).willReturn(1L);

        Allergy savedAllergy = mock(Allergy.class);
        given(savedAllergy.getId()).willReturn(1L);

        SignupApplicationRequest request = SignupApplicationRequest.builder()
                .email("test@test.com")
                .password("password1234Test!")
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .underlyingDisease(List.of("NONE"))
                .allergies(List.of("Milk", "Eggs"))
                .isMarketingTermsAgreed(true)
                .build();

        User user = mock(User.class);

        given(userRepository.findByEmail(any())).willReturn(Optional.empty());
        given(userRepository.findByNickname(any())).willReturn(Optional.empty());
        given(passwordEncoder.encode(any())).willReturn("ENCODED");
        given(userRepository.save(any())).willReturn(user);
        given(conditionRepository.findAllByNameIn(any())).willReturn(List.of());
        given(conditionRepository.saveAll(any())).willReturn(List.of(savedCondition));
        given(allergyRepository.findAllByNameIn(any())).willReturn(List.of());
        given(allergyRepository.saveAll(any())).willReturn(List.of(savedAllergy));

        //when
        authService.signup(request);

        //then
        verify(allergyRepository, times(1)).findAllByNameIn(any());
        verify(allergyRepository, times(1)).saveAll(any());
        verify(userAllergyRepository, times(1)).saveAll(any());
    }

    @DisplayName("알레르기가 이미 존재하면 새로 저장하지 않고 매핑만 한다.")
    @Test
    void shouldNotSaveAllergies_whenAllAllergiesAlreadyExist() {
        //given
        Condition savedCondition = mock(Condition.class);
        given(savedCondition.getId()).willReturn(1L);

        Allergy existingAllergy = mock(Allergy.class);
        given(existingAllergy.getName()).willReturn("milk");
        given(existingAllergy.getId()).willReturn(1L);

        SignupApplicationRequest request = SignupApplicationRequest.builder()
                .email("test@test.com")
                .password("password1234Test!")
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .underlyingDisease(List.of("NONE"))
                .allergies(List.of("milk"))
                .isMarketingTermsAgreed(true)
                .build();

        User user = mock(User.class);

        given(userRepository.findByEmail(any())).willReturn(Optional.empty());
        given(userRepository.findByNickname(any())).willReturn(Optional.empty());
        given(passwordEncoder.encode(any())).willReturn("ENCODED");
        given(userRepository.save(any())).willReturn(user);
        given(conditionRepository.findAllByNameIn(any())).willReturn(List.of());
        given(conditionRepository.saveAll(any())).willReturn(List.of(savedCondition));
        given(allergyRepository.findAllByNameIn(any())).willReturn(List.of(existingAllergy));

        //when
        authService.signup(request);

        //then
        verify(allergyRepository, never()).saveAll(any());
        verify(userAllergyRepository, times(1)).saveAll(any());
    }

    @DisplayName("알레르기 목록이 null이면 저장하지 않는다.")
    @Test
    void shouldNotSaveAllergies_whenAllergiesIsNull() {
        //given
        Condition savedCondition = mock(Condition.class);
        given(savedCondition.getId()).willReturn(1L);

        SignupApplicationRequest request = createSignupApplicationRequest();
        User user = mock(User.class);

        given(userRepository.findByEmail(any())).willReturn(Optional.empty());
        given(userRepository.findByNickname(any())).willReturn(Optional.empty());
        given(passwordEncoder.encode(any())).willReturn("ENCODED");
        given(userRepository.save(any())).willReturn(user);
        given(conditionRepository.findAllByNameIn(any())).willReturn(List.of());
        given(conditionRepository.saveAll(any())).willReturn(List.of(savedCondition));

        //when
        authService.signup(request);

        //then
        verify(allergyRepository, never()).findAllByNameIn(any());
        verify(allergyRepository, never()).saveAll(any());
        verify(userAllergyRepository, never()).saveAll(any());
    }

    @DisplayName("알레르기 일부가 새로운 값이면 새로운 것만 저장하고 전체 매핑한다.")
    @Test
    void shouldSaveOnlyNewAllergies_whenSomeAllergiesAlreadyExist() {
        //given
        Condition savedCondition = mock(Condition.class);
        given(savedCondition.getId()).willReturn(1L);

        Allergy existingAllergy = mock(Allergy.class);
        given(existingAllergy.getName()).willReturn("Milk");
        given(existingAllergy.getId()).willReturn(1L);

        Allergy newSavedAllergy = mock(Allergy.class);
        given(newSavedAllergy.getId()).willReturn(2L);

        SignupApplicationRequest request = SignupApplicationRequest.builder()
                .email("test@test.com")
                .password("password1234Test!")
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .underlyingDisease(List.of("NONE"))
                .allergies(List.of("Milk", "Eggs"))
                .isMarketingTermsAgreed(true)
                .build();

        User user = mock(User.class);

        given(userRepository.findByEmail(any())).willReturn(Optional.empty());
        given(userRepository.findByNickname(any())).willReturn(Optional.empty());
        given(passwordEncoder.encode(any())).willReturn("ENCODED");
        given(userRepository.save(any())).willReturn(user);
        given(conditionRepository.findAllByNameIn(any())).willReturn(List.of());
        given(conditionRepository.saveAll(any())).willReturn(List.of(savedCondition));
        given(allergyRepository.findAllByNameIn(any())).willReturn(List.of(existingAllergy));
        given(allergyRepository.saveAll(any())).willReturn(List.of(newSavedAllergy));

        //when
        authService.signup(request);

        //then
        verify(allergyRepository, times(1)).saveAll(any());
        verify(userAllergyRepository, times(1)).saveAll(any());
    }

    private static Stream<Arguments> provideWithdrawableOrDeletedStatuses(){
        return Stream.of(
                Arguments.of("삭제", UserStatus.DELETED),
                Arguments.of("탈퇴", UserStatus.WITHDRAWN)
        );
    }

    private SignupApplicationRequest createSignupApplicationRequest() {
        return SignupApplicationRequest.builder()
                .email("test@test.com")
                .password("password1234Test!")
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .underlyingDisease(List.of("none"))
                .allergies(null)
                .isMarketingTermsAgreed(true)
                .build();
    }

}
