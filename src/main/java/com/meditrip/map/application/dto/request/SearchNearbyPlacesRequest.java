package com.meditrip.map.application.dto.request;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchNearbyPlacesRequest {

    private UUID userId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private int radius;
    private BoundingBox boundingBox;
    private String type;
    private String keyword;

    public boolean hasBoundingBox() {
        return boundingBox != null;
    }

    @Getter
    @Builder
    public static class BoundingBox {
        private BigDecimal swLat;
        private BigDecimal swLng;
        private BigDecimal neLat;
        private BigDecimal neLng;
    }

}
