package com.meditrip.medicine.application.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MedicineInfo {

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


    @Getter
    @Builder
    @AllArgsConstructor
    public static class Ingredient {
        private final String ingredientName;
        private final String amount;
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
        private final boolean isAuthor;
        private final String review;
    }

}
