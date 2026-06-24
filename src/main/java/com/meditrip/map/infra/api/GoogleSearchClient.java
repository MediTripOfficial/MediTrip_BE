package com.meditrip.map.infra.api;

import com.meditrip.map.application.GoogleSearchApi;
import com.meditrip.map.application.dto.response.PlaceResponse;
import com.meditrip.map.domain.entity.enums.PlaceType;
import com.meditrip.map.infra.api.dto.GoogleFindPlaceResponse;
import com.meditrip.map.infra.api.dto.GoogleNearbySearchRequest;
import com.meditrip.map.infra.api.dto.GoogleNearbySearchResponse;
import com.meditrip.map.infra.api.dto.GoogleNearbySearchResponse.GooglePlace;
import com.meditrip.map.infra.api.dto.GoogleNearbySearchResponse.Period;
import com.meditrip.map.infra.api.dto.GoogleNearbySearchResponse.Point;
import com.meditrip.map.infra.api.dto.GoogleNearbySearchResponse.RegularOpeningHours;
import com.meditrip.map.infra.api.dto.GoogleSearchResponse;
import com.meditrip.map.infra.api.dto.GoogleSearchResponse.PlaceDetailsResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleSearchClient implements GoogleSearchApi {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${google.api.find-place.url-prefix}")
    private String searchUrlPrefix;

    @Value("${google.api.find-place.url-params}")
    private String searchUrlParams;

    @Value("${google.api.detail.url-prefix}")
    private String detailUrlPrefix;

    @Value("${google.api.detail.url-params}")
    private String detailUrlParams;

    @Value("${google.api.nearby-search.url}")
    private String nearbySearchUrl;

    @Value("${google.api.nearby-search.field-mask}")
    private String nearbySearchFieldMask;

    @Override
    public PlaceResponse getDetails(String title, String roadAddress) {
        String query = title + " " + roadAddress;
        String searchUrl = searchUrlPrefix + query + searchUrlParams + apiKey;

        ResponseEntity<GoogleFindPlaceResponse> response =
                restTemplate.getForEntity(searchUrl, GoogleFindPlaceResponse.class);

        String placeId = Optional.ofNullable(response.getBody())
                .map(GoogleFindPlaceResponse::getCandidates)
                .flatMap(candidates -> candidates.stream().findFirst())
                .map(GoogleFindPlaceResponse.Candidate::getPlace_id)
                .orElse(null);

        if (placeId == null) {
            log.warn("Find Place 검색 결과 없음. query: [{}]", query);
            return null;
        }

        String detailUrl = detailUrlPrefix + placeId + detailUrlParams + apiKey;

        ResponseEntity<GoogleSearchResponse> detailResponse =
                restTemplate.getForEntity(detailUrl, GoogleSearchResponse.class);

        PlaceDetailsResult result = Optional.ofNullable(detailResponse.getBody())
                .map(GoogleSearchResponse::getResult)
                .orElse(null);

        if (result == null) {
            log.warn("Place Details 조회 결과 없음. placeId: [{}]", placeId);
            return null;
        }

        return toPlaceDetailResponse(result, placeId);
    }

    private PlaceResponse toPlaceDetailResponse(PlaceDetailsResult result, String placeId) {
        List<String> weekdayText = result.getOpening_hours() != null
                ? result.getOpening_hours().getWeekday_text()
                : List.of();

        double lat = result.getGeometry() != null && result.getGeometry().getLocation() != null
                ? result.getGeometry().getLocation().getLat() : 0;
        double lng = result.getGeometry() != null && result.getGeometry().getLocation() != null
                ? result.getGeometry().getLocation().getLng() : 0;

        return PlaceResponse.builder()
                .placeId(placeId)
                .name(result.getName())
                .address(result.getFormatted_address())
                .phoneNumber(result.getFormatted_phone_number())
                .latitude(lat)
                .longitude(lng)
                .googleMapUrl(result.getUrl())
                .openingHours1(getOrNull(weekdayText, 0))
                .openingHours2(getOrNull(weekdayText, 1))
                .openingHours3(getOrNull(weekdayText, 2))
                .openingHours4(getOrNull(weekdayText, 3))
                .openingHours5(getOrNull(weekdayText, 4))
                .openingHours6(getOrNull(weekdayText, 5))
                .openingHours7(getOrNull(weekdayText, 6))
                .build();
    }

    @Override
    public List<PlaceResponse> searchNearbyPlaces(BigDecimal latitude, BigDecimal longitude, int radius,
                                                  PlaceType type, String keyword, String languageCode) {
        GoogleNearbySearchRequest requestBody = GoogleNearbySearchRequest.of(
                latitude.doubleValue(), longitude.doubleValue(), radius,
                toSearchKeyword(type, keyword), languageCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set("X-Goog-FieldMask", nearbySearchFieldMask);

        HttpEntity<GoogleNearbySearchRequest> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<GoogleNearbySearchResponse> response = restTemplate.postForEntity(
                nearbySearchUrl, entity, GoogleNearbySearchResponse.class);

        List<GooglePlace> places = Optional.ofNullable(response.getBody())
                .map(GoogleNearbySearchResponse::getPlaces)
                .orElse(List.of());

        return places.stream()
                .map(place -> toPlaceResponse(place, type))
                .toList();
    }

    private List<String> toSearchKeyword(PlaceType type, String keyword) {
        Stream<String> baseStream = switch (type) {
            case PHARMACY -> Stream.of("pharmacy");
            case CONVENIENCE_STORE -> Stream.of("convenience_store");
            case ALL -> Stream.of("pharmacy", "convenience_store");
        };

        if (keyword != null && !keyword.isBlank()) {
            return Stream.concat(baseStream, Stream.of(keyword)).toList();
        }

        return baseStream.toList();
    }

    private PlaceResponse toPlaceResponse(GooglePlace place, PlaceType type) {
        RegularOpeningHours hours = place.getRegularOpeningHours();
        String[] weekly = buildWeeklyHours(hours);  // index 0=월 ~ 6=일
        boolean openNow = hours != null && Boolean.TRUE.equals(hours.getOpenNow());

        return PlaceResponse.builder()
                .placeId(place.getId())
                .name(place.getDisplayName() != null ? place.getDisplayName().getText() : null)
                .address(place.getFormattedAddress())
                .latitude(place.getLocation() != null ? place.getLocation().getLatitude() : 0)
                .longitude(place.getLocation() != null ? place.getLocation().getLongitude() : 0)
                .phoneNumber(place.getNationalPhoneNumber())
                .googleMapUrl(place.getGoogleMapsUri())
                .type(type)
                .isOpenNow(openNow)
                .openingHours1(weekly[0])
                .openingHours2(weekly[1])
                .openingHours3(weekly[2])
                .openingHours4(weekly[3])
                .openingHours5(weekly[4])
                .openingHours6(weekly[5])
                .openingHours7(weekly[6])
                .build();
    }

    private String[] buildWeeklyHours(RegularOpeningHours hours) {
        String[] weekly = new String[7]; // [월,화,수,목,금,토,일]
        if (hours == null || hours.getPeriods() == null || hours.getPeriods().isEmpty()) {
            return weekly;
        }

        if (hours.getPeriods().size() == 1) {
            Period only = hours.getPeriods().get(0);
            if (only.getOpen() != null && only.getClose() == null) {
                for (int i = 0; i < 7; i++) {
                    weekly[i] = "00:00 ~ 24:00";
                }
                return weekly;
            }
        }

        for (Period period : hours.getPeriods()) {
            if (period.getOpen() == null) {
                continue;
            }
            int idx = toMondayBasedIndex(period.getOpen().getDay());
            String open = formatPoint(period.getOpen());
            String close = period.getClose() != null ? formatPoint(period.getClose()) : "24:00";
            weekly[idx] = open + " ~ " + close;
        }
        return weekly;
    }

    private String formatPoint(Point point) {
        return String.format("%02d:%02d", point.getHour(), point.getMinute());
    }

    private int toMondayBasedIndex(int googleDay) {
        return (googleDay + 6) % 7;
    }

    private String getOrNull(List<String> list, int index) {
        return index < list.size() ? list.get(index) : null;
    }

}
