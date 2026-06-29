package com.meditrip.medicine.domain.repository;

import com.meditrip.medicine.domain.entity.MedicineIngredients;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicineIngredientsRepository extends JpaRepository<MedicineIngredients, Long> {
}
