package com.meditrip.medicine.domain.repository;

import com.meditrip.medicine.application.dto.MedicineInfo;
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
import java.util.Optional;
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

        List<MedicineInfo.Ingredient> ingredients = queryFactory
                .select(Projections.constructor(
                        MedicineInfo.Ingredient.class,
                        QIngredient.ingredient.name_en,
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
                .name(medicine.getName_en())
                .imageUrl(medicine.getImageUrl())
                .manufacturer(medicine.getManufacturer_en())
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
                .rating(null) //TODO : 리뷰 기능 추가 후 변경
                .reviewCount(null)
                .topReview(null)
                .build();

        return Optional.of(medicineInfo);
    }

}
