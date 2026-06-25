package com.meditrip.map.infra.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.meditrip.map.infra.api.dto.PharmacyApiResponse;
import com.meditrip.map.infra.api.dto.PharmacyApiResponse.PharmacyItem;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class PharmacyInfoClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${pharmacy.api.key}")
    private String apiKey;

    @Value("${pharmacy.api.url}")
    private String apiUrl;

    public PharmacyInfoClient() {
        this.restTemplate = new RestTemplate();

        this.restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        this.objectMapper.coercionConfigFor(LogicalType.POJO)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
    }

    public PharmacyItem findByNameAndRegion(String pharmacyName, String province, String city) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("QN", pharmacyName)
                    .queryParam("numOfRows", 5)
                    .queryParam("pageNo", 1)
                    .queryParam("_type", "json");

            if (StringUtils.hasText(province)) {
                builder.queryParam("Q0", province);
            }
            if (StringUtils.hasText(city)) {
                builder.queryParam("Q1", city);
            }

            URI uri = builder.build().encode().toUri();

            String json = restTemplate.getForObject(uri, String.class);
            if (!StringUtils.hasText(json)) {
                return null;
            }

            PharmacyApiResponse response = objectMapper.readValue(json, PharmacyApiResponse.class);

            if (response == null || response.getResponse() == null
                    || response.getResponse().getBody() == null
                    || response.getResponse().getBody().getItems() == null) {
                return null;
            }

            System.out.println(response.getResponse().getBody().getItems().first().toString());

            return response.getResponse().getBody().getItems().first();
        } catch (Exception e) {
            log.warn("공공 API 약국 영업시간 조회 실패. name: [{}], region: [{} {}]", pharmacyName, province, city, e);
            return null;
        }
    }

}
