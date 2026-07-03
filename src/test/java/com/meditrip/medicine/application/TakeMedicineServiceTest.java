package com.meditrip.medicine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.medicine.domain.entity.MedicineIntake;
import com.meditrip.medicine.domain.repository.MedicineIntakeRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TakeMedicineServiceTest {

    @InjectMocks
    private TakeMedicineService takeMedicineService;

    @Mock
    private MedicineIntakeRepository medicineIntakeRepository;

    @DisplayName("약 정보 삽입에 성공한다.")
    @Test
    void shouldCreateMedicineSuccessfully() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 12L;

        MedicineIntake mockMedicineIntake = mock(MedicineIntake.class);
        given(medicineIntakeRepository.save(any(MedicineIntake.class))).willReturn(mockMedicineIntake);
        given(mockMedicineIntake.getId()).willReturn(11L);

        //when
        Long response = takeMedicineService.take(medicineId, userId);

        //then
        assertThat(response).isEqualTo(11L);
        verify(medicineIntakeRepository, times(1)).save(any(MedicineIntake.class));
    }

}
