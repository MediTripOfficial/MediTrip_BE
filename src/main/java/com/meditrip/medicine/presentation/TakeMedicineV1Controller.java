package com.meditrip.medicine.presentation;

import com.meditrip.common.jwt.CustomUserDetails;
import com.meditrip.medicine.application.TakeMedicineFacade;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/medicines/me")
public class TakeMedicineV1Controller {

    private final TakeMedicineFacade takeMedicineFacade;

    @PostMapping
    public ResponseEntity<?> takeMedicine(@RequestParam Long medicineId,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUserId());
        Long historyId = takeMedicineFacade.take(medicineId, userId);
        return ResponseEntity.created(URI.create("/api/v1/medicines/me/" + historyId)).build();
    }

}
