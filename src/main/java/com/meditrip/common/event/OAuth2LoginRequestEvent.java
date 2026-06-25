package com.meditrip.common.event;

import com.meditrip.config.oauth.user.OAuth2Provider;

public class OAuth2LoginRequestEvent {

    private final String email;
    private final String name;
    private final OAuth2Provider provider;
    private String userId;
    private String userStatus;
    private boolean handled = false;
    private String failureReason;

    public OAuth2LoginRequestEvent(String email, String name, OAuth2Provider provider) {
        this.email = email;
        this.name = name;
        this.provider = provider;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public OAuth2Provider getProvider() {
        return provider;
    }

    public void setResult(String userId, String userStatus) {
        this.userId = userId;
        this.userStatus = userStatus;
        this.handled = true;
    }

    public void setFailure(String failureReason) {
        this.failureReason = failureReason;
        this.handled = false;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public boolean isHandled() {
        return handled;
    }
}
