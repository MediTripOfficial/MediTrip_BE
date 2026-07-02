package com.meditrip.medicine.application;

import com.meditrip.medicine.application.dto.request.UpdateTakeMedicineApplicationRequest;
import com.meditrip.medicine.domain.exception.MedicineNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TakeMedicineFacade {

    private final TakeMedicineService takeMedicineService;
    private final MedicineService medicineService;

    public Long take(Long medicineId, UUID userId) {
        log.info("복약 정보 삽입 요청. User Id : [{}], medicineId : [{}]", userId, medicineId);

        if (!medicineService.existsById(medicineId)) {
            log.info("존재하지 않는 약에 대해 복약 기록 요청. User Id: [{}]. medicineId : [{}]", userId, medicineId);
            throw new MedicineNotFoundException();
        }

        return takeMedicineService.take(medicineId, userId);
    }

    public void recordFollowUp(UpdateTakeMedicineApplicationRequest request, UUID userId) {
        log.info("복약 팔로우업 기록 요청. User Id : [{}], intakeId : [{}]", userId, request.getHistoryId());
        takeMedicineService.applyFollowUp(request.getHistoryId(), userId, request.getCondition());
    }

}
