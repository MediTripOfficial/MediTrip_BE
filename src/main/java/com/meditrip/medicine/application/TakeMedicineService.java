package com.meditrip.medicine.application;

import com.meditrip.medicine.domain.IntakeSortType;
import com.meditrip.medicine.domain.TakeMedicineCondition;
import com.meditrip.medicine.domain.entity.MedicineIntake;
import com.meditrip.medicine.domain.entity.MedicineIntakeLog;
import com.meditrip.medicine.domain.exception.MedicineIntakeNotFoundException;
import com.meditrip.medicine.domain.repository.MedicineIntakeLogRepository;
import com.meditrip.medicine.domain.repository.MedicineIntakeQueryRepository;
import com.meditrip.medicine.domain.repository.MedicineIntakeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TakeMedicineService {

    private final MedicineIntakeRepository medicineIntakeRepository;
    private final MedicineIntakeLogRepository medicineIntakeLogRepository;
    private final MedicineIntakeQueryRepository medicineIntakeQueryRepository;

    @Transactional
    public Long take(Long medicineId, UUID userId) {
        MedicineIntake medicineIntake = MedicineIntake.create(medicineId, userId);

        MedicineIntake saved = medicineIntakeRepository.save(medicineIntake);

        return saved.getId();
    }

    @Transactional
    public void applyFollowUp(Long intakeId, UUID userId, TakeMedicineCondition condition) {
        Optional<MedicineIntake> medicineIntakeOptional = medicineIntakeRepository.findById(intakeId);

        if (medicineIntakeOptional.isEmpty()) {
            log.info("존재하지 않는 복약 정보 업데이트 요청. User Id : [{}], intakeId : [{}]", userId, intakeId);
            throw new MedicineIntakeNotFoundException();
        }

        MedicineIntake medicineIntake = medicineIntakeOptional.get();

        if (!medicineIntake.getUserId().equals(userId)) {
            log.info("본인 소유가 아닌 복약 정보 업데이트 요청. UserId : [{}], intakeId : [{}]", userId, intakeId);
            throw new AccessDeniedException("You do not have permission to access this medication record.");
        }

        Instant now = Instant.now();
        medicineIntake.applyFollowUp(condition, now);

        MedicineIntakeLog log = MedicineIntakeLog.builder()
                .medicineIntakeId(intakeId)
                .condition(condition)
                .takenAt(now)
                .build();

        medicineIntakeLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<MedicineIntake> findByMedicineIntakesById(UUID userId, PageRequest pageRequest,
                                                                             LocalDate firstStartDate,
                                                                             LocalDate firstEndDate, String sortInput) {
        IntakeSortType sort = IntakeSortType.valueOf(sortInput);
        return medicineIntakeQueryRepository.findIntakes(userId, firstStartDate, firstEndDate, sort, pageRequest);
    }
}
