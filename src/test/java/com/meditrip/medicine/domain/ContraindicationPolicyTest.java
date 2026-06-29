package com.meditrip.medicine.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ContraindicationPolicyTest {

    @Nested
    @DisplayName("resolveRestrictedIngredients")
    class ResolveRestrictedIngredients {

        @Test
        @DisplayName("기저질환 1개가 매칭되면 그 카테고리의 금기 성분이 전부 포함된다.")
        void singleMatchingCondition_includesAllIngredientsOfThatCategory() {
            //given
            List<String> conditionNames = List.of("Diabetes");

            //when
            Set<String> result = ContraindicationPolicy.resolveRestrictedIngredients(conditionNames, List.of());

            //then
            assertThat(result).containsExactlyInAnyOrder(
                    "sugar-containing syrup", "sugar-containing liquid",
                    "high-sugar herbal liquid", "caffeine");
        }

        @Test
        @DisplayName("기저질환 2개가 매칭되면 두 카테고리의 금기 성분이 합쳐진다. (중복은 한 번만)")
        void twoMatchingConditions_unionsIngredients() {
            //given
            List<String> conditionNames = List.of("Diabetes", "Asthma");

            //when
            Set<String> result = ContraindicationPolicy.resolveRestrictedIngredients(conditionNames, List.of());

            //then: Caffeine은 DIABETES에만 있고, Ibuprofen/Aspirin 등은 ASTHMA 쪽
            assertThat(result).contains("caffeine", "ibuprofen", "aspirin", "sugar-containing syrup");
        }

        @Test
        @DisplayName("매칭되는 기저질환이 없으면 결과에 영향이 없다.")
        void noMatchingCondition_returnsEmptySet() {
            Set<String> result = ContraindicationPolicy.resolveRestrictedIngredients(
                    List.of("common cold", "headache"), List.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("알레르기명은 그 자체로 금기 성분에 그대로(소문자 변환되어) 추가된다.")
        void allergyNames_addedDirectly() {
            Set<String> result = ContraindicationPolicy.resolveRestrictedIngredients(List.of(), List.of("Aspirin"));

            assertThat(result).containsExactly("aspirin");
        }

        @Test
        @DisplayName("기저질환과 알레르기가 둘 다 있으면 합쳐진다.")
        void conditionsAndAllergies_bothCombined() {
            Set<String> result = ContraindicationPolicy.resolveRestrictedIngredients(
                    List.of("Diabetes"), List.of("Penicillin"));

            assertThat(result).contains("caffeine", "penicillin");
        }

        @Test
        @DisplayName("conditionNames가 null이어도 NPE 없이 동작한다.")
        void nullConditionNames_doesNotThrow() {
            Set<String> result = ContraindicationPolicy.resolveRestrictedIngredients(null, List.of("Aspirin"));

            assertThat(result).containsExactly("aspirin");
        }

        @Test
        @DisplayName("allergyNames가 null이어도 NPE 없이 동작한다.")
        void nullAllergyNames_doesNotThrow() {
            Set<String> result = ContraindicationPolicy.resolveRestrictedIngredients(List.of("Diabetes"), null);

            assertThat(result).contains("caffeine");
        }

        @Test
        @DisplayName("둘 다 null이면 빈 Set을 반환한다.")
        void bothNull_returnsEmptySet() {
            Set<String> result = ContraindicationPolicy.resolveRestrictedIngredients(null, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("둘 다 빈 리스트면 빈 Set을 반환한다.")
        void bothEmpty_returnsEmptySet() {
            Set<String> result = ContraindicationPolicy.resolveRestrictedIngredients(List.of(), List.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("결과는 항상 소문자로 정규화된다. (대소문자 섞인 입력도 소문자로 통일)")
        void result_isAlwaysNormalizedToLowercase() {
            Set<String> result = ContraindicationPolicy.resolveRestrictedIngredients(
                    List.of(), List.of("ASPIRIN", "Penicillin", "  Codeine  "));

            assertThat(result).containsExactlyInAnyOrder("aspirin", "penicillin", "codeine");
        }

        @Test
        @DisplayName("알레르기 목록에 빈 문자열/공백/null이 섞여 있으면 무시된다.")
        void blankOrNullAllergyEntries_areIgnored() {
            Set<String> result = ContraindicationPolicy.resolveRestrictedIngredients(
                    List.of(), Arrays.asList("Aspirin", "", "   ", null));

            assertThat(result).containsExactly("aspirin");
        }
    }

    @Nested
    @DisplayName("containsRestrictedIngredient")
    class ContainsRestrictedIngredient {

        @Test
        @DisplayName("약의 성분 중 하나라도 금기 목록에 있으면 true")
        void oneIngredientMatches_returnsTrue() {
            boolean result = ContraindicationPolicy.containsRestrictedIngredient(
                    List.of("Acetaminophen", "Aspirin"), Set.of("aspirin"));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("대소문자/공백이 달라도 매칭된다")
        void caseAndWhitespaceInsensitiveMatch() {
            boolean result = ContraindicationPolicy.containsRestrictedIngredient(
                    List.of("  ASPIRIN  "), Set.of("aspirin"));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("약의 성분이 금기 목록에 전혀 없으면 false")
        void noIngredientMatches_returnsFalse() {
            boolean result = ContraindicationPolicy.containsRestrictedIngredient(
                    List.of("Acetaminophen"), Set.of("aspirin", "ibuprofen"));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("약의 성분 목록이 null이면 false (NPE 없음)")
        void nullMedicineIngredients_returnsFalse() {
            boolean result = ContraindicationPolicy.containsRestrictedIngredient(null, Set.of("aspirin"));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("약의 성분 목록이 빈 리스트면 false")
        void emptyMedicineIngredients_returnsFalse() {
            boolean result = ContraindicationPolicy.containsRestrictedIngredient(List.of(), Set.of("aspirin"));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("금기 목록이 빈 Set이면 항상 false")
        void emptyRestrictedSet_returnsFalse() {
            boolean result = ContraindicationPolicy.containsRestrictedIngredient(
                    List.of("Aspirin", "Acetaminophen"), Set.of());

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("약의 성분 목록에 null 원소가 섞여 있어도 NPE 없이 나머지를 검사한다")
        void nullElementInIngredientList_doesNotThrow() {
            boolean result = ContraindicationPolicy.containsRestrictedIngredient(
                    Arrays.asList(null, "Aspirin"), Set.of("aspirin"));

            assertThat(result).isTrue();
        }
    }

}
