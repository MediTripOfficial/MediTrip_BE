package com.meditrip.medicine.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.medicine.domain.TakeMedicineCondition;
import com.meditrip.medicine.domain.entity.MedicineIntake;
import com.meditrip.medicine.domain.entity.MedicineIntakeLog;
import com.meditrip.medicine.domain.exception.MedicineIntakeNotFoundException;
import com.meditrip.medicine.domain.repository.MedicineIntakeLogRepository;
import com.meditrip.medicine.domain.repository.MedicineIntakeRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class TakeMedicineServiceFollowUpTest {

    @InjectMocks
    private TakeMedicineService takeMedicineService;

    @Mock
    private MedicineIntakeRepository medicineIntakeRepository;

    @Mock
    private MedicineIntakeLogRepository medicineIntakeLogRepository;

    @DisplayName("복약 기록이 존재하면 복약 정보 F/U에 성공한다.")
    @Test
    void shouldFollowUpIntakeSuccessfully_whenIntakeRecordExists() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineIntakeId = 1L;

        MedicineIntake medicineIntake = MedicineIntake.builder()
                .id(medicineIntakeId)
                .medicineId(1L)
                .isDeleted(false)
                .userId(userId)
                .build();

        given(medicineIntakeRepository.findById(medicineIntakeId)).willReturn(Optional.of(medicineIntake));

        //when
        takeMedicineService.applyFollowUp(medicineIntakeId, userId, TakeMedicineCondition.G);

        //then
        verify(medicineIntakeRepository, times(1)).findById(medicineIntakeId);
        verify(medicineIntakeLogRepository, times(1)).save(any(MedicineIntakeLog.class));
    }

    @DisplayName("복약 기록이 존재하지 않으면 복약 정보 F/U에 실패하고, 예외가 발생한다.")
    @Test
    void shouldThrowException_whenIntakeRecordDoesNotExistDuringFollowUp() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineIntakeId = 1L;

        given(medicineIntakeRepository.findById(medicineIntakeId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> takeMedicineService.applyFollowUp(medicineIntakeId, userId, TakeMedicineCondition.G))
                .isInstanceOf(MedicineIntakeNotFoundException.class)
                .hasMessage("Medicine Intake Not Found");

        verify(medicineIntakeRepository, times(1)).findById(medicineIntakeId);
        verify(medicineIntakeLogRepository, never()).save(any(MedicineIntakeLog.class));
    }

    @DisplayName("본인 소유가 아닌 복약 정보 F/U을 요청하면 복약 정보 F/U에 실패하고, 예외가 발생한다.")
    @Test
    void shouldThrowException_whenRequestingFollowUpOnOthersIntakeRecord() {
        //given
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        Long medicineIntakeId = 1L;

        MedicineIntake medicineIntake = MedicineIntake.builder()
                .id(medicineIntakeId)
                .medicineId(1L)
                .isDeleted(false)
                .userId(anotherUserId)
                .build();

        given(medicineIntakeRepository.findById(medicineIntakeId)).willReturn(Optional.of(medicineIntake));

        //when, then
        assertThatThrownBy(() -> takeMedicineService.applyFollowUp(medicineIntakeId, userId, TakeMedicineCondition.G))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have permission to access this medication record.");

        verify(medicineIntakeRepository, times(1)).findById(medicineIntakeId);
        verify(medicineIntakeLogRepository, never()).save(any(MedicineIntakeLog.class));
    }

}
