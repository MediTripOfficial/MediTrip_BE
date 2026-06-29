package com.meditrip.medicine.domain.repository;

import com.meditrip.medicine.domain.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
}
