package com.meditrip.medicine.application.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SymptomRecommendationResponse {

    private final ResultResponse result;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ResultResponse {
        private final SymptomResponse primarySymptom;
        private final SymptomResponse secondarySymptom;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SymptomResponse {
        private final String name;
        private final String description;
        private final List<String> hashtag;
        private final List<SimilarDrugResponse> similarDrugs;
        private final List<MedicineSummaryResponse> medicines;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SimilarDrugResponse {
        private final String productNameEng;
        private final String manufacturer;
        private final List<String> activeIngredientsEng;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MedicineSummaryResponse {
        private final Long id;
        private final String productNameEng;
        private final String manufacturer;
        private final List<String> activeIngredientsEng;
        private final List<String> purchaseLocation;
        private final String imageUrl;
        private final Double rating;
        private final Integer reviewCount;
    }

}
