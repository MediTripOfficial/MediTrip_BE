package com.meditrip.medicine.application.dto.response;

import com.meditrip.medicine.application.dto.MedicineInfo;
import com.meditrip.medicine.domain.entity.MedicineReview;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MedicineResponse {

    private final Long id;
    private final String name;
    private final String imageUrl;
    private final String manufacturer;
    private final List<Ingredient> ingredients;
    private final List<String> diseaseHashtags;
    private final List<String> efficacyHashtags;
    private final Boolean isChildSafe;
    private final Boolean isConvenienceStore;
    private final List<String> purchaseLocation;
    private final String dosage;
    private final String interval;
    private final String maxLimit;
    private final String caution;
    private final String details;
    private final String drugInteractions;
    private final String seeDoctor;
    private final String countryCode;
    private final Double rating;
    private final Integer reviewCount;
    private final Review topReview;

    public static MedicineResponse of(MedicineInfo info, Double rating, int reviewCount, Review topReview) {
        return MedicineResponse.builder()
                .id(info.getId())
                .name(info.getName())
                .imageUrl(info.getImageUrl())
                .manufacturer(info.getManufacturer())
                .ingredients(info.getIngredients() == null
                        ? null
                        : info.getIngredients().stream()
                                .map(Ingredient::of)
                                .toList())
                .diseaseHashtags(info.getDiseaseHashtags())
                .efficacyHashtags(info.getEfficacyHashtags())
                .isChildSafe(info.getIsChildSafe())
                .isConvenienceStore(info.getIsConvenienceStore())
                .purchaseLocation(info.getPurchaseLocation())
                .dosage(info.getDosage())
                .interval(info.getInterval())
                .maxLimit(info.getMaxLimit())
                .caution(info.getCaution())
                .details(info.getDetails())
                .drugInteractions(info.getDrugInteractions())
                .seeDoctor(info.getSeeDoctor())
                .countryCode(info.getCountryCode())
                .rating(rating)
                .reviewCount(reviewCount)
                .topReview(topReview)
                .build();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Ingredient {
        private final String ingredientName;
        private final String amount;

        public static Ingredient of(MedicineInfo.Ingredient ingredient) {
            return Ingredient.builder()
                    .ingredientName(ingredient.getIngredientName())
                    .amount(ingredient.getAmount())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Review {
        private final Long id;
        private final String nickname;
        private final String authorGender;
        private final String authorAgeGroup;
        private final String authorRegion;
        private final Double rating;
        private final LocalDate createdAt;
        private final List<String> symptoms;
        private final String review;
        private final String profileImg;

        public static Review of(MedicineReview review, String nickname, String profileImg, Integer age) {
            return Review.builder()
                    .id(review.getId())
                    .authorGender(review.getGender())
                    .authorAgeGroup(toAgeGroup(age))
                    .authorRegion(review.getCountry())
                    .rating(review.getRating())
                    .createdAt(review.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDate())
                    .symptoms(toSymptomList(review.getSymptom()))
                    .review(review.getReview())
                    .nickname(nickname)
                    .profileImg(profileImg)
                    .build();
        }
    }

    private static String toAgeGroup(Integer age) {
        if (age == null) {
            return null;
        }
        int decade = (age / 10) * 10;
        return decade + "s";
    }

    private static List<String> toSymptomList(String symptom) {
        return symptom == null ? List.of() : List.of(symptom);
    }

}
