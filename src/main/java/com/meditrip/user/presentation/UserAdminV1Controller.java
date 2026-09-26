package com.meditrip.user.presentation;

import com.meditrip.common.jwt.CustomUserDetails;
import com.meditrip.common.response.PageResponse;
import com.meditrip.user.application.admin.UserAdminService;
import com.meditrip.user.application.admin.dto.response.AdminUserSummaryResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1/users")
@RequiredArgsConstructor
public class UserAdminV1Controller {

    private final UserAdminService userAdminService;

    @GetMapping("/statistics/signups")
    public ResponseEntity<PageResponse<AdminUserSummaryResponse>> getSignupStatistics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page
    ) {
        LocalDate resolvedStart = (startDate != null) ? startDate : LocalDate.of(2026, 1, 1);
        LocalDate resolvedEnd = (endDate != null) ? endDate : LocalDate.now(ZoneId.of("Asia/Seoul"));

        UUID userId = UUID.fromString(userDetails.getUserId());

        return ResponseEntity.ok(PageResponse.from(
                userAdminService.getSignupStatistics(resolvedStart, resolvedEnd, userId, size, page)));
    }

    @GetMapping("/statistics/distribution/country")
    public ResponseEntity<PageResponse<AdminUserSummaryResponse>> getCountryDistribution(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String country,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page) {
        LocalDate resolvedStart = (startDate != null) ? startDate : LocalDate.of(2026, 1, 1);
        LocalDate resolvedEnd = (endDate != null) ? endDate : LocalDate.now(ZoneId.of("Asia/Seoul"));

        UUID userId = UUID.fromString(userDetails.getUserId());

        return ResponseEntity.ok(PageResponse.from(
                userAdminService.getCountryDistribution(resolvedStart, resolvedEnd, country, userId, size, page)));
    }

}
