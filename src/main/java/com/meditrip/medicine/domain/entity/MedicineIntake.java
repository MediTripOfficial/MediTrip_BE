package com.meditrip.medicine.domain.entity;

import com.meditrip.medicine.domain.TakeMedicineCondition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "medicine_intakes")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class MedicineIntake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long medicineId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private boolean isDeleted;

    private Instant firstTakenAt;
    private Instant lastTakenAt;
    private TakeMedicineCondition lastCondition;

    public static MedicineIntake create(Long medicineId, UUID userId){
        return MedicineIntake.builder()
                .medicineId(medicineId)
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(Instant.now())
                .build();
    }

}
