package com.meditrip.medicine.domain.exception;

import com.meditrip.common.exception.NotFoundException;

public class MedicineIntakeNotFoundException extends NotFoundException {
    public MedicineIntakeNotFoundException(String message) {
        super(message);
    }

  public MedicineIntakeNotFoundException() {
    super("Medicine Intake Not Found");
  }
}
