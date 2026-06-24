package com.meditrip.map.application;

import com.meditrip.map.application.dto.request.SearchNearbyPlacesRequest;
import com.meditrip.map.application.dto.request.SearchNearbyPlacesRequest.BoundingBox;
import com.meditrip.map.application.dto.response.PlaceResponse;
import com.meditrip.map.domain.entity.enums.PlaceType;
import com.meditrip.map.infra.api.PharmacyInfoClient;
import com.meditrip.map.infra.api.dto.PharmacyApiResponse.PharmacyItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MapFacade {

    private static final int MAX_RADIUS_METERS = 50_000;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final String LANG_KO = "ko";
    private static final String LANG_EN = "en";

    private final HaversineSpatialService haversineSpatialService;
    private final GoogleSearchApi googleSearchApi;
    private final PharmacyInfoClient pharmacyInfoClient;
    private final PharmacyOpeningHoursService pharmacyOpeningHoursService;

    public List<PlaceResponse> searchNearbyPlaces(SearchNearbyPlacesRequest request) {
        BigDecimal centerLat;
        BigDecimal centerLng;
        int radius;

        if (request.hasBoundingBox()) {
            BoundingBox box = request.getBoundingBox();

            centerLat = box.getSwLat().add(box.getNeLat()).divide(BigDecimal.valueOf(2), 7, RoundingMode.HALF_UP);
            centerLng = box.getSwLng().add(box.getNeLng()).divide(BigDecimal.valueOf(2), 7, RoundingMode.HALF_UP);

            int diameter = haversineSpatialService.calculateDistanceMeters(box.getNeLat(), box.getNeLng(),
                    box.getSwLat(), box.getSwLng());
            radius = Math.min(diameter / 2, MAX_RADIUS_METERS);
        } else {
            centerLat = request.getLatitude();
            centerLng = request.getLongitude();
            radius = request.getRadius();
        }

        PlaceType type = resolveType(request.getType());
        LocalDateTime now = LocalDateTime.now(SEOUL);

        List<PlaceResponse> result = new ArrayList<>();

        if (type == PlaceType.PHARMACY || type == PlaceType.ALL) {
            result.addAll(searchPharmacies(centerLat, centerLng, radius, request.getKeyword(), now));
        }

        if (type == PlaceType.CONVENIENCE_STORE || type == PlaceType.ALL) {
            result.addAll(searchConvenienceStores(centerLat, centerLng, radius, request.getKeyword()));
        }

        return result.stream()
                .sorted(Comparator.comparing(PlaceResponse::isOpenNow).reversed()
                        .thenComparingInt(PlaceResponse::getDistance))
                .toList();
    }

    private List<PlaceResponse> searchPharmacies(BigDecimal centerLat, BigDecimal centerLng, int radius,
                                                 String keyword, LocalDateTime now) {
        // 한국어 - 공공 API 매칭용
        List<PlaceResponse> koList = googleSearchApi.searchNearbyPlaces(
                centerLat, centerLng, radius, PlaceType.PHARMACY, keyword, LANG_KO);
        // 영어 - 사용자 표시용
        List<PlaceResponse> enList = googleSearchApi.searchNearbyPlaces(
                centerLat, centerLng, radius, PlaceType.PHARMACY, keyword, LANG_EN);

        Map<String, PlaceResponse> koByPlaceId = koList.stream()
                .filter(p -> p.getPlaceId() != null)
                .collect(Collectors.toMap(PlaceResponse::getPlaceId, Function.identity(), (a, b) -> a));

        List<PlaceResponse> result = new ArrayList<>();
        for (PlaceResponse en : enList) {
            PlaceResponse ko = koByPlaceId.get(en.getPlaceId());
            result.add(toPharmacyResponse(en, ko, centerLat, centerLng, now));
        }
        return result;
    }

    private List<PlaceResponse> searchConvenienceStores(BigDecimal centerLat, BigDecimal centerLng, int radius, String keyword) {
        List<PlaceResponse> enList = googleSearchApi.searchNearbyPlaces(
                centerLat, centerLng, radius, PlaceType.CONVENIENCE_STORE, keyword, LANG_EN);

        List<PlaceResponse> result = new ArrayList<>();
        for (PlaceResponse en : enList) {
            result.add(toConvenienceStoreResponse(en, centerLat, centerLng));
        }
        return result;
    }

    private PlaceResponse toPharmacyResponse(PlaceResponse en, PlaceResponse ko, BigDecimal centerLat,
                                             BigDecimal centerLng, LocalDateTime now) {
        int distance = calculateDistance(en, centerLat, centerLng);

        PharmacyItem item = null;
        if (ko != null) {
            String province = extractProvince(ko.getAddress());
            String city = extractCity(ko.getAddress());
            item = pharmacyInfoClient.findByNameAndRegion(ko.getName(), province, city);
        }

        if (item == null) {
            return PlaceResponse.builder()
                    .placeId(en.getPlaceId())
                    .name(en.getName())
                    .distance(distance)
                    .distanceUnit("m")
                    .phoneNumber(en.getPhoneNumber())
                    .latitude(en.getLatitude())
                    .longitude(en.getLongitude())
                    .address(en.getAddress())
                    .type(PlaceType.PHARMACY)
                    .googleMapUrl(en.getGoogleMapUrl())
                    .isOpenNow(false)
                    .build();
        }

        return PlaceResponse.builder()
                .placeId(en.getPlaceId())
                .name(en.getName())
                .distance(distance)
                .distanceUnit("m")
                .phoneNumber(en.getPhoneNumber())
                .latitude(en.getLatitude())
                .longitude(en.getLongitude())
                .address(en.getAddress())
                .type(PlaceType.PHARMACY)
                .googleMapUrl(en.getGoogleMapUrl())
                .openingHours1(pharmacyOpeningHoursService.toRange(item.getDutyTime1s(), item.getDutyTime1c()))
                .openingHours2(pharmacyOpeningHoursService.toRange(item.getDutyTime2s(), item.getDutyTime2c()))
                .openingHours3(pharmacyOpeningHoursService.toRange(item.getDutyTime3s(), item.getDutyTime3c()))
                .openingHours4(pharmacyOpeningHoursService.toRange(item.getDutyTime4s(), item.getDutyTime4c()))
                .openingHours5(pharmacyOpeningHoursService.toRange(item.getDutyTime5s(), item.getDutyTime5c()))
                .openingHours6(pharmacyOpeningHoursService.toRange(item.getDutyTime6s(), item.getDutyTime6c()))
                .openingHours7(pharmacyOpeningHoursService.toRange(item.getDutyTime7s(), item.getDutyTime7c()))
                .isOpenNow(pharmacyOpeningHoursService.isOpenNow(item, now))
                .build();
    }

    private PlaceResponse toConvenienceStoreResponse(PlaceResponse en, BigDecimal centerLat, BigDecimal centerLng) {
        int distance = calculateDistance(en, centerLat, centerLng);

        return PlaceResponse.builder()
                .placeId(en.getPlaceId())
                .name(en.getName())
                .distance(distance)
                .distanceUnit("m")
                .openingHours1(en.getOpeningHours1())
                .openingHours2(en.getOpeningHours2())
                .openingHours3(en.getOpeningHours3())
                .openingHours4(en.getOpeningHours4())
                .openingHours5(en.getOpeningHours5())
                .openingHours6(en.getOpeningHours6())
                .openingHours7(en.getOpeningHours7())
                .phoneNumber(en.getPhoneNumber())
                .latitude(en.getLatitude())
                .longitude(en.getLongitude())
                .address(en.getAddress())
                .type(PlaceType.CONVENIENCE_STORE)
                .googleMapUrl(en.getGoogleMapUrl())
                .isOpenNow(en.isOpenNow())
                .build();
    }

    private int calculateDistance(PlaceResponse place, BigDecimal centerLat, BigDecimal centerLng) {
        return haversineSpatialService.calculateDistanceMeters(
                centerLat, centerLng,
                BigDecimal.valueOf(place.getLatitude()), BigDecimal.valueOf(place.getLongitude()));
    }

    private PlaceType resolveType(String type) {
        if (type == null) {
            return PlaceType.ALL;
        }
        return PlaceType.valueOf(type.toUpperCase());
    }

    private String extractProvince(String address) {
        if (address == null) {
            return null;
        }
        String cleaned = address.replace("대한민국", "").trim();
        String[] tokens = cleaned.split(" ");
        return tokens.length > 0 ? tokens[0] : null;
    }

    private String extractCity(String address) {
        if (address == null) {
            return null;
        }
        String cleaned = address.replace("대한민국", "").trim();
        String[] tokens = cleaned.split(" ");
        return tokens.length > 1 ? tokens[1] : null;
    }

}

