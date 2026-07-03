package com.meditrip.medicine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.medicine.domain.IntakeSortType;
import com.meditrip.medicine.domain.entity.MedicineIntake;
import com.meditrip.medicine.domain.repository.MedicineIntakeLogRepository;
import com.meditrip.medicine.domain.repository.MedicineIntakeQueryRepository;
import com.meditrip.medicine.domain.repository.MedicineIntakeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class TakeMedicineServiceGetIntakesTest {

    @InjectMocks
    private TakeMedicineService takeMedicineService;

    @Mock
    private MedicineIntakeRepository medicineIntakeRepository;

    @Mock
    private MedicineIntakeLogRepository medicineIntakeLogRepository;

    @Mock
    private MedicineIntakeQueryRepository medicineIntakeQueryRepository;

    @DisplayName("유효한 sort 문자열이 주어지면 IntakeSortType으로 변환하여 쿼리 레포지토리에 위임한다.")
    @Test
    void shouldDelegateToQueryRepository_whenSortIsValid() {
        //given
        UUID userId = UUID.randomUUID();
        PageRequest pageRequest = PageRequest.of(0, 10);
        LocalDate firstStartDate = LocalDate.of(2024, 1, 1);
        LocalDate firstEndDate = LocalDate.of(2024, 12, 31);

        MedicineIntake intake = MedicineIntake.builder()
                .id(1L)
                .medicineId(10L)
                .userId(userId)
                .isDeleted(false)
                .build();
        Page<MedicineIntake> page = new PageImpl<>(List.of(intake), pageRequest, 1);

        given(medicineIntakeQueryRepository.findIntakes(
                userId, firstStartDate, firstEndDate, IntakeSortType.LATEST, pageRequest))
                .willReturn(page);

        //when
        Page<MedicineIntake> result = takeMedicineService.findByMedicineIntakesById(
                userId, pageRequest, firstStartDate, firstEndDate, "LATEST");

        //then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);

        verify(medicineIntakeQueryRepository, times(1))
                .findIntakes(userId, firstStartDate, firstEndDate, IntakeSortType.LATEST, pageRequest);
    }

    @DisplayName("날짜 필터가 없어도 null 그대로 쿼리 레포지토리에 전달된다.")
    @Test
    void shouldPassNullDateFilters_whenDatesAreNotProvided() {
        //given
        UUID userId = UUID.randomUUID();
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<MedicineIntake> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);

        given(medicineIntakeQueryRepository.findIntakes(
                eq(userId), eq(null), eq(null), eq(IntakeSortType.LATEST), any(PageRequest.class)))
                .willReturn(emptyPage);

        //when
        Page<MedicineIntake> result =
                takeMedicineService.findByMedicineIntakesById(userId, pageRequest, null, null, "LATEST");

        //then
        assertThat(result.getContent()).isEmpty();
        verify(medicineIntakeQueryRepository, times(1))
                .findIntakes(userId, null, null, IntakeSortType.LATEST, pageRequest);
    }

    @DisplayName("존재하지 않는 sort 값이 주어지면 IllegalArgumentException이 발생하고, 쿼리 레포지토리는 호출되지 않는다.")
    @Test
    void shouldThrowException_whenSortIsInvalid() {
        //given
        UUID userId = UUID.randomUUID();
        PageRequest pageRequest = PageRequest.of(0, 10);

        //when, then
        assertThatThrownBy(() -> takeMedicineService.findByMedicineIntakesById(
                userId, pageRequest, null, null, "INVALID_SORT"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(medicineIntakeQueryRepository, times(0))
                .findIntakes(any(), any(), any(), any(), any());
    }

}
