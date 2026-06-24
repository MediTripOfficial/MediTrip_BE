package com.meditrip.map.infra.api.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoogleNearbySearchResponse {

    private List<GooglePlace> places;

    @Getter
    @NoArgsConstructor
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
    public static class DisplayName {
        private String text;
    }

    @Getter
    @NoArgsConstructor
    public static class Location {
        private double latitude;
        private double longitude;
    }

    @Getter
    @NoArgsConstructor
    public static class RegularOpeningHours {
        private List<String> weekdayDescriptions;
    }

}
