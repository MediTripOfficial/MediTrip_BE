
package com.meditrip.medicine.domain;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;

@Getter
public enum SymptomCode {

    GENERAL_INTERNAL_PAIN(11, "Fever, Pain & Inflammation", "General & Internal Pain", 60,
            List.of("Headache", "Period Pain", "Fever")),

    MUSCLE_JOINT_PAIN(12, "Fever, Pain & Inflammation", "Muscle & Joint Pain", 70,
            List.of("Muscle Pain", "Sprain", "Joint Pain")),

    STOMACH_INDIGESTION(21, "Gastrointestinal & Motion Sickness", "Stomach Ache & Indigestion", 40,
            List.of("Heartburn", "Indigestion", "Stomachache")),

    INTESTINAL_ISSUES(22, "Gastrointestinal & Motion Sickness", "Intestinal Issues", 50,
            List.of("Diarrhea", "Cramps", "Constipation")),

    MOTION_SICKNESS(23, "Gastrointestinal & Motion Sickness", "Motion Sickness", 30,
            List.of("Motion Sick", "Dizziness", "Nausea")),

    THROAT_COUGH(31, "Respiratory & Allergies", "Throat & Cough", 50,
            List.of("Sore Throat", "Cough", "Phlegm")),

    NASAL_ALLERGIC(32, "Respiratory & Allergies", "Nasal & Allergic Symptoms", 40,
            List.of("Runny Nose", "Stuffy Nose", "Allergy")),

    WOUNDS_BURNS(41, "Skin, Wounds & Oral Care", "Wounds & Burns", 60,
            List.of("Cuts", "Wounds", "Burns")),

    SKIN_IRRITATION_BITES(42, "Skin, Wounds & Oral Care", "Skin Irritation & Bites", 50,
            List.of("Insect Bites", "Itching", "Rash")),

    ORAL_LIP_ISSUES(43, "Skin, Wounds & Oral Care", "Oral & Lip Issues", 40,
            List.of("Mouth Ulcer", "Cold Sore", "Chapped Lips")),

    EYE_IRRITATION(51, "Ophthalmic Care", "Eye Irritation", 30,
            List.of("Dry Eyes", "Red Eyes", "Eye Allergy")),

    TRAVEL_FATIGUE(61, "Fatigue & Tonic", "Travel Fatigue", 20,
            List.of("Fatigue", "Low Energy", "Jet Lag"));


    private final int code;
    private final String majorNameEn;
    private final String subNameEn;
    private final int baseScore;
    private final List<String> hashtags;

    SymptomCode(int code, String majorNameEn, String subNameEn, int baseScore, List<String> hashtags) {
        this.code = code;
        this.majorNameEn = majorNameEn;
        this.subNameEn = subNameEn;
        this.baseScore = baseScore;
        this.hashtags = hashtags;
    }

    public static SymptomCode fromCode(int code) {
        return Arrays.stream(values())
                .filter(symptomCode -> symptomCode.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 primaryCode 입니다: " + code));
    }

}
