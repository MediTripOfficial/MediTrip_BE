package com.meditrip.common.event;

public class OAuth2UnlinkRequestEvent {

    private final String email;
    private String userId;
    private boolean handled = false;

    public OAuth2UnlinkRequestEvent(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setResult(String userId) {
        this.userId = userId;
        this.handled = true;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isHandled() {
        return handled;
    }

}
