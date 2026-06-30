package com.meditrip.medicine.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ContraindicationPolicy {

    private ContraindicationPolicy() {
    }

    public static Set<String> resolveRestrictedIngredients(List<String> conditionNames, List<String> allergyNames) {
        Set<String> restricted = new HashSet<>();

        if (conditionNames != null) {
            for (String conditionName : conditionNames) {
                for (RestrictedCondition restrictedCondition : RestrictedCondition.values()) {
                    if (restrictedCondition.matches(conditionName)) {
                        restricted.addAll(restrictedCondition.getRestrictedIngredients());
                    }
                }
            }
        }

        if (allergyNames != null) {
            restricted.addAll(allergyNames);
        }

        return normalize(restricted);
    }

    private static Set<String> normalize(Set<String> ingredients) {
        Set<String> normalized = new HashSet<>();
        for (String ingredient : ingredients) {
            if (ingredient != null && !ingredient.isBlank()) {
                normalized.add(ingredient.trim().toLowerCase());
            }
        }
        return normalized;
    }

    public static boolean containsRestrictedIngredient(List<String> medicineIngredientNames,
                                                         Set<String> restrictedIngredientsLowercase) {
        if (medicineIngredientNames == null || medicineIngredientNames.isEmpty()) {
            return false;
        }
        for (String ingredientName : medicineIngredientNames) {
            if (ingredientName != null && restrictedIngredientsLowercase.contains(ingredientName.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

}
