package com.meditrip.medicine.domain.repository;

import com.meditrip.medicine.domain.entity.MedicineIntake;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicineIntakeRepository extends JpaRepository<MedicineIntake, Long> {
}
