package com.meditrip.infra.presentation.dto.request;

import com.meditrip.infra.application.dto.request.PresignedUrlApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PresignedUrlRequest {

    @NotBlank
    private String fileName;

    @NotBlank
    private String contentType;

    public PresignedUrlApplicationRequest toApplicationRequest(){
        return PresignedUrlApplicationRequest.builder()
                .fileName(this.fileName)
                .contentType(this.contentType)
                .build();
    }

}
