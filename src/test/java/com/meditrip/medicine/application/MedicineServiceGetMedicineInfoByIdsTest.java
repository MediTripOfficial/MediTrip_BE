package com.meditrip.medicine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.meditrip.medicine.application.dto.MedicineInfo;
import com.meditrip.medicine.domain.repository.MedicineQueryRepository;
import com.meditrip.medicine.domain.repository.MedicineRepository;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MedicineServiceGetMedicineInfoByIdsTest {

    @InjectMocks
    private MedicineService medicineService;

    @Mock
    private MedicineQueryRepository medicineQueryRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private MedicineReviewRepository medicineReviewRepository;

    @DisplayName("medicineId 목록을 넘기면 쿼리 레포지토리 조회 결과를 그대로 반환한다.")
    @Test
    void shouldReturnMedicineInfoMap_whenMedicineIdsProvided() {
        //given
        List<Long> medicineIds = List.of(1L, 2L);
        MedicineInfo medicineInfo1 = MedicineInfo.builder().id(1L).name("Tylenol").build();
        MedicineInfo medicineInfo2 = MedicineInfo.builder().id(2L).name("Advil").build();
        Map<Long, MedicineInfo> expected = Map.of(1L, medicineInfo1, 2L, medicineInfo2);

        given(medicineQueryRepository.findAllByIds(medicineIds)).willReturn(expected);

        //when
        Map<Long, MedicineInfo> result = medicineService.getMedicineInfoByIds(medicineIds);

        //then
        assertThat(result).hasSize(2);
        assertThat(result.get(1L).getName()).isEqualTo("Tylenol");
        assertThat(result.get(2L).getName()).isEqualTo("Advil");

        then(medicineQueryRepository).should().findAllByIds(medicineIds);
    }

    @DisplayName("빈 medicineId 목록을 넘기면 쿼리 레포지토리에도 빈 목록 그대로 전달된다.")
    @Test
    void shouldDelegateEmptyList_whenMedicineIdsIsEmpty() {
        //given
        given(medicineQueryRepository.findAllByIds(List.of())).willReturn(Map.of());

        //when
        Map<Long, MedicineInfo> result = medicineService.getMedicineInfoByIds(List.of());

        //then
        assertThat(result).isEmpty();
        then(medicineQueryRepository).should().findAllByIds(List.of());
    }

}
