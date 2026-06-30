package com.meditrip.medicine.presentation.dto.request;

import com.meditrip.medicine.application.dto.request.GetMedicineReviewsApplicationRequest;
import com.meditrip.medicine.domain.ReviewSortType;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class GetMedicineReviewsRequest {

    private Long cursor;

    private Integer size = 15;

    private ReviewSortType sort = ReviewSortType.LATEST;

    private List<String> gender;

    private List<String> countries;

    private List<String> symptoms;

    public GetMedicineReviewsApplicationRequest toApplicationRequest(Long medicineId) {
        return GetMedicineReviewsApplicationRequest.builder()
                .medicineId(medicineId)
                .cursor(this.cursor)
                .size(this.size)
                .sort(this.sort)
                .gender(this.gender)
                .countries(this.countries)
                .symptoms(this.symptoms)
                .build();
    }

}
