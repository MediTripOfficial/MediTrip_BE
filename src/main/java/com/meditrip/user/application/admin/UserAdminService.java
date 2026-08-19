package com.meditrip.user.application.admin;

import com.meditrip.common.domain.UserRole;
import com.meditrip.user.application.admin.dto.response.AdminUserSummaryResponse;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAdminService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserSummaryResponse> getSignupStatistics(LocalDate start, LocalDate end, UUID userId,
                                                              int size, int page) {
        log.info("관리자([{}])가 회원가입 유저 조회.", userId);

        PageRequest pageRequest = createPageRequest(page, size);
        Instant startInstant = toStartOfDayInstant(start);
        Instant endInstant = toStartOfDayInstant(end.plusDays(1));

        Page<User> users = userRepository.findByCreatedAtBetweenAndUserRoleNot(startInstant, endInstant, UserRole.ADMIN,
                pageRequest);

        List<AdminUserSummaryResponse> response = users.stream().map(this::toSignupStatisticsResponse).toList();

        return new PageImpl<>(response, users.getPageable(), users.getTotalElements());
    }

    public Page<AdminUserSummaryResponse> getCountryDistribution(LocalDate start, LocalDate end, String country,
                                                                 UUID userId, int size, int page) {
        log.info("관리자([{}])가 국가별 유저 조회.", userId);

        PageRequest pageRequest = createPageRequest(page, size);
        Instant startInstant = toStartOfDayInstant(start);
        Instant endInstant = toStartOfDayInstant(end.plusDays(1));

        Page<User> users = userRepository.findByCountryAndCreatedAtBetweenAndUserRoleNot(country, startInstant,
                endInstant, UserRole.ADMIN, pageRequest);

        List<AdminUserSummaryResponse> response = users.stream().map(this::toSignupStatisticsResponse).toList();

        return new PageImpl<>(response, users.getPageable(), users.getTotalElements());
    }

    private PageRequest createPageRequest(int page, int size) {
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, size, Sort.by(Direction.DESC, "createdAt"));
    }

    private Instant toStartOfDayInstant(LocalDate date) {
        return date.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
    }

    private AdminUserSummaryResponse toSignupStatisticsResponse(User u) {
        return AdminUserSummaryResponse.builder()
                .userId(u.getId())
                .email(u.getEmail())
                .name(u.getName())
                .nickname(u.getNickname())
                .weight(u.getWeight())
                .height(u.getHeight())
                .birth(u.getBirth())
                .gender(u.getGender() == null ? null : u.getGender().getEng())
                .country(u.getCountry())
                .status(u.getStatus())
                .isMarketingTermsAgreed(u.getIsMarketingTermsAgreed())
                .build();
    }

}
