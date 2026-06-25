package com.meditrip.medicine.domain.entity;

import com.meditrip.common.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "medicines")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
public class Medicine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name_ko;
    private String name_en;
    private String manufacturer_ko;
    private String manufacturer_en;
    private Boolean isConvenienceStore;
    private Boolean isChildSafe;
    private String usage_en;
    private String dosage;
    private String interval;
    private String maxLimit;
    private String caution;
    private String cautionDetailEn;
    private String cautionDetailKo;
    private String efficacyDetailEn;
    private String efficacyDetailKo;
    private String usageDetailEn;
    private String usageDetailKo;
    private String drugInteractionsEn;
    private String drugInteractionsKo;
    private String seeDoctorEn;
    private String seeDoctorKo;
    private String sourceUrl;
    private String imageUrl;
    private String countryCode;

}
