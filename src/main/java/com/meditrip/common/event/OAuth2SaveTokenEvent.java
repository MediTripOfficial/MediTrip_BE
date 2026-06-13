package com.meditrip.common.event;

public record OAuth2SaveTokenEvent(
        String userId,
        String refreshToken
) {
}
