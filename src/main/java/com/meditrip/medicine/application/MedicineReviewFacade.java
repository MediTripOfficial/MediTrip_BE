package com.meditrip.medicine.application;

import com.meditrip.common.response.CursorResponse;
import com.meditrip.medicine.application.dto.request.CreateMedicineReviewApplicationRequest;
import com.meditrip.medicine.application.dto.request.GetMedicineReviewsApplicationRequest;
import com.meditrip.medicine.application.dto.response.MedicineReviewsResponse;
import com.meditrip.medicine.domain.UserInfo;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.exception.MedicineNotFoundException;
import com.meditrip.medicine.domain.repository.MedicineReviewQueryRepository;
import com.meditrip.user.application.UserService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MedicineReviewFacade {

    private final UserService userService;
    private final MedicineReviewService medicineReviewService;
    private final MedicineService medicineService;
    private final MedicineReviewQueryRepository medicineReviewQueryRepository;

    private static final int DEFAULT_SIZE = 15;

    public Long createReview(UUID userId, Long medicineId, CreateMedicineReviewApplicationRequest request) {
        log.info("약 리뷰 생성 요청. User Id : [{}], MedicineId : [{}]", userId, medicineId);

        UserInfo userInfo = userService.getReviewUserInfo(userId);
        boolean medicineExists = medicineService.existsById(medicineId);

        if (!medicineExists) {
            log.info("존재하지 않는 약물에 대해 리뷰 요청. User Id : [{}], medicineId : [{}]", userId, medicineId);
            throw new MedicineNotFoundException();
        }

        return medicineReviewService.create(userInfo, medicineId, request);
    }

    @Transactional(readOnly = true)
    public CursorResponse<MedicineReviewsResponse> getReviews(UUID userId, Long medicineId,
                                                              GetMedicineReviewsApplicationRequest request) {
        boolean medicineExists = medicineService.existsById(medicineId);
        if (!medicineExists) {
            log.info("존재하지 않는 약물에 대해 리뷰 조회 요청. User Id : [{}], medicineId : [{}]", userId, medicineId);
            throw new MedicineNotFoundException();
        }

        int size = request.getSize() == null ? DEFAULT_SIZE : request.getSize();

        List<String> effectiveSymptoms = request.getSymptoms();

        List<MedicineReview> reviews = medicineReviewQueryRepository.findReviews(
                medicineId,
                request.getCursor(),
                size + 1,
                request.getSort(),
                request.getGender(),
                request.getCountries(),
                effectiveSymptoms
        );

        boolean hasNext = reviews.size() > size;
        List<MedicineReview> pageContent = hasNext ? reviews.subList(0, size) : reviews;

        String nextCursor = hasNext
                ? String.valueOf(pageContent.get(pageContent.size() - 1).getId())
                : null;

        // 리뷰 작성자들의 최신 닉네임을 한 번에 조회 (N+1 방지)
        List<UUID> authorIds = pageContent.stream()
                .map(MedicineReview::getUserId)
                .distinct()
                .toList();
        Map<UUID, String> nicknameByUserId = userService.getNicknamesByUserIds(authorIds);

        List<MedicineReviewsResponse> items = pageContent.stream()
                .map(review -> MedicineReviewsResponse.from(
                        review,
                        nicknameByUserId.get(review.getUserId()),
                        userId))
                .toList();

        return CursorResponse.of(items, nextCursor, hasNext);
    }

}
