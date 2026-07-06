package com.meditrip.medicine.domain.repository;

import com.meditrip.medicine.application.dto.MedicineInfo;
import com.meditrip.medicine.application.dto.MedicineInfo.Ingredient;
import com.meditrip.medicine.domain.entity.Hashtag.HashtagType;
import com.meditrip.medicine.domain.entity.Medicine;
import com.meditrip.medicine.domain.entity.QHashtag;
import com.meditrip.medicine.domain.entity.QIngredient;
import com.meditrip.medicine.domain.entity.QMedicine;
import com.meditrip.medicine.domain.entity.QMedicineHashtags;
import com.meditrip.medicine.domain.entity.QMedicineIngredients;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MedicineQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Optional<MedicineInfo> findInfoById(Long id) {
        Medicine medicine = queryFactory
                .selectFrom(QMedicine.medicine)
                .where(QMedicine.medicine.id.eq(id))
                .fetchOne();

        if (medicine == null) {
            return Optional.empty();
        }

        List<Ingredient> ingredients = queryFactory
                .select(Projections.constructor(
                        MedicineInfo.Ingredient.class,
                        QIngredient.ingredient.nameEn,
                        QMedicineIngredients.medicineIngredients.amount))
                .from(QMedicineIngredients.medicineIngredients)
                .join(QIngredient.ingredient)
                .on(QMedicineIngredients.medicineIngredients.ingredientId.eq(QIngredient.ingredient.id))
                .where(QMedicineIngredients.medicineIngredients.medicineId.eq(id))
                .fetch();

        List<String> diseaseHashtags = queryFactory
                .select(QHashtag.hashtag.name)
                .from(QMedicineHashtags.medicineHashtags)
                .join(QHashtag.hashtag)
                .on(QMedicineHashtags.medicineHashtags.hashtagId.eq(QHashtag.hashtag.id))
                .where(
                        QMedicineHashtags.medicineHashtags.medicineId.eq(id),
                        QHashtag.hashtag.type.eq(HashtagType.DISEASE)
                )
                .fetch();

        List<String> efficacyHashtags = queryFactory
                .select(QHashtag.hashtag.name)
                .from(QMedicineHashtags.medicineHashtags)
                .join(QHashtag.hashtag)
                .on(QMedicineHashtags.medicineHashtags.hashtagId.eq(QHashtag.hashtag.id))
                .where(
                        QMedicineHashtags.medicineHashtags.medicineId.eq(id),
                        QHashtag.hashtag.type.eq(HashtagType.EFFICACY)
                )
                .fetch();

        boolean isConvenienceStore = Boolean.TRUE.equals(medicine.getIsConvenienceStore());
        List<String> purchaseLocation = isConvenienceStore
                ? List.of("store", "pharmacy")
                : List.of("pharmacy");

        MedicineInfo medicineInfo = MedicineInfo.builder()
                .id(medicine.getId())
                .name(medicine.getNameEn())
                .imageUrl(medicine.getImageUrl())
                .manufacturer(medicine.getManufacturerEn())
                .ingredients(ingredients)
                .diseaseHashtags(diseaseHashtags)
                .efficacyHashtags(efficacyHashtags)
                .isChildSafe(medicine.getIsChildSafe())
                .isConvenienceStore(medicine.getIsConvenienceStore())
                .purchaseLocation(purchaseLocation)
                .dosage(medicine.getDosage())
                .interval(medicine.getDosageInterval())
                .maxLimit(medicine.getMaxLimit())
                .caution(medicine.getCaution())
                .details(medicine.getUsageDetailEn())
                .drugInteractions(medicine.getDrugInteractionsEn())
                .seeDoctor(medicine.getSeeDoctorEn())
                .countryCode(medicine.getCountryCode())
                .build();

        return Optional.of(medicineInfo);
    }

    public Map<Long, MedicineInfo> findAllByIds(List<Long> medicineIds) {
        if (medicineIds.isEmpty()) {
            return Map.of();
        }

        List<Medicine> medicines = queryFactory
                .selectFrom(QMedicine.medicine)
                .where(QMedicine.medicine.id.in(medicineIds))
                .fetch();

        Map<Long, List<Ingredient>> ingredientsByMedicineId = queryFactory
                .select(QMedicineIngredients.medicineIngredients.medicineId,
                        Projections.constructor(
                                MedicineInfo.Ingredient.class,
                                QIngredient.ingredient.nameEn,
                                QMedicineIngredients.medicineIngredients.amount))
                .from(QMedicineIngredients.medicineIngredients)
                .join(QIngredient.ingredient)
                .on(QMedicineIngredients.medicineIngredients.ingredientId.eq(QIngredient.ingredient.id))
                .where(QMedicineIngredients.medicineIngredients.medicineId.in(medicineIds))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(0, Long.class),
                        Collectors.mapping(tuple -> tuple.get(1, MedicineInfo.Ingredient.class), Collectors.toList())
                ));

        Map<Long, List<String>> diseaseHashtagsByMedicineId = fetchHashtagsGrouped(medicineIds, HashtagType.DISEASE);
        Map<Long, List<String>> efficacyHashtagsByMedicineId = fetchHashtagsGrouped(medicineIds, HashtagType.EFFICACY);

        return medicines.stream()
                .collect(Collectors.toMap(
                        Medicine::getId,
                        m -> assembleMedicineInfo(m,
                                ingredientsByMedicineId.getOrDefault(m.getId(), List.of()),
                                diseaseHashtagsByMedicineId.getOrDefault(m.getId(), List.of()),
                                efficacyHashtagsByMedicineId.getOrDefault(m.getId(), List.of()))
                ));
    }

    public Map<Long, List<String>> fetchHashtagsGrouped(List<Long> medicineIds, HashtagType type) {
        return queryFactory
                .select(QMedicineHashtags.medicineHashtags.medicineId, QHashtag.hashtag.name)
                .from(QMedicineHashtags.medicineHashtags)
                .join(QHashtag.hashtag)
                .on(QMedicineHashtags.medicineHashtags.hashtagId.eq(QHashtag.hashtag.id))
                .where(
                        QMedicineHashtags.medicineHashtags.medicineId.in(medicineIds),
                        QHashtag.hashtag.type.eq(type)
                )
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(0, Long.class),
                        Collectors.mapping(tuple -> tuple.get(1, String.class), Collectors.toList())
                ));
    }

    private MedicineInfo assembleMedicineInfo(Medicine medicine, List<MedicineInfo.Ingredient> ingredients,
                                              List<String> diseaseHashtags, List<String> efficacyHashtags) {
        boolean isConvenienceStore = Boolean.TRUE.equals(medicine.getIsConvenienceStore());
        List<String> purchaseLocation = isConvenienceStore
                ? List.of("store", "pharmacy")
                : List.of("pharmacy");

        return MedicineInfo.builder()
                .id(medicine.getId())
                .name(medicine.getNameEn())
                .imageUrl(medicine.getImageUrl())
                .manufacturer(medicine.getManufacturerEn())
                .ingredients(ingredients)
                .diseaseHashtags(diseaseHashtags)
                .efficacyHashtags(efficacyHashtags)
                .isChildSafe(medicine.getIsChildSafe())
                .isConvenienceStore(medicine.getIsConvenienceStore())
                .purchaseLocation(purchaseLocation)
                .dosage(medicine.getDosage())
                .interval(medicine.getDosageInterval())
                .maxLimit(medicine.getMaxLimit())
                .caution(medicine.getCaution())
                .details(medicine.getUsageDetailEn())
                .drugInteractions(medicine.getDrugInteractionsEn())
                .seeDoctor(medicine.getSeeDoctorEn())
                .countryCode(medicine.getCountryCode())
                .build();
    }
}
