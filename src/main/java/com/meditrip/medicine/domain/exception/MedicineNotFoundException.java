package com.meditrip.medicine.domain.exception;

import com.meditrip.common.exception.NotFoundException;

public class MedicineNotFoundException extends NotFoundException {
    public MedicineNotFoundException(String message) {
        super(message);
    }

  public MedicineNotFoundException() {
    super("Medicine Not Found");
  }
}
