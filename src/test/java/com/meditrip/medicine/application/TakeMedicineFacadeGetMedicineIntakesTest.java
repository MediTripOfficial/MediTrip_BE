package com.meditrip.medicine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.medicine.application.dto.MedicineInfo;
import com.meditrip.medicine.application.dto.response.GetMedicineIntakeResponse;
import com.meditrip.medicine.domain.entity.MedicineIntake;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
class TakeMedicineFacadeGetMedicineIntakesTest {

    @InjectMocks
    private TakeMedicineFacade takeMedicineFacade;

    @Mock
    private TakeMedicineService takeMedicineService;

    @Mock
    private MedicineService medicineService;

    private static MedicineIntake intake(Long id, Long medicineId, UUID userId) {
        return MedicineIntake.builder()
                .id(id)
                .medicineId(medicineId)
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(Instant.now())
                .build();
    }

    private static MedicineInfo medicineInfo(Long id, String name) {
        return MedicineInfo.builder()
                .id(id)
                .name(name)
                .manufacturer("Johnson & Johnson")
                .build();
    }

    @DisplayName("복약 이력을 조회하면 각 이력에 약 정보가 매핑되어 응답된다.")
    @Test
    void shouldReturnIntakesWithMappedMedicineInfo_whenIntakesExist() {
        //given
        UUID userId = UUID.randomUUID();
        MedicineIntake intake = intake(1L, 10L, userId);
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<MedicineIntake> intakePage = new PageImpl<>(List.of(intake), pageRequest, 1);

        given(takeMedicineService.findByMedicineIntakesById(
                eq(userId), any(PageRequest.class), any(), any(), eq("LATEST")))
                .willReturn(intakePage);

        given(medicineService.getMedicineInfoByIds(List.of(10L)))
                .willReturn(Map.of(10L, medicineInfo(10L, "Tylenol")));

        //when
        Page<GetMedicineIntakeResponse> response =
                takeMedicineFacade.getMedicineIntakes(userId, 0, 10, "LATEST", null, null);

        //then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getIntakeId()).isEqualTo(1L);
        assertThat(response.getContent().get(0).getMedicine().getId()).isEqualTo(10L);
        assertThat(response.getContent().get(0).getMedicine().getName()).isEqualTo("Tylenol");
        assertThat(response.getTotalElements()).isEqualTo(1);

        verify(medicineService, times(1)).getMedicineInfoByIds(List.of(10L));
    }

    @DisplayName("조회된 복약 이력이 없으면 빈 페이지를 반환하고, 약 정보 조회는 빈 리스트로 호출된다.")
    @Test
    void shouldReturnEmptyPage_whenNoIntakesFound() {
        //given
        UUID userId = UUID.randomUUID();
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<MedicineIntake> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);

        given(takeMedicineService.findByMedicineIntakesById(
                eq(userId), any(PageRequest.class), any(), any(), eq("LATEST")))
                .willReturn(emptyPage);

        given(medicineService.getMedicineInfoByIds(List.of())).willReturn(Map.of());

        //when
        Page<GetMedicineIntakeResponse> response =
                takeMedicineFacade.getMedicineIntakes(userId, 0, 10, "LATEST", null, null);

        //then
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();

        verify(medicineService, times(1)).getMedicineInfoByIds(List.of());
    }

    @DisplayName("같은 약을 여러 번 복용한 이력이 있어도 약 정보 조회는 distinct된 medicineId로 한 번만 요청한다.")
    @Test
    void shouldRequestMedicineInfoWithDistinctMedicineIds_whenSameMedicineTakenMultipleTimes() {
        //given
        UUID userId = UUID.randomUUID();
        MedicineIntake first = intake(2L, 10L, userId);
        MedicineIntake second = intake(1L, 10L, userId);
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<MedicineIntake> intakePage = new PageImpl<>(List.of(first, second), pageRequest, 2);

        given(takeMedicineService.findByMedicineIntakesById(
                eq(userId), any(PageRequest.class), any(), any(), eq("LATEST")))
                .willReturn(intakePage);

        given(medicineService.getMedicineInfoByIds(List.of(10L)))
                .willReturn(Map.of(10L, medicineInfo(10L, "Tylenol")));

        //when
        Page<GetMedicineIntakeResponse> response =
                takeMedicineFacade.getMedicineIntakes(userId, 0, 10, "LATEST", null, null);

        //then
        assertThat(response.getContent()).hasSize(2);
        verify(medicineService, times(1)).getMedicineInfoByIds(List.of(10L));
    }

    @DisplayName("이력에 매핑되는 약 정보를 찾을 수 없어도(약 삭제 등) 예외 없이 medicine 필드가 빈 값으로 채워져 응답된다.")
    @Test
    void shouldReturnEmptyMedicineResponse_whenMedicineInfoNotFound() {
        //given
        UUID userId = UUID.randomUUID();
        MedicineIntake intake = intake(1L, 999L, userId);
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<MedicineIntake> intakePage = new PageImpl<>(List.of(intake), pageRequest, 1);

        given(takeMedicineService.findByMedicineIntakesById(
                eq(userId), any(PageRequest.class), any(), any(), eq("LATEST")))
                .willReturn(intakePage);

        given(medicineService.getMedicineInfoByIds(List.of(999L))).willReturn(Map.of());

        //when
        Page<GetMedicineIntakeResponse> response =
                takeMedicineFacade.getMedicineIntakes(userId, 0, 10, "LATEST", null, null);

        //then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getMedicine()).isNotNull();
        assertThat(response.getContent().get(0).getMedicine().getId()).isNull();
        assertThat(response.getContent().get(0).getMedicine().getName()).isNull();
    }

    @DisplayName("firstStartDate, firstEndDate, sort 파라미터를 그대로 서비스 계층에 전달한다.")
    @Test
    void shouldPassFilterParamsToServiceAsIs() {
        //given
        UUID userId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        PageRequest pageRequest = PageRequest.of(1, 5);
        Page<MedicineIntake> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);

        given(takeMedicineService.findByMedicineIntakesById(
                eq(userId), eq(pageRequest), eq(startDate), eq(endDate), eq("LATEST")))
                .willReturn(emptyPage);
        given(medicineService.getMedicineInfoByIds(List.of())).willReturn(Map.of());

        //when
        takeMedicineFacade.getMedicineIntakes(userId, 1, 5, "LATEST", startDate, endDate);

        //then
        verify(takeMedicineService, times(1))
                .findByMedicineIntakesById(userId, pageRequest, startDate, endDate, "LATEST");
    }

}
