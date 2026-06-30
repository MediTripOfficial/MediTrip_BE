package com.meditrip.medicine.presentation;

import com.meditrip.common.jwt.CustomUserDetails;
import com.meditrip.medicine.application.SymptomRecommendationService;
import com.meditrip.medicine.application.dto.response.SymptomRecommendationResponse;
import com.meditrip.medicine.presentation.dto.request.SymptomRecommendationRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/symptoms")
public class SymptomRecommendationV1Controller {

    private final SymptomRecommendationService symptomRecommendationService;

    @PostMapping("/recommendations")
    public ResponseEntity<SymptomRecommendationResponse> recommend(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SymptomRecommendationRequest request) {
        UUID userId = UUID.fromString(userDetails.getUserId());
        return ResponseEntity.ok(symptomRecommendationService.recommend(request.toApplicationRequest(), userId));
    }

}
