package com.meditrip.map.application;

import com.meditrip.map.infra.api.dto.PharmacyApiResponse.PharmacyItem;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Service;

@Service
public class PharmacyOpeningHoursService {

    public String formatTime(String time) {
        if (time == null || time.length() != 4) {
            return null;
        }
        return time.substring(0, 2) + ":" + time.substring(2, 4);
    }

    public String toRange(String start, String close) {
        String s = formatTime(start);
        String c = formatTime(close);
        if (s == null || c == null) {
            return null;
        }
        return s + " ~ " + c;
    }

    public boolean isOpenNow(PharmacyItem item, LocalDateTime now) {
        DayOfWeek day = now.getDayOfWeek();
        String start = switch (day) {
            case MONDAY -> item.getDutyTime1s();
            case TUESDAY -> item.getDutyTime2s();
            case WEDNESDAY -> item.getDutyTime3s();
            case THURSDAY -> item.getDutyTime4s();
            case FRIDAY -> item.getDutyTime5s();
            case SATURDAY -> item.getDutyTime6s();
            case SUNDAY -> item.getDutyTime7s();
        };
        String close = switch (day) {
            case MONDAY -> item.getDutyTime1c();
            case TUESDAY -> item.getDutyTime2c();
            case WEDNESDAY -> item.getDutyTime3c();
            case THURSDAY -> item.getDutyTime4c();
            case FRIDAY -> item.getDutyTime5c();
            case SATURDAY -> item.getDutyTime6c();
            case SUNDAY -> item.getDutyTime7c();
        };

        if (start == null || close == null || start.length() != 4 || close.length() != 4) {
            return false;
        }

        LocalTime nowTime = now.toLocalTime();
        LocalTime openTime = LocalTime.of(Integer.parseInt(start.substring(0, 2)),
                Integer.parseInt(start.substring(2, 4)));
        LocalTime closeTime = LocalTime.of(Integer.parseInt(close.substring(0, 2)),
                Integer.parseInt(close.substring(2, 4)));

        return !nowTime.isBefore(openTime) && nowTime.isBefore(closeTime);
    }

}
