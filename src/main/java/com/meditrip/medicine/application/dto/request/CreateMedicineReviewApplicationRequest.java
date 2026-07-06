package com.meditrip.medicine.application.dto.request;

import java.util.List;
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
    private List<String> images;

}
