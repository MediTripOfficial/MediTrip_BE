package com.meditrip.infra.application.dto.request;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PresignedUrlApplicationRequest {

    private final String fileName;
    private final String contentType;

}
