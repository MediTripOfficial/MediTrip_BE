package com.meditrip.medicine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.meditrip.medicine.application.dto.MedicineInfo;
import com.meditrip.medicine.application.dto.response.MedicineResponse;
import com.meditrip.medicine.domain.exception.MedicineNotFoundException;
import com.meditrip.medicine.domain.repository.MedicineQueryRepository;
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

    @InjectMocks
    private MedicineService medicineService;

    @Test
    @DisplayName("존재하는 약 ID로 조회하면 MedicineInfo를 MedicineResponse로 변환하여 반환한다")
    void shouldReturnMedicineResponse_whenMedicineExistsWithValidId() {
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
                .rating(null)
                .reviewCount(null)
                .topReview(null)
                .build();

        given(medicineQueryRepository.findInfoById(medicineId))
                .willReturn(Optional.of(medicineInfo));

        //when
        MedicineResponse response = medicineService.getInfo(medicineId, userId);

        //then
        assertThat(response.getId()).isEqualTo(medicineId);
        assertThat(response.getName()).isEqualTo("Tylenol");
        assertThat(response.getManufacturer()).isEqualTo("Johnson & Johnson");
        assertThat(response.getIngredients()).hasSize(1);
        assertThat(response.getIngredients().get(0).getIngredientName()).isEqualTo("Acetaminophen");
        assertThat(response.getIngredients().get(0).getAmount()).isEqualTo("500mg");
        assertThat(response.getDiseaseHashtags()).containsExactly("headache", "fever");
        assertThat(response.getEfficacyHashtags()).containsExactly("painRelief");
        assertThat(response.getIsConvenienceStore()).isTrue();
        assertThat(response.getPurchaseLocation()).containsExactly("store", "pharmacy");
        assertThat(response.getRating()).isNull();
        assertThat(response.getReviewCount()).isNull();
        assertThat(response.getTopReview()).isNull();

        then(medicineQueryRepository).should().findInfoById(medicineId);
    }

    @Test
    @DisplayName("존재하지 않는 약 ID로 조회하면 MedicineNotFoundException이 발생한다")
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

}
