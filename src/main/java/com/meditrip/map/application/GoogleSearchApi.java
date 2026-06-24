package com.meditrip.map.application;

import com.meditrip.map.application.dto.response.PlaceResponse;
import com.meditrip.map.domain.entity.enums.PlaceType;
import java.math.BigDecimal;
import java.util.List;

public interface GoogleSearchApi {

    PlaceResponse getDetails(String title, String roadAddress);

    List<PlaceResponse> searchNearbyPlaces(BigDecimal latitude, BigDecimal longitude, int radius, PlaceType type,
                                           String keyword, String languageCode);

}
