package com.meditrip.map.application.dto.response;

import com.meditrip.map.domain.entity.enums.PlaceType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaceResponse {

    private String placeId;
    private String name;
    private int distance;
    private String distanceUnit;
    private String openingHours1;
    private String openingHours2;
    private String openingHours3;
    private String openingHours4;
    private String openingHours5;
    private String openingHours6;
    private String openingHours7;
    private String phoneNumber;
    private double latitude;
    private double longitude;
    private String address;
    private PlaceType type;
    private String googleMapUrl;
    private boolean isOpenNow;

}
