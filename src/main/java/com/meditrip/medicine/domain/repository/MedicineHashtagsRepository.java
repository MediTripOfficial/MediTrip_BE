package com.meditrip.medicine.domain.repository;

import com.meditrip.medicine.domain.entity.MedicineHashtags;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicineHashtagsRepository extends JpaRepository<MedicineHashtags, Long> {
}
