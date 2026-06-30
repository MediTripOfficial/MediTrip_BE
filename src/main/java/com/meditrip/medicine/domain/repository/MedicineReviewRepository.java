package com.meditrip.medicine.domain.repository;

import com.meditrip.medicine.domain.entity.MedicineReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicineReviewRepository extends JpaRepository<MedicineReview, Long> {
    List<MedicineReview> findAllByMedicineId(Long medicineId);

    Optional<MedicineReview> findTopByMedicineIdAndIsDeletedFalseOrderByIdDesc(Long medicineId);

    List<MedicineReview> findAllByMedicineIdAndIsDeletedFalse(Long medicineId);

    List<MedicineReview> findAllByMedicineIdInAndIsDeletedFalse(List<Long> medicineIds);

}
