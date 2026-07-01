package com.meditrip.medicine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.meditrip.medicine.application.dto.request.SymptomRecommendationApplicationRequest;
import com.meditrip.medicine.application.dto.response.SymptomRecommendationResponse;
import com.meditrip.medicine.application.dto.response.SymptomRecommendationResponse.MedicineSummaryResponse;
import com.meditrip.medicine.application.dto.response.SymptomRecommendationResponse.SimilarDrugResponse;
import com.meditrip.medicine.application.dto.response.SymptomRecommendationResponse.SymptomResponse;
import com.meditrip.medicine.domain.entity.Medicine;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import com.meditrip.medicine.domain.repository.SymptomMedicineQueryRepository;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.repository.UserAllergyRepository;
import com.meditrip.user.domain.repository.UserConditionRepository;
import com.meditrip.user.domain.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SymptomRecommendationServiceTest {

    @Mock
    private SymptomMedicineQueryRepository symptomMedicineQueryRepository;

    @Mock
    private UserConditionRepository userConditionRepository;

    @Mock
    private UserAllergyRepository userAllergyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MedicineReviewRepository medicineReviewRepository;

    @InjectMocks
    private SymptomRecommendationService symptomRecommendationService;

    private static Medicine medicine(Long id, String nameEn, String manufacturer, String countryCode,
                                     Boolean convenienceStore, Integer severityTier) {
        return Medicine.builder()
                .id(id)
                .nameEn(nameEn)
                .manufacturerEn(manufacturer)
                .countryCode(countryCode)
                .isConvenienceStore(convenienceStore)
                .severityTier(severityTier)
                .build();
    }

    private static MedicineReview review(Long id, Long medicineId, double rating) {
        return MedicineReview.builder()
                .id(id)
                .medicineId(medicineId)
                .rating(rating)
                .build();
    }

    @Test
    @DisplayName("같은 대분류에 형제 소분류가 없으면(Travel Fatigue) secondarySymptom은 null이다")
    void recommend_secondarySymptomIsNull_whenNoSiblingInSameMajorCategory() {
        //given
        UUID userId = UUID.randomUUID();
        SymptomRecommendationApplicationRequest request = SymptomRecommendationApplicationRequest.builder()
                .primaryCode(61) // TRAVEL_FATIGUE, 형제 소분류 없음
                .totalScore(20)
                .chatting("피곤해요")
                .build();

        given(userConditionRepository.findConditionNamesByUserId(userId)).willReturn(List.of());
        given(userAllergyRepository.findAllergyNamesByUserId(userId)).willReturn(List.of());
        given(userRepository.findById(userId)).willReturn(Optional.empty()); // 유저 없음 -> country=null

        Medicine vitaminC = medicine(5L, "Vitamin C", "Drug Co", "KR", true, 1);
        given(symptomMedicineQueryRepository.findMedicinesBySymptomCode(61)).willReturn(List.of(vitaminC));
        given(symptomMedicineQueryRepository.findIngredientNamesByMedicineIds(List.of(5L)))
                .willReturn(Map.of(5L, List.of("Vitamin C")));

        //when
        SymptomRecommendationResponse response = symptomRecommendationService.recommend(request, userId);

        //then
        assertThat(response.getResult().getSecondarySymptom()).isNull();
        assertThat(response.getResult().getPrimarySymptom().getMedicines()).hasSize(1);

        //secondarySymptom이 없으므로 findMedicinesBySymptomCode는 primaryCode(61)에 대해서만 호출됨
        then(symptomMedicineQueryRepository).should(times(1)).findMedicinesBySymptomCode(61);
        then(symptomMedicineQueryRepository).should(never()).findMedicinesBySymptomCode(eq(51));
    }

    @Test
    @DisplayName("매칭되는 약이 하나도 없으면 hashtag/similarDrugs/medicines가 전부 빈 배열로 내려간다")
    void recommend_returnsEmptyArrays_whenNoMedicinesMatchSymptomCode() {
        //given
        UUID userId = UUID.randomUUID();
        SymptomRecommendationApplicationRequest request = SymptomRecommendationApplicationRequest.builder()
                .primaryCode(51) // EYE_IRRITATION, 형제 없음
                .totalScore(10)
                .chatting("눈이 가려워요")
                .build();

        given(userConditionRepository.findConditionNamesByUserId(userId)).willReturn(List.of());
        given(userAllergyRepository.findAllergyNamesByUserId(userId)).willReturn(List.of());
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        given(symptomMedicineQueryRepository.findMedicinesBySymptomCode(51)).willReturn(List.of());
        given(symptomMedicineQueryRepository.findIngredientNamesByMedicineIds(List.of())).willReturn(Map.of());

        //when
        SymptomRecommendationResponse response = symptomRecommendationService.recommend(request, userId);

        //then
        SymptomResponse primary = response.getResult().getPrimarySymptom();
        assertThat(primary.getHashtag()).isEmpty();
        assertThat(primary.getSimilarDrugs()).isEmpty();
        assertThat(primary.getMedicines()).isEmpty();
        assertThat(response.getResult().getSecondarySymptom()).isNull();
    }

    @Test
    @DisplayName("기저질환(Diabetes)에 매칭되는 성분이 든 약은 추천 목록에서 제외된다")
    void recommend_excludesMedicine_whenIngredientMatchesRestrictedConditionIngredient() {
        //given
        UUID userId = UUID.randomUUID();
        SymptomRecommendationApplicationRequest request = SymptomRecommendationApplicationRequest.builder()
                .primaryCode(51)
                .totalScore(10)
                .chatting("당뇨가 있어요")
                .build();

        given(userConditionRepository.findConditionNamesByUserId(userId)).willReturn(List.of("Diabetes Type 2"));
        given(userAllergyRepository.findAllergyNamesByUserId(userId)).willReturn(List.of());
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        Medicine sugarSyrup = medicine(6L, "SugarSyrup", "Co", "KR", true, 1);
        Medicine antacid = medicine(7L, "AntacidTab", "Co", "KR", true, 1);

        given(symptomMedicineQueryRepository.findMedicinesBySymptomCode(51))
                .willReturn(List.of(sugarSyrup, antacid));
        given(symptomMedicineQueryRepository.findIngredientNamesByMedicineIds(List.of(6L, 7L)))
                .willReturn(Map.of(
                        6L, List.of("Sugar-containing syrup"),
                        7L, List.of("Antacid")));

        //when
        SymptomRecommendationResponse response = symptomRecommendationService.recommend(request, userId);

        //then
        List<MedicineSummaryResponse> medicines = response.getResult().getPrimarySymptom().getMedicines();
        assertThat(medicines).hasSize(1);
        assertThat(medicines.get(0).getId()).isEqualTo(7L);
        assertThat(medicines).extracting(MedicineSummaryResponse::getId).doesNotContain(6L);
    }

    @Test
    @DisplayName("알레르기에 매칭되는 성분이 든 약은 추천 목록에서 제외된다")
    void recommend_excludesMedicine_whenIngredientMatchesAllergyName() {
        //given
        UUID userId = UUID.randomUUID();
        SymptomRecommendationApplicationRequest request = SymptomRecommendationApplicationRequest.builder()
                .primaryCode(51)
                .totalScore(25)
                .chatting("페니실린 알레르기가 있어요")
                .build();

        given(userConditionRepository.findConditionNamesByUserId(userId)).willReturn(List.of());
        given(userAllergyRepository.findAllergyNamesByUserId(userId)).willReturn(List.of("Penicillin"));
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        Medicine penicillinOintment = medicine(8L, "PenicillinOintment", "Co", "KR", true, 1);
        Medicine povidone = medicine(9L, "PovidoneSol", "Co", "KR", true, 1);

        given(symptomMedicineQueryRepository.findMedicinesBySymptomCode(51))
                .willReturn(List.of(penicillinOintment, povidone));
        given(symptomMedicineQueryRepository.findIngredientNamesByMedicineIds(List.of(8L, 9L)))
                .willReturn(Map.of(
                        8L, List.of("Penicillin"),
                        9L, List.of("Povidone iodine")));

        //when
        SymptomRecommendationResponse response = symptomRecommendationService.recommend(request, userId);

        //then
        List<MedicineSummaryResponse> medicines = response.getResult().getPrimarySymptom().getMedicines();
        assertThat(medicines).hasSize(1);
        assertThat(medicines.get(0).getId()).isEqualTo(9L);
        assertThat(response.getResult().getSecondarySymptom()).isNull();
    }

    @ParameterizedTest(name = "totalScore={0} -> targetTier 약(id={1})이 가장 먼저 추천된다")
    @DisplayName("경계값: totalScore 구간(0~59/60~119/120~)에 따라 tier가 가까운 약이 먼저 정렬된다")
    @CsvSource({
            "0, 100",
            "59, 100",
            "60, 200",
            "119, 200",
            "120, 300",
            "99999, 300",
    })
    void recommend_sortsMedicinesByTierCloseness_acrossScoreBoundaries(int totalScore, long expectedFirstMedicineId) {
        //given
        UUID userId = UUID.randomUUID();
        SymptomRecommendationApplicationRequest request = SymptomRecommendationApplicationRequest.builder()
                .primaryCode(61) // 형제 없음 -> secondarySymptom 관련 스텁 불필요
                .totalScore(totalScore)
                .chatting("test")
                .build();

        given(userConditionRepository.findConditionNamesByUserId(userId)).willReturn(List.of());
        given(userAllergyRepository.findAllergyNamesByUserId(userId)).willReturn(List.of());
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        Medicine tier1Medicine = medicine(100L, "Tier1", "Co", "KR", true, 1);
        Medicine tier2Medicine = medicine(200L, "Tier2", "Co", "KR", true, 2);
        Medicine tier3Medicine = medicine(300L, "Tier3", "Co", "KR", true, 3);

        given(symptomMedicineQueryRepository.findMedicinesBySymptomCode(61))
                .willReturn(List.of(tier1Medicine, tier2Medicine, tier3Medicine));
        given(symptomMedicineQueryRepository.findIngredientNamesByMedicineIds(List.of(100L, 200L, 300L)))
                .willReturn(Map.of(100L, List.of("A"), 200L, List.of("B"), 300L, List.of("C")));

        //when
        SymptomRecommendationResponse response = symptomRecommendationService.recommend(request, userId);

        //then
        List<MedicineSummaryResponse> medicines = response.getResult().getPrimarySymptom().getMedicines();
        assertThat(medicines).hasSize(3);
        assertThat(medicines.get(0).getId()).isEqualTo(expectedFirstMedicineId);
    }

    @Test
    @DisplayName("같은 성분 조합을 쓰는 약은 similarDrugs에 대표 1개만 남고, medicines 전체 목록에는 다 남는다")
    void recommend_groupsSimilarDrugsByIngredientSet_keepingOnlyFirstRepresentative() {
        //given
        UUID userId = UUID.randomUUID();
        SymptomRecommendationApplicationRequest request = SymptomRecommendationApplicationRequest.builder()
                .primaryCode(61)
                .totalScore(10)
                .chatting("test")
                .build();

        given(userConditionRepository.findConditionNamesByUserId(userId)).willReturn(List.of());
        given(userAllergyRepository.findAllergyNamesByUserId(userId)).willReturn(List.of());
        given(userRepository.findById(userId))
                .willReturn(Optional.of(User.builder().id(userId).country("KR").build()));

        Medicine medA1 = medicine(1L, "BrandA1", "Co", "KR", true, 1);
        Medicine medA2 = medicine(2L, "BrandA2", "Co", "KR", true, 1);
        Medicine medB = medicine(3L, "BrandB", "Co", "KR", true, 1);

        given(symptomMedicineQueryRepository.findMedicinesBySymptomCode(61))
                .willReturn(List.of(medA1, medA2, medB));
        given(symptomMedicineQueryRepository.findIngredientNamesByMedicineIds(List.of(1L, 2L, 3L)))
                .willReturn(Map.of(
                        1L, List.of("X"),
                        2L, List.of("X"),
                        3L, List.of("Y")));

        //when
        SymptomRecommendationResponse response = symptomRecommendationService.recommend(request, userId);

        //then
        SymptomResponse primary = response.getResult().getPrimarySymptom();
        assertThat(primary.getMedicines()).hasSize(3); // 전체 목록은 3개 다 유지
        assertThat(primary.getSimilarDrugs()).hasSize(2); // 대표는 X그룹 1개 + Y그룹 1개
        assertThat(primary.getSimilarDrugs())
                .extracting(SimilarDrugResponse::getProductNameEng)
                .containsExactly("BrandA1", "BrandB");
    }

    @Test
    @DisplayName("사용자 국가와 일치하는 약이 하나도 없으면 null을 출력한다.")
    void recommend_fallsBackToAllMedicines_whenNoMedicineMatchesUserCountry() {
        //given
        UUID userId = UUID.randomUUID();
        SymptomRecommendationApplicationRequest request = SymptomRecommendationApplicationRequest.builder()
                .primaryCode(61)
                .totalScore(10)
                .chatting("test")
                .build();

        given(userConditionRepository.findConditionNamesByUserId(userId)).willReturn(List.of());
        given(userAllergyRepository.findAllergyNamesByUserId(userId)).willReturn(List.of());
        given(userRepository.findById(userId))
                .willReturn(Optional.of(User.builder().id(userId).country("KR").build()));

        Medicine usMedicine = medicine(1L, "USOnly", "Co", "US", true, 1);

        given(symptomMedicineQueryRepository.findMedicinesBySymptomCode(61)).willReturn(List.of(usMedicine));
        given(symptomMedicineQueryRepository.findIngredientNamesByMedicineIds(List.of(1L)))
                .willReturn(Map.of(1L, List.of("X")));

        //when
        SymptomRecommendationResponse response = symptomRecommendationService.recommend(request, userId);

        //then
        assertThat(response.getResult().getPrimarySymptom().getSimilarDrugs()).hasSize(0);
    }

    @Test
    @DisplayName("User.country가 빈 문자열이면 국가 필터를 적용하지 않는다.")
    void recommend_doesNotFilterByCountry_whenUserCountryIsBlank() {
        //given
        UUID userId = UUID.randomUUID();
        SymptomRecommendationApplicationRequest request = SymptomRecommendationApplicationRequest.builder()
                .primaryCode(61)
                .totalScore(10)
                .chatting("test")
                .build();

        given(userConditionRepository.findConditionNamesByUserId(userId)).willReturn(List.of());
        given(userAllergyRepository.findAllergyNamesByUserId(userId)).willReturn(List.of());
        given(userRepository.findById(userId))
                .willReturn(Optional.of(User.builder().id(userId).country("   ").build()));

        Medicine usMedicine = medicine(1L, "USOnly", "Co", "US", true, 1);
        Medicine krMedicine = medicine(2L, "KROnly", "Co", "KR", true, 1);

        given(symptomMedicineQueryRepository.findMedicinesBySymptomCode(61))
                .willReturn(List.of(usMedicine, krMedicine));
        given(symptomMedicineQueryRepository.findIngredientNamesByMedicineIds(List.of(1L, 2L)))
                .willReturn(Map.of(1L, List.of("X"), 2L, List.of("Y")));

        //when
        SymptomRecommendationResponse response = symptomRecommendationService.recommend(request, userId);

        //then
        assertThat(response.getResult().getPrimarySymptom().getSimilarDrugs()).hasSize(0);
    }

    @Test
    @DisplayName("isConvenienceStore가 null이면 NPE 없이 purchaseLocation은 pharmacy만 포함한다")
    void recommend_treatsNullConvenienceStoreAsPharmacyOnly() {
        //given
        UUID userId = UUID.randomUUID();
        SymptomRecommendationApplicationRequest request = SymptomRecommendationApplicationRequest.builder()
                .primaryCode(61)
                .totalScore(10)
                .chatting("test")
                .build();

        given(userConditionRepository.findConditionNamesByUserId(userId)).willReturn(List.of());
        given(userAllergyRepository.findAllergyNamesByUserId(userId)).willReturn(List.of());
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        Medicine unknownConvenience = medicine(1L, "Unknown", "Co", "KR", null, 1);

        given(symptomMedicineQueryRepository.findMedicinesBySymptomCode(61))
                .willReturn(List.of(unknownConvenience));
        given(symptomMedicineQueryRepository.findIngredientNamesByMedicineIds(List.of(1L)))
                .willReturn(Map.of(1L, List.of("X")));

        //when
        SymptomRecommendationResponse response = symptomRecommendationService.recommend(request, userId);

        //then
        assertThat(response.getResult().getPrimarySymptom().getMedicines().get(0).getPurchaseLocation())
                .containsExactly("pharmacy");
    }

    @Test
    @DisplayName("severityTier가 null인 약은 tier 1로 취급되어 정렬된다")
    void recommend_treatsNullSeverityTierAsTierOne() {
        //given
        UUID userId = UUID.randomUUID();
        SymptomRecommendationApplicationRequest request = SymptomRecommendationApplicationRequest.builder()
                .primaryCode(61)
                .totalScore(0) // targetTier = 1
                .chatting("test")
                .build();

        given(userConditionRepository.findConditionNamesByUserId(userId)).willReturn(List.of());
        given(userAllergyRepository.findAllergyNamesByUserId(userId)).willReturn(List.of());
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        Medicine nullTierMedicine = medicine(1L, "NullTier", "Co", "KR", true, null);
        Medicine tier3Medicine = medicine(2L, "Tier3", "Co", "KR", true, 3);

        given(symptomMedicineQueryRepository.findMedicinesBySymptomCode(61))
                .willReturn(List.of(nullTierMedicine, tier3Medicine));
        given(symptomMedicineQueryRepository.findIngredientNamesByMedicineIds(List.of(1L, 2L)))
                .willReturn(Map.of(1L, List.of("X"), 2L, List.of("Y")));

        //when
        SymptomRecommendationResponse response = symptomRecommendationService.recommend(request, userId);

        //then: targetTier=1이므로 null(=tier1 취급, 거리0)이 tier3(거리2)보다 먼저 나와야 함
        List<MedicineSummaryResponse> medicines = response.getResult().getPrimarySymptom().getMedicines();
        assertThat(medicines.get(0).getId()).isEqualTo(1L);
        assertThat(medicines.get(1).getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("유저가 존재하지 않으면(findById 결과 없음) 국가는 null로 취급되어 필터링하지 않는다")
    void recommend_treatsUserCountryAsNull_whenUserNotFoundInRepository() {
        //given
        UUID userId = UUID.randomUUID();
        SymptomRecommendationApplicationRequest request = SymptomRecommendationApplicationRequest.builder()
                .primaryCode(61)
                .totalScore(10)
                .chatting("test")
                .build();

        given(userConditionRepository.findConditionNamesByUserId(userId)).willReturn(List.of());
        given(userAllergyRepository.findAllergyNamesByUserId(userId)).willReturn(List.of());
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        Medicine usMedicine = medicine(1L, "USOnly", "Co", "US", true, 1);

        given(symptomMedicineQueryRepository.findMedicinesBySymptomCode(61)).willReturn(List.of(usMedicine));
        given(symptomMedicineQueryRepository.findIngredientNamesByMedicineIds(List.of(1L)))
                .willReturn(Map.of(1L, List.of("X")));

        //when
        SymptomRecommendationResponse response = symptomRecommendationService.recommend(request, userId);

        //then
        assertThat(response.getResult().getPrimarySymptom().getSimilarDrugs()).hasSize(0);
        then(userRepository).should(times(1)).findById(userId);
    }

    @Test
    @DisplayName("정의되지 않은 primaryCode면 IllegalArgumentException이 발생하고, 어떤 repository도 호출되지 않는다")
    void recommend_throwsIllegalArgumentException_whenPrimaryCodeIsInvalid_andNeverCallsRepositories() {
        //given
        UUID userId = UUID.randomUUID();
        SymptomRecommendationApplicationRequest request = SymptomRecommendationApplicationRequest.builder()
                .primaryCode(999)
                .totalScore(10)
                .chatting("test")
                .build();

        //when, then
        assertThatThrownBy(() -> symptomRecommendationService.recommend(request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");

        then(symptomMedicineQueryRepository).should(never())
                .findMedicinesBySymptomCode(org.mockito.ArgumentMatchers.anyInt());
        then(userConditionRepository).should(never()).findConditionNamesByUserId(org.mockito.ArgumentMatchers.any());
        then(userAllergyRepository).should(never()).findAllergyNamesByUserId(org.mockito.ArgumentMatchers.any());
        then(userRepository).should(never()).findById(org.mockito.ArgumentMatchers.any());
    }

}
