package com.meditrip.common.event;

import com.meditrip.config.oauth.user.OAuth2Provider;

public record OAuth2LoginEvent(
        String email,
        String name,
        OAuth2Provider provider
) {
}
