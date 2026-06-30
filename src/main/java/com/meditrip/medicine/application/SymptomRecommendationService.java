package com.meditrip.medicine.application;

import com.meditrip.medicine.application.dto.request.SymptomRecommendationApplicationRequest;
import com.meditrip.medicine.application.dto.response.SymptomRecommendationResponse;
import com.meditrip.medicine.application.dto.response.SymptomRecommendationResponse.MedicineSummaryResponse;
import com.meditrip.medicine.application.dto.response.SymptomRecommendationResponse.ResultResponse;
import com.meditrip.medicine.application.dto.response.SymptomRecommendationResponse.SimilarDrugResponse;
import com.meditrip.medicine.application.dto.response.SymptomRecommendationResponse.SymptomResponse;
import com.meditrip.medicine.domain.ContraindicationPolicy;
import com.meditrip.medicine.domain.SymptomCode;
import com.meditrip.medicine.domain.entity.Medicine;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import com.meditrip.medicine.domain.repository.SymptomMedicineQueryRepository;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.repository.UserAllergyRepository;
import com.meditrip.user.domain.repository.UserConditionRepository;
import com.meditrip.user.domain.repository.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SymptomRecommendationService {

    private static final int TIER_2_THRESHOLD = 60;
    private static final int TIER_3_THRESHOLD = 120;

    private final SymptomMedicineQueryRepository symptomMedicineQueryRepository;
    private final UserConditionRepository userConditionRepository;
    private final UserAllergyRepository userAllergyRepository;
    private final UserRepository userRepository;
    private final MedicineReviewRepository medicineReviewRepository;

    @Transactional(readOnly = true)
    public SymptomRecommendationResponse recommend(SymptomRecommendationApplicationRequest request, UUID userId) {
        SymptomCode primaryCode = SymptomCode.fromCode(request.getPrimaryCode());
        int totalScore = request.getTotalScore();

        log.info("증상 추천 요청. userId=[{}], primaryCode=[{}], totalScore=[{}]", userId, request.getPrimaryCode(),
                totalScore);

        List<String> conditionNames = userConditionRepository.findConditionNamesByUserId(userId);
        List<String> allergyNames = userAllergyRepository.findAllergyNamesByUserId(userId);
        Set<String> restrictedIngredients =
                ContraindicationPolicy.resolveRestrictedIngredients(conditionNames, allergyNames);

        String userCountry = resolveUserCountry(userId);

        SymptomResponse primarySymptom =
                buildSymptomResponse(primaryCode, totalScore, restrictedIngredients, userCountry);

        SymptomCode secondaryCode = resolveClosestSiblingByScore(primaryCode, totalScore);
        SymptomResponse secondarySymptom = secondaryCode == null
                ? null
                : buildSymptomResponse(secondaryCode, totalScore, restrictedIngredients, userCountry);

        return SymptomRecommendationResponse.builder()
                .result(ResultResponse.builder()
                        .primarySymptom(primarySymptom)
                        .secondarySymptom(secondarySymptom)
                        .build())
                .build();
    }

    private String resolveUserCountry(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getCountry)
                .orElse(null);
    }

    private SymptomResponse buildSymptomResponse(SymptomCode symptomCode, int totalScore,
                                                 Set<String> restrictedIngredients, String userCountry) {
        MedicineBundle bundle = loadSafeMedicines(symptomCode.getCode(), restrictedIngredients);

        List<Medicine> sortedMedicines = sortByTierCloseness(bundle.medicines(), totalScore);

        List<MedicineSummaryResponse> medicineResponses = sortedMedicines.stream()
                .map(medicine -> toMedicineSummary(
                        medicine, bundle.ingredientsByMedicineId().getOrDefault(medicine.getId(), List.of())))
                .toList();

        List<Medicine> medicinesForSimilarDrugs = filterByCountry(sortedMedicines, userCountry);
        List<SimilarDrugResponse> similarDrugs =
                buildSimilarDrugs(medicinesForSimilarDrugs, bundle.ingredientsByMedicineId());

        return SymptomResponse.builder()
                .name(symptomCode.getMajorNameEn())
                .description(symptomCode.getSubNameEn())
                .hashtag(new ArrayList<>()) //TODO : 기획팀 답변 받은 후 수정
                .similarDrugs(similarDrugs)
                .medicines(medicineResponses)
                .build();
    }

    private List<Medicine> filterByCountry(List<Medicine> medicines, String userCountry) {
        if (userCountry == null || userCountry.isBlank()) {
            return medicines;
        }

        List<Medicine> filtered = medicines.stream()
                .filter(medicine -> userCountry.equalsIgnoreCase(medicine.getCountryCode()))
                .toList();

        return filtered.isEmpty() ? medicines : filtered;
    }

    private SymptomCode resolveClosestSiblingByScore(SymptomCode primaryCode, int totalScore) {
        return Arrays.stream(SymptomCode.values())
                .filter(symptomCode -> symptomCode != primaryCode)
                .filter(symptomCode -> symptomCode.getMajorNameEn().equals(primaryCode.getMajorNameEn()))
                .min(Comparator.comparingInt(symptomCode -> Math.abs(symptomCode.getBaseScore() - totalScore)))
                .orElse(null);
    }

    private MedicineBundle loadSafeMedicines(int symptomCode, Set<String> restrictedIngredients) {
        List<Medicine> medicines = symptomMedicineQueryRepository.findMedicinesBySymptomCode(symptomCode);

        List<Long> medicineIds = medicines.stream().map(Medicine::getId).toList();
        Map<Long, List<String>> ingredientsByMedicineId =
                symptomMedicineQueryRepository.findIngredientNamesByMedicineIds(medicineIds);

        List<Medicine> safeMedicines = medicines.stream()
                .filter(medicine -> !ContraindicationPolicy.containsRestrictedIngredient(
                        ingredientsByMedicineId.getOrDefault(medicine.getId(), List.of()),
                        restrictedIngredients))
                .toList();

        if (safeMedicines.size() != medicines.size()) {
            log.info("기저질환/알레르기로 제외된 약 {}개. symptomCode=[{}]",
                    medicines.size() - safeMedicines.size(), symptomCode);
        }

        return new MedicineBundle(safeMedicines, ingredientsByMedicineId);
    }

    private List<Medicine> sortByTierCloseness(List<Medicine> medicines, int totalScore) {
        int targetTier = resolveTargetTier(totalScore);
        return medicines.stream()
                .sorted(Comparator
                        .comparingInt((Medicine medicine) -> tierDistance(medicine, targetTier))
                        .thenComparing(Medicine::getId))
                .toList();
    }

    private int resolveTargetTier(int totalScore) {
        if (totalScore >= TIER_3_THRESHOLD) {
            return 3;
        }
        if (totalScore >= TIER_2_THRESHOLD) {
            return 2;
        }
        return 1;
    }

    private int tierDistance(Medicine medicine, int targetTier) {
        int tier = medicine.getSeverityTier() == null ? 1 : medicine.getSeverityTier();
        return Math.abs(tier - targetTier);
    }

    private MedicineSummaryResponse toMedicineSummary(Medicine medicine, List<String> ingredientNames) {
        List<MedicineReview> medicineReviews = medicineReviewRepository.findAllByMedicineId(medicine.getId());

        double rating = medicineReviews.isEmpty() ? 0.0
                : medicineReviews.stream().mapToDouble(MedicineReview::getRating).average().orElse(0.0);

        boolean isConvenienceStore = Boolean.TRUE.equals(medicine.getIsConvenienceStore());
        List<String> purchaseLocation = isConvenienceStore
                ? List.of("store", "pharmacy")
                : List.of("pharmacy");

        return MedicineSummaryResponse.builder()
                .id(medicine.getId())
                .productNameEng(medicine.getNameEn())
                .manufacturer(medicine.getManufacturerEn())
                .activeIngredientsEng(ingredientNames)
                .purchaseLocation(purchaseLocation)
                .imageUrl(medicine.getImageUrl())
                .rating(rating)
                .reviewCount(medicineReviews.size())
                .build();
    }

    private List<SimilarDrugResponse> buildSimilarDrugs(List<Medicine> medicines,
                                                        Map<Long, List<String>> ingredientsByMedicineId) {
        Map<List<String>, Medicine> representativeByIngredientSet = new LinkedHashMap<>();
        for (Medicine medicine : medicines) {
            List<String> ingredients = ingredientsByMedicineId.getOrDefault(medicine.getId(), List.of());
            representativeByIngredientSet.putIfAbsent(ingredients, medicine);
        }

        return representativeByIngredientSet.entrySet().stream()
                .map(entry -> SimilarDrugResponse.builder()
                        .productNameEng(entry.getValue().getNameEn())
                        .manufacturer(entry.getValue().getManufacturerEn())
                        .activeIngredientsEng(entry.getKey())
                        .build())
                .toList();
    }

    private record MedicineBundle(List<Medicine> medicines, Map<Long, List<String>> ingredientsByMedicineId) {
    }

}
