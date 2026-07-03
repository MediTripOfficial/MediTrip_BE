package com.meditrip.medicine.application.dto.response;

import com.meditrip.medicine.application.dto.MedicineInfo;
import com.meditrip.medicine.domain.entity.MedicineIntake;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetMedicineIntakeResponse {

    private final Long intakeId;
    private final LocalDate firstTakenAt;
    private final MedicineResponse medicine;

    public static GetMedicineIntakeResponse of(MedicineIntake medicineIntake, MedicineInfo medicineInfo) {
        return GetMedicineIntakeResponse.builder()
                .intakeId(medicineIntake.getId())
                .firstTakenAt(LocalDate.ofInstant(medicineIntake.getFirstTakenAt(), ZoneId.of("Asia/Seoul")))
                .medicine(MedicineResponse.of(medicineInfo))
                .build();
    }

}
