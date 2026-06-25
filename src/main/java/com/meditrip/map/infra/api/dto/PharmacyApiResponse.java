package com.meditrip.map.infra.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PharmacyApiResponse {

    private Response response;

    @Getter
    @NoArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private Body body;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private Items items;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        private List<PharmacyItem> item;

        public PharmacyItem first() {
            return (item == null || item.isEmpty()) ? null : item.get(0);
        }
    }

    @Getter
    @NoArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PharmacyItem {
        private String dutyName;
        private String dutyAddr;
        private String dutyEtc;
        private String dutyTel1;
        private Double wgs84Lat;
        private Double wgs84Lon;
        private String dutyTime1s;   // 월 시작 (ex. "0900")
        private String dutyTime1c;   // 월 종료
        private String dutyTime2s;
        private String dutyTime2c;
        private String dutyTime3s;
        private String dutyTime3c;
        private String dutyTime4s;
        private String dutyTime4c;
        private String dutyTime5s;
        private String dutyTime5c;
        private String dutyTime6s;
        private String dutyTime6c;
        private String dutyTime7s;
        private String dutyTime7c;   // 일 종료
    }

}
