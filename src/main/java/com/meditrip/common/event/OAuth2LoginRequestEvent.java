package com.meditrip.common.event;

import com.meditrip.common.domain.UserRole;
import com.meditrip.config.oauth.user.OAuth2Provider;
import lombok.Getter;

@Getter
public class OAuth2LoginRequestEvent {

    private final String email;
    private final String name;
    private final OAuth2Provider provider;
    private String userId;
    private String userStatus;
    private boolean handled = false;
    private String failureReason;
    private UserRole userRole;

    public OAuth2LoginRequestEvent(String email, String name, OAuth2Provider provider) {
        this.email = email;
        this.name = name;
        this.provider = provider;
    }

    public void setResult(String userId, String userStatus, UserRole userRole) {
        this.userId = userId;
        this.userStatus = userStatus;
        this.handled = true;
        this.userRole = userRole;
    }

    public void setFailure(String failureReason) {
        this.failureReason = failureReason;
        this.handled = false;
    }

}
