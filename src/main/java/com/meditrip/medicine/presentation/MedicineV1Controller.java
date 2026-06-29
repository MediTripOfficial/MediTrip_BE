package com.meditrip.medicine.presentation;

import com.meditrip.common.jwt.CustomUserDetails;
import com.meditrip.medicine.application.MedicineService;
import com.meditrip.medicine.application.dto.response.MedicineResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/medicines")
public class MedicineV1Controller {

    private final MedicineService medicineService;

    @GetMapping("/{medicineId}")
    public ResponseEntity<MedicineResponse> getMedicineInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @PathVariable Long medicineId) {
        UUID userId = UUID.fromString(userDetails.getUserId());
        return ResponseEntity.ok(medicineService.getInfo(medicineId, userId));
    }

}
