package com.meditrip.medicine.application;

import com.meditrip.medicine.application.dto.request.CreateMedicineReviewApplicationRequest;
import com.meditrip.medicine.domain.UserInfo;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicineReviewService {

    private final MedicineReviewRepository medicineReviewRepository;

    @Transactional
    public Long create(UserInfo userInfo, Long medicineId, CreateMedicineReviewApplicationRequest request) {
        MedicineReview review = MedicineReview.create(medicineId, request.getReview(), userInfo.getAge(), userInfo.getHeight(),
                userInfo.getWeight(), request.getRating(), userInfo.getGender(), userInfo.getCountry(),
                userInfo.getUserId(), request.getSymptom());

        MedicineReview saved = medicineReviewRepository.save(review);
        return saved.getId();
    }

}
