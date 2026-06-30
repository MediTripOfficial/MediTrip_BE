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
import com.meditrip.user.application.dto.request.UpdateUserInfoApplicationRequest;
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

@ExtendWith(MockitoExtension.class)
class UserFacadeUpdateUserInfoTest {

    @InjectMocks
    private UserFacade userFacade;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @DisplayName("유저 정보 업데이트에 성공한다.")
    @Test
    void shouldSucceedUpdateUserInfo_whenRequestIsValid() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        UpdateUserInfoApplicationRequest request = getValidRequest();

        UserInfoResponse expectedResponse = UserInfoResponse.builder()
                .email(user.getEmail())
                .name(request.getName())
                .nickname(request.getNickname())
                .birth(request.getBirth())
                .gender(request.getGender().getEng())
                .country(request.getCountry())
                .weight(request.getWeight())
                .height(request.getHeight())
                .underlyingDisease(request.getUnderlyingDisease())
                .allergies(request.getAllergies())
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);
        given(userService.getUserInfo(userId)).willReturn(expectedResponse);

        //when
        UserInfoResponse response = userFacade.updateUserInfo(userId, request);

        //then
        assertThat(response.getName()).isEqualTo(request.getName());
        assertThat(response.getNickname()).isEqualTo(request.getNickname());
        assertThat(response.getCountry()).isEqualTo(request.getCountry());
        assertThat(response.getUnderlyingDisease()).isEqualTo(request.getUnderlyingDisease());
        assertThat(response.getAllergies()).isEqualTo(request.getAllergies());
    }

    @DisplayName("ACTIVE, GUEST 상태면 업데이트에 성공한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldSucceedUpdateUserInfo_whenUserStatusIsActiveOrGuest(UserStatus userStatus) {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, userStatus);
        UpdateUserInfoApplicationRequest request = getValidRequest();

        given(userService.findById(eq(userId), any())).willReturn(user);
        given(userService.getUserInfo(userId)).willReturn(UserInfoResponse.from(user, List.of(), List.of()));

        //when
        userFacade.updateUserInfo(userId, request);

        //then
        verify(userService, times(1)).deleteAllergyAndConditions(userId);
        verify(authService, times(1)).saveAllergies(any(), eq(userId));
        verify(authService, times(1)).saveConditions(any(), eq(userId));
        verify(userService, times(1)).getUserInfo(userId);
    }

    @DisplayName("업데이트 시 기존 conditions/allergies를 삭제하고 새로 저장한다.")
    @Test
    void shouldDeleteAndResaveConditionsAndAllergies_whenUpdating() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);
        UpdateUserInfoApplicationRequest request = getValidRequest();

        given(userService.findById(eq(userId), any())).willReturn(user);
        given(userService.getUserInfo(userId)).willReturn(UserInfoResponse.from(user, List.of(), List.of()));

        //when
        userFacade.updateUserInfo(userId, request);

        //then - 삭제 먼저, 저장 나중
        var inOrder = org.mockito.Mockito.inOrder(userService, authService);
        inOrder.verify(userService).deleteAllergyAndConditions(userId);
        inOrder.verify(authService).saveAllergies(eq(request.getAllergies()), eq(userId));
        inOrder.verify(authService).saveConditions(eq(request.getUnderlyingDisease()), eq(userId));
    }

    @DisplayName("allergies, conditions가 null이면 삭제 후 saveAllergies/saveConditions에 null 전달된다.")
    @Test
    void shouldPassNullAllergiesAndConditions_whenBothAreNull() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("업데이트 이름")
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
        given(userService.getUserInfo(userId)).willReturn(UserInfoResponse.from(user, List.of(), List.of()));

        //when
        userFacade.updateUserInfo(userId, request);

        //then
        verify(userService, times(1)).deleteAllergyAndConditions(userId);
        verify(authService, times(1)).saveAllergies(eq(null), eq(userId));
        verify(authService, times(1)).saveConditions(eq(null), eq(userId));
    }

    // ==================== 유저 상태 ====================

    @DisplayName("WITHDRAWN, DELETED 상태면 NotFoundException이 발생한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldThrowNotFoundException_whenUserStatusIsWithdrawnOrDeleted(UserStatus userStatus) {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, userStatus);
        UpdateUserInfoApplicationRequest request = getValidRequest();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.updateUserInfo(userId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User Not Found");

        verify(userService, never()).deleteAllergyAndConditions(any());
        verify(authService, never()).saveAllergies(any(), any());
        verify(authService, never()).saveConditions(any(), any());
    }

    @DisplayName("유저가 존재하지 않으면 NotFoundException이 발생한다.")
    @Test
    void shouldThrowNotFoundException_whenUserDoesNotExist() {
        //given
        UUID userId = UUID.randomUUID();
        UpdateUserInfoApplicationRequest request = getValidRequest();

        given(userService.findById(eq(userId), any()))
                .willThrow(new NotFoundException("User Not Found"));

        //when, then
        assertThatThrownBy(() -> userFacade.updateUserInfo(userId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User Not Found");

        verify(userService, never()).deleteAllergyAndConditions(any());
        verify(authService, never()).saveAllergies(any(), any());
        verify(authService, never()).saveConditions(any(), any());
    }

    // ==================== 도메인 검증 ====================

    @DisplayName("이름이 null이면 IllegalArgumentException이 발생한다.")
    @Test
    void shouldThrowException_whenNameIsNull() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name(null)
                .nickname("닉네임")
                .weight(70.0).height(175.0)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F).country("KR")
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.updateUserInfo(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name is required.");
    }

    @DisplayName("이름이 2자 미만이면 IllegalArgumentException이 발생한다.")
    @Test
    void shouldThrowException_whenNameIsTooShort() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("김")
                .nickname("닉네임")
                .weight(70.0).height(175.0)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F).country("KR")
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.updateUserInfo(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name must be between 2 and 20 characters.");
    }

    @DisplayName("이름이 20자 초과면 IllegalArgumentException이 발생한다.")
    @Test
    void shouldThrowException_whenNameIsTooLong() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("김".repeat(21))
                .nickname("닉네임")
                .weight(70.0).height(175.0)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F).country("KR")
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.updateUserInfo(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name must be between 2 and 20 characters.");
    }

    @DisplayName("생년월일이 null이면 IllegalArgumentException이 발생한다.")
    @Test
    void shouldThrowException_whenBirthIsNull() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(70.0).height(175.0)
                .birth(null)
                .gender(Gender.F).country("KR")
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.updateUserInfo(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Date of birth is required.");
    }

    @DisplayName("생년월일이 미래 날짜면 IllegalArgumentException이 발생한다.")
    @Test
    void shouldThrowException_whenBirthIsInFuture() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(70.0).height(175.0)
                .birth(LocalDate.now().plusDays(1))
                .gender(Gender.F).country("KR")
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.updateUserInfo(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Date of birth can't be in the future.");
    }

    @DisplayName("생년월일이 150년 이상의 과거면 IllegalArgumentException이 발생한다.")
    @Test
    void shouldThrowException_whenBirthIsMoreThan150YearsAgo() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(70.0).height(175.0)
                .birth(LocalDate.now().minusYears(150).minusDays(1))
                .gender(Gender.F).country("KR")
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.updateUserInfo(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Please enter a valid date of birth.");
    }

    @DisplayName("성별이 null이면 IllegalArgumentException이 발생한다.")
    @Test
    void shouldThrowException_whenGenderIsNull() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        UpdateUserInfoApplicationRequest request = UpdateUserInfoApplicationRequest.builder()
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(70.0).height(175.0)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(null).country("KR")
                .build();

        given(userService.findById(eq(userId), any())).willReturn(user);

        //when, then
        assertThatThrownBy(() -> userFacade.updateUserInfo(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Gender is required.");
    }

    // ==================== 공통 ====================

    private UpdateUserInfoApplicationRequest getValidRequest() {
        return UpdateUserInfoApplicationRequest.builder()
                .name("업데이트 이름")
                .nickname("새닉네임")
                .weight(75.0)
                .height(180.0)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .underlyingDisease(List.of("diabetes"))
                .allergies(List.of("milk"))
                .isMarketingTermsAgreed(true)
                .build();
    }

    private User getUser(UUID userId, UserStatus userStatus) {
        return User.builder()
                .id(userId)
                .email("test@test.com")
                .password("password1234!")
                .name("테스트 유저")
                .nickname("닉네임")
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .weight(70.0)
                .height(175.0)
                .provider(Provider.LOCAL)
                .isMarketingTermsAgreed(true)
                .status(userStatus)
                .build();
    }
}
