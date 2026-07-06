package com.meditrip.medicine.application;

import com.meditrip.medicine.application.dto.MedicineInfo;
import com.meditrip.medicine.application.dto.response.MedicineHistoryResponse;
import com.meditrip.medicine.application.dto.response.MedicineResponse;
import com.meditrip.medicine.domain.UserInfo;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.user.application.UserService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MedicineFacade {

    private final MedicineService medicineService;
    private final UserService userService;
    private final MedicineReviewService medicineReviewService;

    public MedicineResponse getInfo(Long medicineId, UUID userId) {
        MedicineInfo medicineInfo = medicineService.getInfo(medicineId, userId);

        List<MedicineReview> reviews = medicineReviewService.getReviews(medicineId);
        Double rating = reviews.isEmpty()
                ? null
                : reviews.stream().mapToDouble(MedicineReview::getRating).average().orElse(0.0);
        int reviewCount = reviews.size();

        MedicineResponse.Review topReviewResponse = buildTopReviewResponse(medicineId);

        return MedicineResponse.of(medicineInfo, rating, reviewCount, topReviewResponse);
    }

    private MedicineResponse.Review buildTopReviewResponse(Long medicineId) {
        MedicineReview topReview = medicineService.getMedicineTopReview(medicineId);
        if (topReview == null) {
            return null;
        }

        UserInfo userInfo = userService.getReviewUserInfo(topReview.getUserId());

        String nickname = userInfo == null ? "탈퇴한 유저" : userInfo.getNickname();
        String profileImg = userInfo == null ? null : userInfo.getProfileImg();
        Integer age = userInfo == null ? null : userInfo.getAge();

        return MedicineResponse.Review.of(topReview, nickname, profileImg, age);
    }

    public Page<MedicineHistoryResponse> getMedicineHistories(UUID userId, int page, int size,
                                                              LocalDate startDate, LocalDate endDate) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return medicineService.findByMedicineHistoriesByUserId(userId, startDate, endDate, pageRequest);
    }

}
