package com.meditrip.medicine.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.meditrip.medicine.application.dto.ReviewAuthorInfo;
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
    private final String userProfileImg;
    private final String authorGender;
    private final String authorAgeGroup;
    private final String authorRegion;
    private final Double rating;
    private final LocalDate createdAt;
    private final List<String> symptoms;

    @JsonProperty("isAuthor")
    private final boolean isAuthor;

    private final String review;
    private final List<String> images;

    public static MedicineReviewsResponse from(MedicineReview review, ReviewAuthorInfo reviewAuthorInfo,
                                               UUID requestUserId) {
        return MedicineReviewsResponse.builder()
                .id(review.getId())
                .nickname(reviewAuthorInfo.getNickname())
                .userProfileImg(reviewAuthorInfo.getProfileImg())
                .authorGender(review.getGender())
                .authorAgeGroup(toAgeGroup(reviewAuthorInfo.getAge()))
                .authorRegion(review.getCountry())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDate())
                .symptoms(toSymptomList(review.getSymptom()))
                .isAuthor(review.getUserId().equals(requestUserId))
                .review(review.getReview())
                .images(review.getImages())
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
