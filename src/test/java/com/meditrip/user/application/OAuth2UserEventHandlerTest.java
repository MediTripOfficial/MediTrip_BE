package com.meditrip.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.common.domain.UserStatus;
import com.meditrip.common.event.OAuth2LoginRequestEvent;
import com.meditrip.common.event.OAuth2UnlinkRequestEvent;
import com.meditrip.config.oauth.user.OAuth2Provider;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.repository.UserRepository;
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

@ExtendWith(MockitoExtension.class)
class OAuth2UserEventHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private OAuth2UserEventHandler oAuth2UserEventHandler;

    private final String testEmail = "test@example.com";
    private final String testName = "테스트 유저";

    @DisplayName("가입된 적 없는 이메일이면 신규 유저를 생성하고 결과를 채운다.")
    @Test
    void shouldCreateNewUser_whenEmailIsNotRegistered() {
        //given
        OAuth2LoginRequestEvent event = new OAuth2LoginRequestEvent(testEmail, testName, OAuth2Provider.GOOGLE);
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.empty());

        //when
        oAuth2UserEventHandler.handleLogin(event);

        //then
        verify(userRepository, times(1)).save(any(User.class));
        assertThat(event.isHandled()).isTrue();
        assertThat(event.getUserStatus()).isEqualTo(UserStatus.GUEST.name());
        assertThat(event.getFailureReason()).isNull();
    }

    @DisplayName("ACTIVE 또는 GUEST 상태의 기존 유저는 정상적으로 로그인 처리된다.")
    @ParameterizedTest(name = "[{index}] 계정 상태 : {0}")
    @MethodSource("provideLoginableStatuses")
    void shouldLoginExistingUser_whenStatusIsActiveOrGuest(String label, UserStatus status) {
        //given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(testEmail)
                .status(status)
                .build();

        OAuth2LoginRequestEvent event = new OAuth2LoginRequestEvent(testEmail, testName, OAuth2Provider.GOOGLE);
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(user));

        //when
        oAuth2UserEventHandler.handleLogin(event);

        //then
        assertThat(event.isHandled()).isTrue();
        assertThat(event.getUserId()).isEqualTo(userId.toString());
        assertThat(event.getUserStatus()).isEqualTo(status.name());
        assertThat(event.getFailureReason()).isNull();

        verify(userRepository, never()).save(any());
    }

    private static Stream<Arguments> provideLoginableStatuses() {
        return Stream.of(
                Arguments.of("정상", UserStatus.ACTIVE),
                Arguments.of("게스트", UserStatus.GUEST)
        );
    }

    @DisplayName("탈퇴 또는 삭제된 계정은 소셜 로그인이 거부된다.")
    @ParameterizedTest(name = "[{index}] 계정 상태 : {0}")
    @MethodSource("provideBlockedStatuses")
    void shouldBlockLogin_whenStatusIsWithdrawnOrDeleted(String label, UserStatus status) {
        //given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(testEmail)
                .status(status)
                .build();

        OAuth2LoginRequestEvent event = new OAuth2LoginRequestEvent(testEmail, testName, OAuth2Provider.GOOGLE);
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(user));

        //when
        oAuth2UserEventHandler.handleLogin(event);

        //then
        assertThat(event.isHandled()).isFalse();
        assertThat(event.getUserId()).isNull();
        assertThat(event.getFailureReason()).isEqualTo("WITHDRAWN_OR_DELETED_ACCOUNT");

        verify(userRepository, never()).save(any());
    }

    private static Stream<Arguments> provideBlockedStatuses() {
        return Stream.of(
                Arguments.of("탈퇴", UserStatus.WITHDRAWN),
                Arguments.of("삭제", UserStatus.DELETED)
        );
    }

    @DisplayName("연동 해제 요청 시 유저를 탈퇴 처리하고 Refresh Token을 삭제한다.")
    @Test
    void shouldWithdrawUserAndDeleteRefreshToken_whenUnlinkRequested() {
        //given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(testEmail)
                .status(UserStatus.ACTIVE)
                .build();

        OAuth2UnlinkRequestEvent event = new OAuth2UnlinkRequestEvent(testEmail);
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(user));

        //when
        oAuth2UserEventHandler.handleUnlink(event);

        //then
        verify(tokenService, times(1)).deleteRefreshToken(userId);
        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
    }
}
