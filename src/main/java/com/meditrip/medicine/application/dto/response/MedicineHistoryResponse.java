package com.meditrip.medicine.application.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MedicineHistoryResponse {

    private Long medicineId;
    private String medicineName;
    private LocalDate date;
    private String image;
    private List<String> symptoms;

}
