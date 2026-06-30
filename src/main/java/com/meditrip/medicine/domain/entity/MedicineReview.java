package com.meditrip.medicine.domain.entity;

import com.meditrip.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "medicine_review")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class MedicineReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long medicineId;

    @Column(columnDefinition = "TEXT")
    private String review;

    private Integer age; //TODO : 기획팀 답변 받으면 수정
    private Double height;
    private Double weight;

    @Column(nullable = false)
    private Double rating;
    private String gender;
    private String country;

    @Column(nullable = false)
    private Boolean isDeleted;

    @Column(nullable = false)
    private UUID userId;

    private String symptom; //TODO : 디자인 나오면 수정

    public static MedicineReview create(Long medicineId, String review, Integer age, Double height, Double weight, Double rating,
                                        String gender, String country, UUID userId, String symptom) {
        return MedicineReview.builder()
                .medicineId(medicineId)
                .review(review)
                .age(age)
                .height(height)
                .weight(weight)
                .rating(rating)
                .gender(gender)
                .country(country)
                .userId(userId)
                .symptom(symptom)
                .isDeleted(false)
                .build();
    }

}
