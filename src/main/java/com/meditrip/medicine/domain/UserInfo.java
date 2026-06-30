package com.meditrip.medicine.domain;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfo {

    private final UUID userId;
    private final String gender;
    private final String country;
    private final Double weight;
    private final Double height;
    private final Integer age;

}
