package com.meditrip.map.presentation;

import com.meditrip.common.jwt.CustomUserDetails;
import com.meditrip.map.application.MapFacade;
import com.meditrip.map.application.dto.request.SearchNearbyPlacesRequest;
import com.meditrip.map.application.dto.request.SearchNearbyPlacesRequest.BoundingBox;
import com.meditrip.map.application.dto.response.PlaceResponse;
import com.meditrip.map.domain.entity.enums.PlaceType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/maps")
public class MapV1Controller {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    private final MapFacade mapFacade;

    @GetMapping
    public ResponseEntity<List<PlaceResponse>> searchNearbyPlaces(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String lat,
            @RequestParam(required = false) String lng,
            @RequestParam(defaultValue = "1000") int radius,
            @RequestParam(required = false) String swLat,
            @RequestParam(required = false) String swLng,
            @RequestParam(required = false) String neLat,
            @RequestParam(required = false) String neLng,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        UUID userId = UUID.fromString(userDetails.getUserId());

        Coordinates coordinates = parseUserCoordinates(lat, lng);
        BoundingBox boundingBox = parseBoundingBox(swLat, swLng, neLat, neLng);
        validateRadius(radius);
        validateType(type);

        SearchNearbyPlacesRequest request = SearchNearbyPlacesRequest.builder()
                .userId(userId)
                .latitude(coordinates.latitude())
                .longitude(coordinates.longitude())
                .radius(radius)
                .boundingBox(boundingBox)
                .type(type)
                .keyword(keyword)
                .build();

        return ResponseEntity.ok(mapFacade.searchNearbyPlaces(request));
    }

    private Coordinates parseUserCoordinates(String latitudeStr, String longitudeStr) {
        if (latitudeStr == null && longitudeStr == null) {
            return new Coordinates(null, null);
        }
        if (latitudeStr == null || longitudeStr == null) {
            throw new IllegalArgumentException("Both 'lat' and 'lng' must be provided together.");
        }
        return parseCoordinates(latitudeStr, longitudeStr);
    }

    private Coordinates parseCoordinates(String latitudeStr, String longitudeStr) {
        BigDecimal latitude = parseDecimal(latitudeStr, "lat");
        BigDecimal longitude = parseDecimal(longitudeStr, "lng");
        return new Coordinates(latitude, longitude);
    }

    private BoundingBox parseBoundingBox(String swLatStr, String swLngStr, String neLatStr, String neLngStr) {
        boolean allPresent = swLatStr != null && swLngStr != null && neLatStr != null && neLngStr != null;
        boolean allAbsent = swLatStr == null && swLngStr == null && neLatStr == null && neLngStr == null;

        if (allAbsent) {
            return null;
        }
        if (!allPresent) {
            throw new IllegalArgumentException(
                    "swLat, swLng, neLat, neLng must all be provided together or all omitted.");
        }

        Coordinates sw = parseCoordinates(swLatStr, swLngStr);
        Coordinates ne = parseCoordinates(neLatStr, neLngStr);

        return BoundingBox.builder()
                .swLat(sw.latitude())
                .swLng(sw.longitude())
                .neLat(ne.latitude())
                .neLng(ne.longitude())
                .build();
    }

    private BigDecimal parseDecimal(String value, String fieldName) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid format for '%s'. Expected a numeric value.".formatted(fieldName));
        }
    }

    private void validateRadius(int radius) {
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be a positive number.");
        }
    }

    private void validateType(String type) {
        if (type == null) {
            return;
        }
        try {
            PlaceType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported place type.");
        }
    }

    record Coordinates(BigDecimal latitude, BigDecimal longitude) {
        Coordinates {
            if (latitude != null && (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0)) {
                throw new IllegalArgumentException("Invalid latitude. Latitude must be between -90 and 90.");
            }
            if (longitude != null && (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0)) {
                throw new IllegalArgumentException("Invalid longitude. Longitude must be between -180 and 180.");
            }
        }
    }

}
