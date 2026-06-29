package com.meditrip.medicine.domain.repository;

import com.meditrip.medicine.domain.entity.Hashtag.HashtagType;
import com.meditrip.medicine.domain.entity.Medicine;
import com.meditrip.medicine.domain.entity.QHashtag;
import com.meditrip.medicine.domain.entity.QIngredient;
import com.meditrip.medicine.domain.entity.QMedicine;
import com.meditrip.medicine.domain.entity.QMedicineHashtags;
import com.meditrip.medicine.domain.entity.QMedicineIngredients;
import com.meditrip.medicine.domain.entity.QMedicineSymptomCode;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SymptomMedicineQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<Medicine> findMedicinesBySymptomCode(int symptomCode) {
        List<Long> medicineIds = queryFactory
                .select(QMedicineSymptomCode.medicineSymptomCode.medicineId)
                .from(QMedicineSymptomCode.medicineSymptomCode)
                .where(QMedicineSymptomCode.medicineSymptomCode.symptomCode.eq(symptomCode))
                .distinct()
                .fetch();

        if (medicineIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(QMedicine.medicine)
                .where(QMedicine.medicine.id.in(medicineIds))
                .fetch();
    }

    public Map<Long, List<String>> findIngredientNamesByMedicineIds(List<Long> medicineIds) {
        if (medicineIds.isEmpty()) {
            return Map.of();
        }

        List<Tuple> rows = queryFactory
                .select(QMedicineIngredients.medicineIngredients.medicineId, QIngredient.ingredient.name_en)
                .from(QMedicineIngredients.medicineIngredients)
                .join(QIngredient.ingredient)
                .on(QMedicineIngredients.medicineIngredients.ingredientId.eq(QIngredient.ingredient.id))
                .where(QMedicineIngredients.medicineIngredients.medicineId.in(medicineIds))
                .fetch();

        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (Tuple row : rows) {
            Long medicineId = row.get(QMedicineIngredients.medicineIngredients.medicineId);
            String ingredientName = row.get(QIngredient.ingredient.name_en);
            result.computeIfAbsent(medicineId, key -> new ArrayList<>()).add(ingredientName);
        }
        return result;
    }

    public List<String> findDiseaseHashtagNamesByMedicineIds(List<Long> medicineIds) {
        if (medicineIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .select(QHashtag.hashtag.name)
                .from(QMedicineHashtags.medicineHashtags)
                .join(QHashtag.hashtag)
                .on(QMedicineHashtags.medicineHashtags.hashtagId.eq(QHashtag.hashtag.id))
                .where(
                        QMedicineHashtags.medicineHashtags.medicineId.in(medicineIds),
                        QHashtag.hashtag.type.eq(HashtagType.DISEASE)
                )
                .distinct()
                .fetch();
    }

}
