package com.meditrip.medicine.presentation;

import com.meditrip.common.jwt.CustomUserDetails;
import com.meditrip.common.response.CursorResponse;
import com.meditrip.medicine.application.MedicineReviewFacade;
import com.meditrip.medicine.application.dto.response.MedicineReviewsResponse;
import com.meditrip.medicine.presentation.dto.request.CreateMedicineReviewRequest;
import com.meditrip.medicine.presentation.dto.request.GetMedicineReviewsRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
    @Operation(summary = "약 리뷰 생성")
    public ResponseEntity<?> createMedicineReview(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                  @Valid @RequestBody CreateMedicineReviewRequest request,
                                                  @PathVariable Long medicineId) {
        UUID userId = UUID.fromString(userDetails.getUserId());
        Long reviewId = medicineReviewFacade.createReview(userId, medicineId, request.toApplicationRequest());
        return ResponseEntity.created(URI.create("/api/v1/reviews/" + reviewId)).build();
    }

    @GetMapping("/{medicineId}/reviews")
    @Operation(summary = "약 리뷰 조회")
    public ResponseEntity<CursorResponse<MedicineReviewsResponse>> getMedicineReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long medicineId,
            @ModelAttribute GetMedicineReviewsRequest request) {
        UUID userId = UUID.fromString(userDetails.getUserId());
        return ResponseEntity.ok(
                medicineReviewFacade.getReviews(userId, medicineId, request.toApplicationRequest(medicineId)));
    }

}
