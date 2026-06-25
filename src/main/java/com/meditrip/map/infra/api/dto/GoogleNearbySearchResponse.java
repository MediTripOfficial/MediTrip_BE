package com.meditrip.map.infra.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleNearbySearchResponse {

    private List<GooglePlace> places;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GooglePlace {
        private String id;
        private DisplayName displayName;
        private String formattedAddress;
        private Location location;
        private Double rating;
        private Integer userRatingCount;
        private String nationalPhoneNumber;
        private String googleMapsUri;
        private RegularOpeningHours regularOpeningHours;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DisplayName {
        private String text;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private double latitude;
        private double longitude;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RegularOpeningHours {
        private Boolean openNow;
        private List<Period> periods;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Period {
        private Point open;
        private Point close;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Point {
        private int day;    // 0=일요일, 6=토요일 (구글 기준)
        private int hour;
        private int minute;
    }

}
