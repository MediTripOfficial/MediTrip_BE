package com.meditrip.medicine.application;

import com.meditrip.medicine.application.dto.request.CreateMedicineReviewApplicationRequest;
import com.meditrip.medicine.domain.UserInfo;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.exception.MedicineNotFoundException;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicineReviewService {

    private final MedicineReviewRepository medicineReviewRepository;

    @Transactional
    public Long create(UserInfo userInfo, Long medicineId, CreateMedicineReviewApplicationRequest request) {
        MedicineReview review = MedicineReview.create(medicineId, request.getReview(), userInfo.getAge(),
                userInfo.getHeight(),
                userInfo.getWeight(), request.getRating(), userInfo.getGender(), userInfo.getCountry(),
                userInfo.getUserId(), request.getSymptom());

        MedicineReview saved = medicineReviewRepository.save(review);
        return saved.getId();
    }

    @Transactional
    public void delete(UUID userId, Long reviewId) {
        Optional<MedicineReview> review = medicineReviewRepository.findById(reviewId);
        if (review.isEmpty() || review.get().getIsDeleted().equals(Boolean.TRUE)) {
            log.info("존재하지 않는 약 리뷰 삭제 요청. User Id : [{}], reviewId : [{}]", userId, reviewId);
            throw new MedicineNotFoundException("Review Not Found.");
        }

        if (!review.get().getUserId().equals(userId)) {
            log.info("본인이 작성하지 않은 리뷰 삭제 요청. User Id : [{}], review Id : [{}]", userId, reviewId);
            throw new AccessDeniedException("You do not have permission to perform this action.");
        }

        review.get().delete();
    }

}
