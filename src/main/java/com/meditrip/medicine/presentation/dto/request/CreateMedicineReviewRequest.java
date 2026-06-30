package com.meditrip.medicine.presentation.dto.request;

import com.meditrip.medicine.application.dto.request.CreateMedicineReviewApplicationRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class CreateMedicineReviewRequest {

    private String symptom; //TODO : 디자인 나오고 수정

    @NotBlank(message = "Review is required.")
    private String review; //TODO : 디자인 나오고 글자수 valid 설정

    @NotNull(message = "Rating is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Rating must be greater than or equal to 0.0.")
    @DecimalMax(value = "5.0", inclusive = true, message = "Rating must be less than or equal to 5.0.")
    @Digits(integer = 1, fraction = 1, message = "Rating must have up to 1 integer digit and 1 fractional digit.")
    private Double rating;

    public CreateMedicineReviewApplicationRequest toApplicationRequest(){
        return CreateMedicineReviewApplicationRequest.builder()
                .symptom(this.symptom)
                .review(this.review)
                .rating(this.rating)
                .build();
    }

}
