package com.meditrip.medicine.domain;

import lombok.Getter;

@Getter
public enum TakeMedicineCondition {
    G("Good"), O("Okay"), W("Worse");

    private final String description;

    TakeMedicineCondition(String description) {
        this.description = description;
    }
}
