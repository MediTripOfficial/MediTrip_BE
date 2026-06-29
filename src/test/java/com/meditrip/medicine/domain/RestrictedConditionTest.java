package com.meditrip.medicine.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RestrictedConditionTest {

    @ParameterizedTest(name = "[{0}] -> DIABETES 매칭")
    @DisplayName("DIABETES는 별칭을 포함하는 다양한 표현을 매칭한다. (대소문자/부분일치)")
    @ValueSource(strings = {"diabetes", "Diabetes", "DIABETES", "diabetes type 2", "I have diabetic issues", "당뇨", "내가 당뇨가 있어요"})
    void diabetes_matchesVariousExpressions(String conditionName) {
        assertThat(RestrictedCondition.DIABETES.matches(conditionName)).isTrue();
    }

    @ParameterizedTest(name = "[{0}] -> 매칭 안 됨")
    @DisplayName("관련 없는 condition 이름은 어떤 카테고리에도 매칭되지 않는다.")
    @ValueSource(strings = {"common cold", "headache", "스트레스", "fatigue"})
    void unrelatedConditionName_matchesNothing(String conditionName) {
        for (RestrictedCondition restrictedCondition : RestrictedCondition.values()) {
            assertThat(restrictedCondition.matches(conditionName))
                    .as("[%s]가 [%s]에 매칭되면 안 됨", conditionName, restrictedCondition)
                    .isFalse();
        }
    }

    @ParameterizedTest
    @DisplayName("null/blank/빈 문자열은 항상 매칭되지 않는다.")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void nullOrBlank_neverMatches(String conditionName) {
        for (RestrictedCondition restrictedCondition : RestrictedCondition.values()) {
            assertThat(restrictedCondition.matches(conditionName)).isFalse();
        }
    }

    @ParameterizedTest(name = "{0} -> alias 역방향 부분일치 (\"{1}\" contains alias 가 아니라 alias contains 정규화값)")
    @DisplayName("정규화된 입력값이 alias보다 짧아도(alias가 입력값을 포함하는 경우) 매칭된다.")
    @CsvSource({
            "ASTHMA, asth",
            "CARDIOVASCULAR_DISEASE, cardio",
    })
    void shortInputContainedInAlias_alsoMatches(RestrictedCondition restrictedCondition, String shortInput) {
        assertThat(restrictedCondition.matches(shortInput)).isTrue();
    }

    @Test
    @DisplayName("PREGNANCY_BREASTFEEDING의 금기 성분은 NSAID 계열 + 아스피린 + 와파린 등을 포함한다.")
    void pregnancyBreastfeeding_restrictedIngredients() {
        assertThat(RestrictedCondition.PREGNANCY_BREASTFEEDING.getRestrictedIngredients())
                .contains("Ibuprofen", "Aspirin", "Warfarin", "Retinol", "Caffeine");
    }

    @Test
    @DisplayName("DIABETES의 금기 성분은 당류 함유 제형 위주다.")
    void diabetes_restrictedIngredients() {
        assertThat(RestrictedCondition.DIABETES.getRestrictedIngredients())
                .containsExactlyInAnyOrder(
                        "Sugar-containing syrup", "Sugar-containing liquid",
                        "High-sugar herbal liquid", "Caffeine");
    }

    @Test
    @DisplayName("LIVER_KIDNEY_DISEASE의 금기 성분에는 Acetaminophen이 포함된다. (간/신장 환자 핵심 주의 성분)")
    void liverKidneyDisease_includesAcetaminophen() {
        assertThat(RestrictedCondition.LIVER_KIDNEY_DISEASE.getRestrictedIngredients())
                .contains("Acetaminophen");
    }

    @Test
    @DisplayName("모든 카테고리는 금기 성분을 최소 1개 이상 가진다. (빈 매핑 방지)")
    void everyCategory_hasAtLeastOneRestrictedIngredient() {
        for (RestrictedCondition restrictedCondition : RestrictedCondition.values()) {
            assertThat(restrictedCondition.getRestrictedIngredients())
                    .as("%s 는 빈 목록이면 안 됨", restrictedCondition)
                    .isNotEmpty();
        }
    }

    @Test
    @DisplayName("6개 카테고리가 모두 정의되어 있다.")
    void hasExactlySixCategories() {
        assertThat(RestrictedCondition.values()).hasSize(6);
    }

}
