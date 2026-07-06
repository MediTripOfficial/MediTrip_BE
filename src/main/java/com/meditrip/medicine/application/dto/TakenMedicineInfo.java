package com.meditrip.medicine.application.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TakenMedicineInfo {

    private Long medicineId;
    private String name;
    private Instant date;
    private String image;
    private List<String> hashtags;

    public TakenMedicineInfo(Long medicineId, String name, Instant date, String image) {
        this.medicineId = medicineId;
        this.name = name;
        this.date = date;
        this.image = image;
    }
}
