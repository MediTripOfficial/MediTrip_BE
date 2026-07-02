package com.meditrip.medicine.application.dto.request;

import com.meditrip.medicine.domain.TakeMedicineCondition;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateTakeMedicineApplicationRequest {

    private final TakeMedicineCondition condition;
    private final Long historyId;

}
