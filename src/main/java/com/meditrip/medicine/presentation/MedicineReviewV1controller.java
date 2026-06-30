package com.meditrip.medicine.presentation;

import com.meditrip.common.jwt.CustomUserDetails;
import com.meditrip.medicine.application.MedicineReviewFacade;
import com.meditrip.medicine.presentation.dto.request.CreateMedicineReviewRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/medicines")
@RequiredArgsConstructor
public class MedicineReviewV1controller {

    private final MedicineReviewFacade medicineReviewFacade;

    @PostMapping("/{medicineId}/reviews")
    public ResponseEntity<?> createMedicineReview(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                  @Valid @RequestBody CreateMedicineReviewRequest request,
                                                  @PathVariable Long medicineId) {
        UUID userId = UUID.fromString(userDetails.getUserId());
        Long reviewId = medicineReviewFacade.createReview(userId, medicineId, request.toApplicationRequest());
        return ResponseEntity.created(URI.create("/api/v1/reviews/" + reviewId)).build();
    }

}
