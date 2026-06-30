package com.meditrip.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.common.domain.UserStatus;
import com.meditrip.common.exception.NotFoundException;
import com.meditrip.user.application.dto.request.OnboardingApplicationRequest;
import com.meditrip.user.application.dto.response.UserInfoResponse;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.entity.enums.Gender;
import com.meditrip.user.domain.entity.enums.Provider;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class UserFacadeOnboardingTest {

    @InjectMocks
    private UserFacade userFacade;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @DisplayName("GUEST 유저는 온보딩에 성공하고 UserInfoResponse를 반환한다.")
    @Test
    void shouldSucceedOnboarding_whenUserIsGuest() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getGuestUser(userId);
        OnboardingApplicationRequest request = getValidRequest();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when
        UserInfoResponse response = userFacade.onboarding(userId, request);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(request.getName());
        assertThat(response.getNickname()).isEqualTo(request.getNickname());
        assertThat(response.getGender()).isEqualTo(request.getGender().getEng());
        assertThat(response.getCountry()).isEqualTo(request.getCountry());
        assertThat(response.getUnderlyingDisease()).isEqualTo(request.getUnderlyingDisease());
        assertThat(response.getAllergies()).isEqualTo(request.getAllergies());
    }

    @DisplayName("온보딩 성공 시 유저 status가 ACTIVE로 변경된다.")
    @Test
    void shouldChangeStatusToActive_whenOnboardingSucceeds() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getGuestUser(userId);
        OnboardingApplicationRequest request = getValidRequest();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when
        userFacade.onboarding(userId, request);

        //then
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @DisplayName("온보딩 성공 시 conditions와 allergies가 저장된다.")
    @Test
    void shouldSaveConditionsAndAllergies_whenOnboardingSucceeds() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getGuestUser(userId);

        OnboardingApplicationRequest request = OnboardingApplicationRequest.builder()
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(70.0)
                .height(175.0)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .underlyingDisease(List.of("diabetes", "asthma"))
                .allergies(List.of("milk", "eggs"))
                .isMarketingTermsAgreed(true)
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when
        userFacade.onboarding(userId, request);

        //then
        verify(authService, times(1)).saveConditions(eq(List.of("diabetes", "asthma")), eq(userId));
        verify(authService, times(1)).saveAllergies(eq(List.of("milk", "eggs")), eq(userId));
    }

    @DisplayName("conditions, allergies가 null이면 null로 전달된다.")
    @Test
    void shouldPassNullConditionsAndAllergies_whenBothAreNull() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getGuestUser(userId);

        OnboardingApplicationRequest request = OnboardingApplicationRequest.builder()
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(70.0)
                .height(175.0)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .underlyingDisease(null)
                .allergies(null)
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when
        userFacade.onboarding(userId, request);

        //then
        verify(authService, times(1)).saveConditions(eq(null), eq(userId));
        verify(authService, times(1)).saveAllergies(eq(null), eq(userId));
    }

    @DisplayName("GUEST가 아닌 유저가 온보딩하면 AccessDeniedException이 발생한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "WITHDRAWN", "DELETED"})
    void shouldThrowAccessDeniedException_whenUserIsNotGuest(UserStatus userStatus) {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, userStatus);
        OnboardingApplicationRequest request = getValidRequest();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.onboarding(userId, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You don't have permission to access this page.");

        verify(authService, never()).saveConditions(any(), any());
        verify(authService, never()).saveAllergies(any(), any());
    }

    @DisplayName("유저가 존재하지 않으면 NotFoundException이 발생한다.")
    @Test
    void shouldThrowNotFoundException_whenUserDoesNotExist() {
        //given
        UUID userId = UUID.randomUUID();
        OnboardingApplicationRequest request = getValidRequest();

        given(userService.findById(eq(userId), any()))
                .willThrow(new NotFoundException("User Not Found"));

        //when, then
        assertThatThrownBy(() -> userFacade.onboarding(userId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User Not Found");

        verify(authService, never()).saveConditions(any(), any());
        verify(authService, never()).saveAllergies(any(), any());
    }

    @DisplayName("이름이 null이면 IllegalArgumentException이 발생한다.")
    @Test
    void shouldThrowException_whenNameIsNull() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getGuestUser(userId);

        OnboardingApplicationRequest request = OnboardingApplicationRequest.builder()
                .name(null)
                .nickname("닉네임")
                .weight(70.0).height(175.0)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F).country("KR")
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.onboarding(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name is required.");
    }

    @DisplayName("이름이 2자 미만이면 IllegalArgumentException이 발생한다.")
    @Test
    void shouldThrowException_whenNameIsTooShort() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getGuestUser(userId);

        OnboardingApplicationRequest request = OnboardingApplicationRequest.builder()
                .name("김")
                .nickname("닉네임")
                .weight(70.0).height(175.0)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F).country("KR")
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.onboarding(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name must be between 2 and 20 characters.");
    }

    @DisplayName("이름이 20자 초과면 IllegalArgumentException이 발생한다.")
    @Test
    void shouldThrowException_whenNameIsTooLong() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getGuestUser(userId);

        OnboardingApplicationRequest request = OnboardingApplicationRequest.builder()
                .name("김".repeat(21))
                .nickname("닉네임")
                .weight(70.0).height(175.0)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F).country("KR")
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.onboarding(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name must be between 2 and 20 characters.");
    }

    @DisplayName("생년월일이 null이면 IllegalArgumentException이 발생한다.")
    @Test
    void shouldThrowException_whenBirthIsNull() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getGuestUser(userId);

        OnboardingApplicationRequest request = OnboardingApplicationRequest.builder()
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(70.0).height(175.0)
                .birth(null)
                .gender(Gender.F).country("KR")
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.onboarding(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Date of birth is required.");
    }

    @DisplayName("생년월일이 미래 날짜면 IllegalArgumentException이 발생한다.")
    @Test
    void shouldThrowException_whenBirthIsInFuture() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getGuestUser(userId);

        OnboardingApplicationRequest request = OnboardingApplicationRequest.builder()
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(70.0).height(175.0)
                .birth(LocalDate.now().plusDays(1))
                .gender(Gender.F).country("KR")
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.onboarding(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Date of birth can't be in the future.");
    }

    @DisplayName("생년월일이 150년 이상의 과거면 IllegalArgumentException이 발생한다.")
    @Test
    void shouldThrowException_whenBirthIsMoreThan150YearsAgo() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getGuestUser(userId);

        OnboardingApplicationRequest request = OnboardingApplicationRequest.builder()
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(70.0).height(175.0)
                .birth(LocalDate.now().minusYears(150).minusDays(1))
                .gender(Gender.F).country("KR")
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.onboarding(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Please enter a valid date of birth.");
    }

    @DisplayName("성별이 null이면 IllegalArgumentException이 발생한다.")
    @Test
    void shouldThrowException_whenGenderIsNull() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getGuestUser(userId);

        OnboardingApplicationRequest request = OnboardingApplicationRequest.builder()
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(70.0).height(175.0)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(null).country("KR")
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.onboarding(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Gender is required.");
    }

    private OnboardingApplicationRequest getValidRequest() {
        return OnboardingApplicationRequest.builder()
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(70.0)
                .height(175.0)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .underlyingDisease(List.of("diabetes"))
                .allergies(List.of("milk"))
                .isMarketingTermsAgreed(true)
                .build();
    }

    private User getGuestUser(UUID userId) {
        return User.builder()
                .id(userId)
                .email("test@test.com")
                .name("소셜 유저")
                .provider(Provider.GOOGLE)
                .isMarketingTermsAgreed(false)
                .status(UserStatus.GUEST)
                .build();
    }

    private User getUser(UUID userId, UserStatus userStatus) {
        return User.builder()
                .id(userId)
                .email("test@test.com")
                .name("소셜 유저")
                .provider(Provider.GOOGLE)
                .isMarketingTermsAgreed(false)
                .status(userStatus)
                .build();
    }

}
