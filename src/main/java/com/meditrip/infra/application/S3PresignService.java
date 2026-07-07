package com.meditrip.infra.application;

import com.meditrip.infra.application.dto.request.PresignedUrlApplicationRequest;
import com.meditrip.infra.application.dto.response.PresignedUrlResponse;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3PresignService {

    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.cdn-domain}")
    private String cdnDomain;

    private static final String DIR_PREFIX = "reviews/";
    private static final Duration EXPIRATION = Duration.ofMinutes(5);

    public PresignedUrlResponse generatePresignedUrl(PresignedUrlApplicationRequest request) {
        String key = DIR_PREFIX + UUID.randomUUID() + "-" + request.getFileName();

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(request.getContentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(EXPIRATION)
                .putObjectRequest(objectRequest)
                .build();

        String presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        String fileUrl = cdnDomain + "/" + key;

        return PresignedUrlResponse.builder()
                .fileUrl(fileUrl)
                .presignedUrl(presignedUrl)
                .build();
    }

}
