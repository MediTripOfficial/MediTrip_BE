
package com.meditrip.medicine.domain;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum SymptomCode {

    GENERAL_INTERNAL_PAIN(11, "Fever, Pain & Inflammation", "General & Internal Pain", 60),
    MUSCLE_JOINT_PAIN(12, "Fever, Pain & Inflammation", "Muscle & Joint Pain", 70),
    STOMACH_INDIGESTION(21, "Gastrointestinal & Motion Sickness", "Stomach Ache & Indigestion", 40),
    INTESTINAL_ISSUES(22, "Gastrointestinal & Motion Sickness", "Intestinal Issues", 50),
    MOTION_SICKNESS(23, "Gastrointestinal & Motion Sickness", "Motion Sickness", 30),
    THROAT_COUGH(31, "Respiratory & Allergies", "Throat & Cough", 50),
    NASAL_ALLERGIC(32, "Respiratory & Allergies", "Nasal & Allergic Symptoms", 40),
    WOUNDS_BURNS(41, "Skin, Wounds & Oral Care", "Wounds & Burns", 60),
    SKIN_IRRITATION_BITES(42, "Skin, Wounds & Oral Care", "Skin Irritation & Bites", 50),
    ORAL_LIP_ISSUES(43, "Skin, Wounds & Oral Care", "Oral & Lip Issues", 40),
    EYE_IRRITATION(51, "Ophthalmic Care", "Eye Irritation", 30),
    TRAVEL_FATIGUE(61, "Fatigue & Tonic", "Travel Fatigue", 20);

    private final int code;
    private final String majorNameEn;
    private final String subNameEn;
    private final int baseScore;

    SymptomCode(int code, String majorNameEn, String subNameEn, int baseScore) {
        this.code = code;
        this.majorNameEn = majorNameEn;
        this.subNameEn = subNameEn;
        this.baseScore = baseScore;
    }

    public static SymptomCode fromCode(int code) {
        return Arrays.stream(values())
                .filter(symptomCode -> symptomCode.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 primaryCode 입니다: " + code));
    }

}
