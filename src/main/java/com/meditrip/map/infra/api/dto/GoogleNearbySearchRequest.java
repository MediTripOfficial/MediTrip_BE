package com.meditrip.map.infra.api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoogleNearbySearchRequest {

    private List<String> includedTypes;
    private int maxResultCount;
    private String languageCode;
    private String regionCode;
    private LocationRestriction locationRestriction;

    public static GoogleNearbySearchRequest of(double lat, double lng, int radius, List<String> includedTypes,
                                               String languageCode) {
        return GoogleNearbySearchRequest.builder()
                .includedTypes(includedTypes)
                .maxResultCount(20)
                .languageCode(languageCode)
                .regionCode("KR")
                .locationRestriction(LocationRestriction.builder()
                        .circle(Circle.builder()
                                .center(Center.builder().latitude(lat).longitude(lng).build())
                                .radius((double) radius)
                                .build())
                        .build())
                .build();
    }

    @Getter
    @Builder
    public static class LocationRestriction {
        private Circle circle;
    }

    @Getter
    @Builder
    public static class Circle {
        private Center center;
        private double radius;
    }

    @Getter
    @Builder
    public static class Center {
        private double latitude;
        private double longitude;
    }

}
