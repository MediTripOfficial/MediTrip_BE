package com.meditrip.config.oauth.user;

import java.util.Map;

public class GoogleUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final String accessToken;
    private final String id;
    private final String email;
    private final String name;

    public GoogleUserInfo(Map<String, Object> attributes, String accessToken) {
        this.accessToken = accessToken;
        this.attributes = attributes;
        this.id = requireString(attributes, "sub");
        this.email = requireString(attributes, "email");
        this.name = requireString(attributes, "name");
    }

    private static String requireString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (!(value instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException("Missing or invalid OAuth attribute: " + key);
        }
        return s;
    }

    @Override
    public OAuth2Provider getProvider() {
        return OAuth2Provider.GOOGLE;
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getName() {
        return name;
    }

}
