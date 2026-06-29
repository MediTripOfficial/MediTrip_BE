package com.meditrip.medicine.application.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SymptomRecommendationApplicationRequest {

    private Integer primaryCode;
    private Integer totalScore;
    private String chatting;

}
