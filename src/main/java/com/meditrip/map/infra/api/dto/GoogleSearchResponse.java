package com.meditrip.map.infra.api.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoogleSearchResponse {

    private PlaceDetailsResult result;

    @Getter
    @NoArgsConstructor
    public static class PlaceDetailsResult {
        private String name;
        private String formatted_address;
        private String formatted_phone_number;
        private Geometry geometry;
        private OpeningHours opening_hours;
        private String url;
    }

    @Getter
    @NoArgsConstructor
    public static class Geometry {
        private LocationLatLng location;
    }

    @Getter
    @NoArgsConstructor
    public static class LocationLatLng {
        private double lat;
        private double lng;
    }

    @Getter
    @NoArgsConstructor
    public static class OpeningHours {
        private List<String> weekday_text;
    }

}
