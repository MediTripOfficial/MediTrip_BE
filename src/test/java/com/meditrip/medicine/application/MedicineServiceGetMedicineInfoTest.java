package com.meditrip.medicine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.meditrip.medicine.application.dto.MedicineInfo;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.exception.MedicineNotFoundException;
import com.meditrip.medicine.domain.repository.MedicineQueryRepository;
import com.meditrip.medicine.domain.repository.MedicineRepository;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MedicineServiceGetMedicineInfoTest {

    @Mock
    private MedicineQueryRepository medicineQueryRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private MedicineReviewRepository medicineReviewRepository;

    @InjectMocks
    private MedicineService medicineService;

    @DisplayName("존재하는 약 ID로 조회하면 MedicineInfo를 그대로 반환한다")
    @Test
    void shouldReturnMedicineInfo_whenMedicineExistsWithValidId() {
        //given
        Long medicineId = 1L;
        UUID userId = UUID.randomUUID();

        MedicineInfo medicineInfo = MedicineInfo.builder()
                .id(medicineId)
                .name("Tylenol")
                .imageUrl("https://example.com/tylenol.png")
                .manufacturer("Johnson & Johnson")
                .ingredients(List.of(
                        MedicineInfo.Ingredient.builder()
                                .ingredientName("Acetaminophen")
                                .amount("500mg")
                                .build()))
                .diseaseHashtags(List.of("headache", "fever"))
                .efficacyHashtags(List.of("painRelief"))
                .isChildSafe(true)
                .isConvenienceStore(true)
                .purchaseLocation(List.of("store", "pharmacy"))
                .dosage("1 tablet")
                .interval("4-6 hours")
                .maxLimit("8 tablets/day")
                .caution("Avoid alcohol")
                .details("Pain reliever")
                .drugInteractions("None known")
                .seeDoctor("If symptoms persist over 3 days")
                .countryCode("US")
                .build();

        given(medicineQueryRepository.findInfoById(medicineId))
                .willReturn(Optional.of(medicineInfo));

        //when
        MedicineInfo result = medicineService.getInfo(medicineId, userId);

        //then
        assertThat(result.getId()).isEqualTo(medicineId);
        assertThat(result.getName()).isEqualTo("Tylenol");
        assertThat(result.getManufacturer()).isEqualTo("Johnson & Johnson");
        assertThat(result.getIngredients()).hasSize(1);
        assertThat(result.getIngredients().get(0).getIngredientName()).isEqualTo("Acetaminophen");
        assertThat(result.getDiseaseHashtags()).containsExactly("headache", "fever");
        assertThat(result.getEfficacyHashtags()).containsExactly("painRelief");
        assertThat(result.getIsConvenienceStore()).isTrue();
        assertThat(result.getPurchaseLocation()).containsExactly("store", "pharmacy");

        then(medicineQueryRepository).should().findInfoById(medicineId);
    }

    @DisplayName("존재하지 않는 약 ID로 조회하면 MedicineNotFoundException이 발생한다")
    @Test
    void shouldThrowMedicineNotFoundException_whenMedicineDoesNotExist() {
        //given
        Long medicineId = 999L;
        UUID userId = UUID.randomUUID();

        given(medicineQueryRepository.findInfoById(medicineId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> medicineService.getInfo(medicineId, userId))
                .isInstanceOf(MedicineNotFoundException.class);

        then(medicineQueryRepository).should().findInfoById(medicineId);
    }

    @DisplayName("리뷰가 존재하면 가장 최근(id가 가장 큰) 삭제되지 않은 리뷰를 topReview로 반환한다")
    @Test
    void shouldReturnLatestNonDeletedReview_whenReviewsExist() {
        //given
        Long medicineId = 1L;
        MedicineReview latestReview = MedicineReview.create(
                medicineId, "최고예요", 170.0, 60.0, 5.0, "Female", "KR", UUID.randomUUID(), "Headache", null);

        given(medicineReviewRepository.findTopByMedicineIdAndIsDeletedFalseOrderByIdDesc(medicineId))
                .willReturn(Optional.of(latestReview));

        //when
        MedicineReview result = medicineService.getMedicineTopReview(medicineId);

        //then
        assertThat(result).isEqualTo(latestReview);
    }

    @DisplayName("리뷰가 하나도 없으면(또는 전부 삭제됐으면) topReview로 null을 반환한다 (예외를 던지지 않음)")
    @Test
    void shouldReturnNull_whenNoReviewsExist() {
        //given
        Long medicineId = 1L;

        given(medicineReviewRepository.findTopByMedicineIdAndIsDeletedFalseOrderByIdDesc(medicineId))
                .willReturn(Optional.empty());

        //when
        MedicineReview result = medicineService.getMedicineTopReview(medicineId);

        //then
        assertThat(result).isNull();
    }

}
