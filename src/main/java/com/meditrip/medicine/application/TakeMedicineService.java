package com.meditrip.medicine.application;

import com.meditrip.medicine.domain.entity.MedicineIntake;
import com.meditrip.medicine.domain.repository.MedicineIntakeRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TakeMedicineService {

    private final MedicineIntakeRepository medicineIntakeRepository;

    @Transactional
    public Long take(Long medicineId, UUID userId) {
        MedicineIntake medicineIntake = MedicineIntake.create(medicineId, userId);

        MedicineIntake saved = medicineIntakeRepository.save(medicineIntake);

        return saved.getId();
    }

}
