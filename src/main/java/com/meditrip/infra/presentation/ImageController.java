package com.meditrip.infra.presentation;

import com.meditrip.infra.application.S3PresignService;
import com.meditrip.infra.application.dto.response.PresignedUrlResponse;
import com.meditrip.infra.presentation.dto.request.PresignedUrlRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
public class ImageController {

    private final S3PresignService s3PresignService;

    @PostMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(@Valid @RequestBody PresignedUrlRequest request){
        return ResponseEntity.ok(s3PresignService.generatePresignedUrl(request.toApplicationRequest()));
    }

}
