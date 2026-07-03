package com.meditrip.medicine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.medicine.domain.exception.MedicineNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TakeMedicineFacadeTakeTest {

    @InjectMocks
    private TakeMedicineFacade takeMedicineFacade;

    @Mock
    private TakeMedicineService takeMedicineService;

    @Mock
    private MedicineService medicineService;

    @DisplayName("약이 존재하면 복약 정보를 삽입할 수 있다.")
    @Test
    void shouldSaveIntakeInformationSuccessfully_whenMedicineExists() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 12L;

        given(medicineService.existsById(medicineId)).willReturn(true);
        given(takeMedicineService.take(medicineId, userId)).willReturn(11L);

        //when
        Long response = takeMedicineFacade.take(medicineId, userId);

        //then
        assertThat(response).isEqualTo(11L);
        verify(medicineService, times(1)).existsById(medicineId);
        verify(takeMedicineService, times(1)).take(medicineId, userId);
    }

    @DisplayName("약 정보가 존재하지 않으면 약 정보 삽입에 실패하고, 예외가 발생한다.")
    @Test
    void shouldThrowException_whenMedicineDoesNotExistDuringIntakeCreation() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 12L;

        given(medicineService.existsById(medicineId)).willReturn(false);

        //when, then
        assertThatThrownBy(() -> takeMedicineFacade.take(medicineId, userId))
                .isInstanceOf(MedicineNotFoundException.class)
                .hasMessage("Medicine Not Found");

        verify(medicineService, times(1)).existsById(medicineId);
        verify(takeMedicineService, never()).take(medicineId, userId);
    }

}
