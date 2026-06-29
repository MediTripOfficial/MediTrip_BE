package com.meditrip.medicine.domain;

import java.util.List;
import java.util.Set;
import lombok.Getter;

@Getter
public enum RestrictedCondition {

    PREGNANCY_BREASTFEEDING(
            Set.of("pregnancy", "breastfeeding", "nursing", "lactation", "임신", "수유"),
            List.of("Ibuprofen", "Dexibuprofen", "Ketoprofen", "Piroxicam", "Diclofenac",
                    "Methyl salicylate", "Glycol salicylate", "Aspirin", "Naproxen",
                    "Isopropylantipyrine", "Caffeine", "Retinol", "Warfarin")
    ),

    CARDIOVASCULAR_DISEASE(
            Set.of("cardiovascular", "heart disease", "hypertension", "심혈관", "고혈압", "심장"),
            List.of("Ibuprofen", "Dexibuprofen", "Ketoprofen", "Piroxicam", "Diclofenac",
                    "Naproxen", "Aspirin", "Caffeine", "Sodium bicarbonate", "Sodium alginate",
                    "Calcium carbonate")
    ),

    DIABETES(
            Set.of("diabetes", "diabetic", "당뇨"),
            List.of("Sugar-containing syrup", "Sugar-containing liquid", "High-sugar herbal liquid", "Caffeine")
    ),

    ASTHMA(
            Set.of("asthma", "천식"),
            List.of("Ibuprofen", "Dexibuprofen", "Ketoprofen", "Piroxicam", "Diclofenac",
                    "Naproxen", "Aspirin", "Methyl salicylate", "Glycol salicylate")
    ),

    LIVER_KIDNEY_DISEASE(
            Set.of("liver", "kidney", "renal", "hepatic", "간", "신장"),
            List.of("Acetaminophen", "Ibuprofen", "Dexibuprofen", "Ketoprofen", "Piroxicam",
                    "Diclofenac", "Naproxen", "Aspirin", "Sodium bicarbonate", "Sodium alginate",
                    "Calcium carbonate", "Aluminum phosphate", "Magnesium hydroxide")
    ),

    GASTRIC_ULCER_GI_BLEEDING(
            Set.of("gastric ulcer", "gastrointestinal bleeding", "gi bleeding", "peptic ulcer",
                    "위궤양", "소화성 출혈", "위장출혈"),
            List.of("Ibuprofen", "Dexibuprofen", "Ketoprofen", "Piroxicam", "Diclofenac",
                    "Naproxen", "Aspirin", "Methyl salicylate", "Glycol salicylate",
                    "Isopropylantipyrine", "Caffeine", "Warfarin")
    );

    private final Set<String> aliases;
    private final List<String> restrictedIngredients;

    RestrictedCondition(Set<String> aliases, List<String> restrictedIngredients) {
        this.aliases = aliases;
        this.restrictedIngredients = restrictedIngredients;
    }

    public boolean matches(String conditionName) {
        if (conditionName == null || conditionName.isBlank()) {
            return false;
        }
        String normalized = conditionName.trim().toLowerCase();
        return aliases.stream().anyMatch(alias -> {
            String aliasLower = alias.toLowerCase();
            return normalized.contains(aliasLower) || aliasLower.contains(normalized);
        });
    }

}
