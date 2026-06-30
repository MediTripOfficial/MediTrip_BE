package com.meditrip.medicine.presentation.dto.request;

import com.meditrip.medicine.application.dto.request.SymptomRecommendationApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SymptomRecommendationRequest {

    @NotNull(message = "코드는 생략이 불가능합니다.")
    private Integer primaryCode;

    @NotNull(message = "점수는 생략이 불가능합니다.")
    private Integer totalScore;

    @NotBlank(message = "채팅 내용은 생략이 불가능합니다.")
    private String chatting;

    public SymptomRecommendationApplicationRequest toApplicationRequest(){
        return SymptomRecommendationApplicationRequest.builder()
                .primaryCode(this.primaryCode)
                .totalScore(this.totalScore)
                .chatting(this.chatting)
                .build();
    }

}
