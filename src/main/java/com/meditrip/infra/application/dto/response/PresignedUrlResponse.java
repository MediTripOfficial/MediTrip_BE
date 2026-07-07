package com.meditrip.infra.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignedUrlResponse {

    private final String presignedUrl;
    private final String fileUrl;

}
