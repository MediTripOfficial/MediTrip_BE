package com.meditrip.user.domain.exception;

import com.meditrip.common.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException() {
        super("User not found.");
    }

}
