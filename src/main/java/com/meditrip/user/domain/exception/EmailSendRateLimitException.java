package com.meditrip.user.domain.exception;

import com.meditrip.common.exception.TooManyRequestsException;

public class EmailSendRateLimitException extends TooManyRequestsException {
    public EmailSendRateLimitException(String message) {
        super(message);
    }
}