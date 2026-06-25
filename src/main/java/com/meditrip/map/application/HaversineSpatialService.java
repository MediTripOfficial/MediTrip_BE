package com.meditrip.map.application;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class HaversineSpatialService {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public int calculateDistanceMeters(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        double lat1Val = lat1.doubleValue();
        double lng1Val = lng1.doubleValue();
        double lat2Val = lat2.doubleValue();
        double lng2Val = lng2.doubleValue();

        double lat1Rad = Math.toRadians(lat1Val);
        double lat2Rad = Math.toRadians(lat2Val);
        double deltaLat = Math.toRadians(lat2Val - lat1Val);
        double deltaLng = Math.toRadians(lng2Val - lng1Val);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (int) (EARTH_RADIUS_METERS * c);
    }

}
