package com.meditrip.user.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.common.domain.UserRole;
import com.meditrip.common.domain.UserStatus;
import com.meditrip.user.application.admin.dto.response.AdminUserSummaryResponse;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.entity.enums.Gender;
import com.meditrip.user.domain.entity.enums.Provider;
import com.meditrip.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceGetCountryDistributionTest {

    @InjectMocks
    private UserAdminService userAdminService;

    @Mock
    private UserRepository userRepository;

    @DisplayName("국가별 유저를 조회할 수 있다.")
    @Test
    void shouldRetrieveRegisteredUsers_byCountry() {
        //given
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate end = LocalDate.now().minusDays(1);
        UUID userId = UUID.randomUUID();
        int size = 9;
        int page = 0;
        String country = "KR";

        User mockUser = getUser(UUID.randomUUID(), UserStatus.ACTIVE);

        given(userRepository.findByCountryAndCreatedAtBetweenAndUserRoleNot(eq(country), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(mockUser)));

        //when
        Page<AdminUserSummaryResponse> response = userAdminService.getCountryDistribution(start, end, country, userId,
                size, page);

        //then
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getContent().get(0).getUserId()).isEqualTo(mockUser.getId());

        verify(userRepository, times(1)).findByCountryAndCreatedAtBetweenAndUserRoleNot(
                country,
                start.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
                end.plusDays(1).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
                UserRole.ADMIN,
                PageRequest.of(page, size, Sort.by(Direction.DESC, "createdAt")));
    }

    @DisplayName("page가 음수라면 0 페이지로 조회된다.")
    @Test
    void shouldDefaultToFirstPage_whenPageIsNegative() {
        //given
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate end = LocalDate.now().minusDays(1);
        UUID userId = UUID.randomUUID();
        int size = 9;
        int page = -1;
        String country = "KR";

        User mockUser = getUser(UUID.randomUUID(), UserStatus.ACTIVE);

        given(userRepository.findByCountryAndCreatedAtBetweenAndUserRoleNot(eq(country), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(mockUser)));

        //when
        Page<AdminUserSummaryResponse> response = userAdminService.getCountryDistribution(start, end, country, userId,
                size, page);

        //then
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getContent().get(0).getUserId()).isEqualTo(mockUser.getId());

        verify(userRepository, times(1)).findByCountryAndCreatedAtBetweenAndUserRoleNot(
                country,
                start.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
                end.plusDays(1).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
                UserRole.ADMIN,
                PageRequest.of(0, size, Sort.by(Direction.DESC, "createdAt")));
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
