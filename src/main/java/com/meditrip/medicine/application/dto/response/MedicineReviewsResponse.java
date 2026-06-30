package com.meditrip.medicine.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.meditrip.medicine.domain.entity.MedicineReview;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MedicineReviewsResponse {

    private final Long id;
    private final String nickname;
    private final String authorGender;
    private final String authorAgeGroup;
    private final String authorRegion;
    private final Double rating;
    private final LocalDate createdAt;
    private final List<String> symptoms;

    @JsonProperty("isAuthor")
    private final boolean isAuthor;

    private final String review;

    public static MedicineReviewsResponse from(MedicineReview review, String nickname, UUID requestUserId) {
        return MedicineReviewsResponse.builder()
                .id(review.getId())
                .nickname(nickname)
                .authorGender(review.getGender())
                .authorAgeGroup(toAgeGroup(review.getAge()))
                .authorRegion(review.getCountry())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDate())
                .symptoms(toSymptomList(review.getSymptom()))
                .isAuthor(review.getUserId().equals(requestUserId))
                .review(review.getReview())
                .build();
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
