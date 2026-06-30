package com.meditrip.medicine.domain.repository;

import com.meditrip.medicine.domain.entity.MedicineReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicineReviewRepository extends JpaRepository<MedicineReview, Long> {
}
