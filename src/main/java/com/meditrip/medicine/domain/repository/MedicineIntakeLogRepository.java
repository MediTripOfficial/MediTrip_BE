package com.meditrip.medicine.domain.repository;

import com.meditrip.medicine.domain.entity.MedicineIntakeLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicineIntakeLogRepository extends JpaRepository<MedicineIntakeLog, Long> {
}
