package com.meditrip.user.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.common.domain.UserStatus;
import com.meditrip.user.application.dto.request.UpdatePasswordApplicationRequest;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.entity.enums.Gender;
import com.meditrip.user.domain.entity.enums.Provider;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserFacadeUpdatePasswordTest {

    @InjectMocks
    private UserFacade userFacade;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @DisplayName("유저 정보가 존재하고 GUEST, ACTIVE 상태이며 기존 비밀번호가 일치하면 비밀번호 변경에 성공한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldChangePasswordSuccessfully_whenUserExistsAndIsActiveOrGuestAndPasswordMatches(UserStatus userStatus) {
        //given
        UUID userId = UUID.randomUUID();
        UpdatePasswordApplicationRequest request = UpdatePasswordApplicationRequest.builder()
                .existingPassword("password1234!")
                .newPassword("asdfqwer1234$")
                .build();

        User user = User.builder()
                .id(userId)
                .email("email@test.com")
                .password(request.getExistingPassword())
                .name("테스트 유저")
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

        given(userService.findById(userId, "비밀번호 변경")).willReturn(user);
        willDoNothing().given(authService).verifyPassword(request.getExistingPassword(), user.getPassword());

        //when, then
        userFacade.updatePassword(userId, request);

        verify(authService, times(1)).updatePassword(user, request.getNewPassword());
    }

    @DisplayName("유저가 ACTIVE 상태거나 GUEST 상태지만 비밀번호가 일치하지 않으면 예외가 발생한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldThrowException_whenUserIsActiveOrGuestButPasswordDoesNotMatch(UserStatus status) {
        //given
        UUID userId = UUID.randomUUID();
        UpdatePasswordApplicationRequest request = UpdatePasswordApplicationRequest.builder()
                .existingPassword("password1234!")
                .newPassword("asdfqwer1234$")
                .build();

        User user = User.builder()
                .id(userId)
                .email("email@test.com")
                .password("qwerasdf1111!")
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(80.0)
                .height(190.2)
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .provider(Provider.LOCAL)
                .isMarketingTermsAgreed(true)
                .status(status)
                .build();

        given(userService.findById(userId, "비밀번호 변경")).willReturn(user);
        willThrow(new IllegalArgumentException("Passwords do not match"))
                .given(authService)
                .verifyPassword(request.getExistingPassword(), user.getPassword());

        //when, then
        assertThatThrownBy(() -> userFacade.updatePassword(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passwords do not match");
    }

}
