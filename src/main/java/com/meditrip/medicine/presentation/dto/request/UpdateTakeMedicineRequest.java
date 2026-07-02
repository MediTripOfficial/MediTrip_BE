package com.meditrip.medicine.presentation.dto.request;

import com.meditrip.medicine.application.dto.request.UpdateTakeMedicineApplicationRequest;
import com.meditrip.medicine.domain.TakeMedicineCondition;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UpdateTakeMedicineRequest {

    @NotBlank(message = "Condition is required.")
    private final String condition;

    public UpdateTakeMedicineApplicationRequest toApplicationRequest(Long historyId) {
        return UpdateTakeMedicineApplicationRequest.builder()
                .condition(TakeMedicineCondition.valueOf(condition))
                .historyId(historyId)
                .build();
    }

}
