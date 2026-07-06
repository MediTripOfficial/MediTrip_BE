package com.meditrip.medicine.presentation;

import com.meditrip.common.jwt.CustomUserDetails;
import com.meditrip.common.response.PageResponse;
import com.meditrip.medicine.application.MedicineFacade;
import com.meditrip.medicine.application.dto.response.MedicineHistoryResponse;
import com.meditrip.medicine.application.dto.response.MedicineResponse;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/medicines")
public class MedicineV1Controller {

    private final MedicineFacade medicineFacade;

    @GetMapping("/{medicineId}")
    public ResponseEntity<MedicineResponse> getMedicineInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @PathVariable Long medicineId) {
        UUID userId = UUID.fromString(userDetails.getUserId());
        return ResponseEntity.ok(medicineFacade.getInfo(medicineId, userId));
    }

    @GetMapping("/histories")
    public ResponseEntity<PageResponse<MedicineHistoryResponse>> getMedicineHistories(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "9", required = false) int size,
            @RequestParam(value = "start-date", required = false) LocalDate startDate,
            @RequestParam(value = "end-date", required = false) LocalDate endDate) {
        UUID userId = UUID.fromString(userDetails.getUserId());
        return ResponseEntity.ok(
                PageResponse.from(medicineFacade.getMedicineHistories(userId, page, size, startDate, endDate)));
    }

}
