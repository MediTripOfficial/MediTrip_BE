package com.meditrip.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.meditrip.common.domain.UserStatus;
import com.meditrip.common.exception.NotFoundException;
import com.meditrip.user.application.dto.response.UserInfoResponse;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.entity.enums.Gender;
import com.meditrip.user.domain.entity.enums.Provider;
import com.meditrip.user.domain.repository.UserAllergyRepository;
import com.meditrip.user.domain.repository.UserConditionRepository;
import com.meditrip.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
class UserServiceGetUserInfoTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserConditionRepository userConditionRepository;

    @Mock
    private UserAllergyRepository userAllergyRepository;

    @DisplayName("유저 정보 조회에 성공한다.")
    @Test
    void shouldSucceedGetUserInfo_whenUserExistsAndStatusIsValid() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        List<String> conditions = List.of("DIABETES", "ASTHMA");
        List<String> allergies = List.of("Milk", "Eggs");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userConditionRepository.findConditionNamesByUserId(userId)).willReturn(conditions);
        given(userAllergyRepository.findAllergyNamesByUserId(userId)).willReturn(allergies);

        //when
        UserInfoResponse response = userService.getUserInfo(userId);

        //then
        assertThat(response.getEmail()).isEqualTo(user.getEmail());
        assertThat(response.getName()).isEqualTo(user.getName());
        assertThat(response.getNickname()).isEqualTo(user.getNickname());
        assertThat(response.getBirth()).isEqualTo(user.getBirth());
        assertThat(response.getGender()).isEqualTo(user.getGender().getEng());
        assertThat(response.getCountry()).isEqualTo(user.getCountry());
        assertThat(response.getWeight()).isEqualTo(user.getWeight());
        assertThat(response.getHeight()).isEqualTo(user.getHeight());
        assertThat(response.isMarketingTermsAgreed()).isEqualTo(user.getIsMarketingTermsAgreed());
        assertThat(response.getUnderlyingDisease()).isEqualTo(conditions);
        assertThat(response.getAllergies()).isEqualTo(allergies);
    }

    @DisplayName("ACTIVE, GUEST 상태면 유저 정보 조회에 성공한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldSucceedGetUserInfo_whenUserStatusIsActiveOrGuest(UserStatus userStatus) {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, userStatus);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userConditionRepository.findConditionNamesByUserId(userId)).willReturn(List.of());
        given(userAllergyRepository.findAllergyNamesByUserId(userId)).willReturn(List.of());

        //when
        UserInfoResponse response = userService.getUserInfo(userId);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(user.getEmail());
    }

    @DisplayName("기저질환과 알레르기가 없으면 빈 리스트로 반환된다.")
    @Test
    void shouldReturnEmptyLists_whenUserHasNoConditionsAndAllergies() {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, UserStatus.ACTIVE);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userConditionRepository.findConditionNamesByUserId(userId)).willReturn(List.of());
        given(userAllergyRepository.findAllergyNamesByUserId(userId)).willReturn(List.of());

        //when
        UserInfoResponse response = userService.getUserInfo(userId);

        //then
        assertThat(response.getUnderlyingDisease()).isEmpty();
        assertThat(response.getAllergies()).isEmpty();
    }

    @DisplayName("WITHDRAWN, DELETED 상태면 NotFoundException이 발생한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldThrowNotFoundException_whenUserStatusIsWithdrawnOrDeleted(UserStatus userStatus) {
        //given
        UUID userId = UUID.randomUUID();
        User user = getUser(userId, userStatus);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        //when, then
        assertThatThrownBy(() -> userService.getUserInfo(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User Not Found");
    }

    @DisplayName("유저가 존재하지 않으면 NotFoundException이 발생한다.")
    @Test
    void shouldThrowNotFoundException_whenUserDoesNotExist() {
        //given
        UUID userId = UUID.randomUUID();

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> userService.getUserInfo(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User Not Found");
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
