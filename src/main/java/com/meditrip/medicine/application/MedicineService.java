package com.meditrip.medicine.application;

import com.meditrip.medicine.application.dto.MedicineInfo;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.exception.MedicineNotFoundException;
import com.meditrip.medicine.domain.repository.MedicineQueryRepository;
import com.meditrip.medicine.domain.repository.MedicineRepository;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicineService {

    private final MedicineQueryRepository medicineQueryRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineReviewRepository medicineReviewRepository;

    @Transactional(readOnly = true)
    public MedicineInfo getInfo(Long medicineId, UUID userId) {
        Optional<MedicineInfo> medicineInfo = medicineQueryRepository.findInfoById(medicineId);
        if (medicineInfo.isEmpty()) {
            log.info("존재하지 않는 약 정보 조회 요청. User Id : [{}], medicineId : [{}]", userId, medicineId);
            throw new MedicineNotFoundException();
        }

        return medicineInfo.get();
    }

    @Transactional(readOnly = true)
    public MedicineReview getMedicineTopReview(Long medicineId) {
        Optional<MedicineReview> topReview =
                medicineReviewRepository.findTopByMedicineIdAndIsDeletedFalseOrderByIdDesc(medicineId);

        return topReview.orElse(null);
    }

    public boolean existsById(Long medicineId) {
        return medicineRepository.existsById(medicineId);
    }

}
