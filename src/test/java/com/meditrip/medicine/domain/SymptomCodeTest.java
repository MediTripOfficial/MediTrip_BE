package com.meditrip.medicine.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SymptomCodeTest {

    @ParameterizedTest(name = "code={0} -> {1}/{2}, baseScore={3}")
    @DisplayName("fromCode는 유효한 12개 코드 전부에 대해 올바른 enum을 반환한다.")
    @CsvSource({
            "11, 'Fever, Pain & Inflammation', 'General & Internal Pain', 60",
            "12, 'Fever, Pain & Inflammation', 'Muscle & Joint Pain', 70",
            "21, 'Gastrointestinal & Motion Sickness', 'Stomach Ache & Indigestion', 40",
            "22, 'Gastrointestinal & Motion Sickness', 'Intestinal Issues', 50",
            "23, 'Gastrointestinal & Motion Sickness', 'Motion Sickness', 30",
            "31, 'Respiratory & Allergies', 'Throat & Cough', 50",
            "32, 'Respiratory & Allergies', 'Nasal & Allergic Symptoms', 40",
            "41, 'Skin, Wounds & Oral Care', 'Wounds & Burns', 60",
            "42, 'Skin, Wounds & Oral Care', 'Skin Irritation & Bites', 50",
            "43, 'Skin, Wounds & Oral Care', 'Oral & Lip Issues', 40",
            "51, 'Ophthalmic Care', 'Eye Irritation', 30",
            "61, 'Fatigue & Tonic', 'Travel Fatigue', 20",
    })
    void fromCode_returnsExpectedSymptomCode(int code, String majorNameEn, String subNameEn, int baseScore) {
        //when
        SymptomCode result = SymptomCode.fromCode(code);

        //then
        assertThat(result.getCode()).isEqualTo(code);
        assertThat(result.getMajorNameEn()).isEqualTo(majorNameEn);
        assertThat(result.getSubNameEn()).isEqualTo(subNameEn);
        assertThat(result.getBaseScore()).isEqualTo(baseScore);
    }

    @ParameterizedTest(name = "잘못된 코드: {0}")
    @DisplayName("정의되지 않은 코드면 예외가 발생한다.")
    @ValueSource(ints = {0, -1, 1, 10, 13, 20, 24, 30, 33, 40, 44, 50, 52, 60, 62, 99, Integer.MAX_VALUE, Integer.MIN_VALUE})
    void fromCode_throwsException_whenCodeIsInvalid(int invalidCode) {
        assertThatThrownBy(() -> SymptomCode.fromCode(invalidCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(invalidCode));
    }

    @Test
    @DisplayName("12개 소분류 코드는 전부 서로 다른 code 값을 가진다.")
    void allSymptomCodes_haveUniqueCodeValues() {
        long distinctCount = java.util.Arrays.stream(SymptomCode.values())
                .map(SymptomCode::getCode)
                .distinct()
                .count();

        assertThat(SymptomCode.values()).hasSize(12);
        assertThat(distinctCount).isEqualTo(12);
    }

    @Test
    @DisplayName("같은 대분류(majorNameEn) 안에서는 baseScore가 서로 겹치지 않는다. (secondarySymptom 추정 로직의 전제조건)")
    void withinSameMajorCategory_baseScoresAreDistinct() {
        java.util.Map<String, java.util.List<Integer>> byMajor = new java.util.HashMap<>();
        for (SymptomCode symptomCode : SymptomCode.values()) {
            byMajor.computeIfAbsent(symptomCode.getMajorNameEn(), k -> new java.util.ArrayList<>())
                    .add(symptomCode.getBaseScore());
        }

        byMajor.forEach((major, scores) -> {
            long distinct = scores.stream().distinct().count();
            assertThat(distinct)
                    .as("대분류 [%s] 안의 baseScore 목록 %s 는 전부 달라야 함", major, scores)
                    .isEqualTo(scores.size());
        });
    }

}
