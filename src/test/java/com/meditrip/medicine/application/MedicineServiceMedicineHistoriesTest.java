package com.meditrip.medicine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.medicine.application.dto.TakenMedicineInfo;
import com.meditrip.medicine.application.dto.response.MedicineHistoryResponse;
import com.meditrip.medicine.domain.entity.Hashtag.HashtagType;
import com.meditrip.medicine.domain.repository.MedicineHistoryQueryRepository;
import com.meditrip.medicine.domain.repository.MedicineQueryRepository;
import com.meditrip.medicine.domain.repository.MedicineRepository;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import java.time.Instant;
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
class MedicineServiceMedicineHistoriesTest {

    @InjectMocks
    private MedicineService medicineService;

    @Mock
    private MedicineQueryRepository medicineQueryRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private MedicineReviewRepository medicineReviewRepository;

    @Mock
    private MedicineHistoryQueryRepository medicineHistoryQueryRepository;

    @DisplayName("복용한 약 정보 조회에 성공한다.")
    @Test
    void shouldRetrieveTakenMedicineInformationSuccessfully() {
        //given
        UUID userId = UUID.randomUUID();

        Long medicineId1 = 1L;
        Long medicineId2 = 12L;
        Long medicineId3 = 15L;

        List<TakenMedicineInfo> takenMedicineInfo = List.of(
                TakenMedicineInfo.builder()
                        .medicineId(medicineId1)
                        .name("Tylenol Tab. 500 mg")
                        .date(Instant.now())
                        .image("https://www.s3.com/tylenol")
                        .build(),
                TakenMedicineInfo.builder()
                        .medicineId(medicineId2)
                        .name("Children’s Tylenol Suspension")
                        .date(Instant.now())
                        .image("https://www.s3.com/tylenol2")
                        .build(),
                TakenMedicineInfo.builder()
                        .medicineId(medicineId3)
                        .name("Mentholatum Lotion")
                        .date(Instant.now())
                        .image("https://www.s3.com/mentholatum-lotion")
                        .build()
        );

        PageRequest pageRequest = PageRequest.of(0, 9);
        Page<TakenMedicineInfo> historyPage = new PageImpl<>(takenMedicineInfo, pageRequest, takenMedicineInfo.size());

        given(medicineHistoryQueryRepository.findHistories(eq(userId), any(), any(), any()))
                .willReturn(historyPage);

        Map<Long, List<String>> hashtags = Map.of(
                medicineId1, List.of("#fever", "#headache", "#body_aches"),
                medicineId2, List.of("#child_fever", "#cold_pain", "#toothache"),
                medicineId3, List.of("#muscle_pain", "#back_pain", "#stiff_shoulder")
        );

        given(medicineQueryRepository.fetchHashtagsGrouped(anyList(), any())).willReturn(hashtags);

        //when
        Page<MedicineHistoryResponse> response =
                medicineService.findByMedicineHistoriesByUserId(userId, null, null, pageRequest);

        //then
        assertThat(response.getContent()).hasSize(3)
                .extracting("medicineId", "medicineName", "image", "symptoms")
                .containsExactlyInAnyOrder(
                        tuple(medicineId1, "Tylenol Tab. 500 mg", "https://www.s3.com/tylenol",
                                List.of("#fever", "#headache", "#body_aches")),
                        tuple(medicineId2, "Children’s Tylenol Suspension", "https://www.s3.com/tylenol2",
                                List.of("#child_fever", "#cold_pain", "#toothache")),
                        tuple(medicineId3, "Mentholatum Lotion", "https://www.s3.com/mentholatum-lotion",
                                List.of("#muscle_pain", "#back_pain", "#stiff_shoulder"))
                );

        verify(medicineHistoryQueryRepository, times(1)).findHistories(eq(userId), any(), any(), any());
        verify(medicineQueryRepository, times(1)).fetchHashtagsGrouped(anyList(), any());
    }

    @DisplayName("해시태그가 없는 약은 symptoms가 빈 리스트로 채워진다.")
    @Test
    void shouldHandleMissingHashtags_whenSomeMedicineHasNoHashtags() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 1L;

        List<TakenMedicineInfo> takenMedicineInfo = List.of(
                TakenMedicineInfo.builder()
                        .medicineId(medicineId)
                        .name("Tylenol")
                        .date(Instant.now())
                        .image("https://www.s3.com/tylenol")
                        .build()
        );

        PageRequest pageRequest = PageRequest.of(0, 9);
        Page<TakenMedicineInfo> historyPage = new PageImpl<>(takenMedicineInfo, pageRequest, 1);

        given(medicineHistoryQueryRepository.findHistories(eq(userId), any(), any(), any()))
                .willReturn(historyPage);
        given(medicineQueryRepository.fetchHashtagsGrouped(anyList(), any())).willReturn(Map.of());

        //when
        Page<MedicineHistoryResponse> response =
                medicineService.findByMedicineHistoriesByUserId(userId, null, null, pageRequest);

        //then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getSymptoms()).isEmpty();
    }

    @DisplayName("복용 이력이 없으면 빈 페이지를 반환하고, 해시태그 조회는 호출되지 않는다.")
    @Test
    void shouldReturnEmptyPageWithoutFetchingHashtags_whenNoHistoriesFound() {
        //given
        UUID userId = UUID.randomUUID();
        PageRequest pageRequest = PageRequest.of(0, 9);
        Page<TakenMedicineInfo> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);

        given(medicineHistoryQueryRepository.findHistories(eq(userId), any(), any(), any()))
                .willReturn(emptyPage);

        //when
        Page<MedicineHistoryResponse> response =
                medicineService.findByMedicineHistoriesByUserId(userId, null, null, pageRequest);

        //then
        assertThat(response.getContent()).isEmpty();
        verify(medicineQueryRepository, never()).fetchHashtagsGrouped(anyList(), any());
    }

    @DisplayName("같은 약을 여러 번 복용한 이력이 있어도 해시태그 조회는 distinct된 medicineId로 한 번만 요청한다.")
    @Test
    void shouldRequestHashtagsWithDistinctMedicineIds_whenSameMedicineTakenMultipleTimes() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 1L;

        List<TakenMedicineInfo> takenMedicineInfo = List.of(
                TakenMedicineInfo.builder().medicineId(medicineId).name("Tylenol").date(Instant.now()).image("img").build(),
                TakenMedicineInfo.builder().medicineId(medicineId).name("Tylenol").date(Instant.now().minusSeconds(1000)).image("img").build()
        );

        PageRequest pageRequest = PageRequest.of(0, 9);
        Page<TakenMedicineInfo> historyPage = new PageImpl<>(takenMedicineInfo, pageRequest, 2);

        given(medicineHistoryQueryRepository.findHistories(eq(userId), any(), any(), any()))
                .willReturn(historyPage);
        given(medicineQueryRepository.fetchHashtagsGrouped(List.of(medicineId), HashtagType.DISEASE))
                .willReturn(Map.of(medicineId, List.of("#fever")));

        //when
        Page<MedicineHistoryResponse> response =
                medicineService.findByMedicineHistoriesByUserId(userId, null, null, pageRequest);

        //then
        assertThat(response.getContent()).hasSize(2);
        verify(medicineQueryRepository, times(1)).fetchHashtagsGrouped(List.of(medicineId), HashtagType.DISEASE);
    }

}
