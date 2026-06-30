package com.meditrip.medicine.application;

import com.meditrip.medicine.application.dto.request.CreateMedicineReviewApplicationRequest;
import com.meditrip.medicine.domain.UserInfo;
import com.meditrip.medicine.domain.exception.MedicineNotFoundException;
import com.meditrip.user.application.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MedicineReviewFacade {

    private final UserService userService;
    private final MedicineReviewService medicineReviewService;
    private final MedicineService medicineService;

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

}
