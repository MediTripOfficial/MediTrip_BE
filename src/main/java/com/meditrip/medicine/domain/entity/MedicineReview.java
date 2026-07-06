package com.meditrip.medicine.domain.entity;

import com.meditrip.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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

    @ElementCollection
    @CollectionTable(name = "medicine_review_image", joinColumns = @JoinColumn(name = "medicine_review_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    public static MedicineReview create(Long medicineId, String review, Double height, Double weight, Double rating,
                                        String gender, String country, UUID userId, String symptom,
                                        List<String> images) {
        return MedicineReview.builder()
                .medicineId(medicineId)
                .review(review)
                .height(height)
                .weight(weight)
                .rating(rating)
                .gender(gender)
                .country(country)
                .userId(userId)
                .symptom(symptom)
                .isDeleted(false)
                .images(images)
                .build();
    }

    public void delete() {
        this.isDeleted = true;
    }

}
