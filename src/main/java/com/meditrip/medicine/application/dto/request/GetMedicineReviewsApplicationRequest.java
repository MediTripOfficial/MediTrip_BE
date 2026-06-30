package com.meditrip.medicine.application.dto.request;

import com.meditrip.medicine.domain.ReviewSortType;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetMedicineReviewsApplicationRequest {

    private final Long medicineId;
    private final Long cursor;
    private final Integer size;
    private final ReviewSortType sort;
    private final List<String> gender;
    private final List<String> countries;
    private final List<String> symptoms;

}
