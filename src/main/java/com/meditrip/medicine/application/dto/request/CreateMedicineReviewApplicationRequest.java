package com.meditrip.medicine.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CreateMedicineReviewApplicationRequest {

    private String symptom;
    private String review;
    private Double rating;

}
