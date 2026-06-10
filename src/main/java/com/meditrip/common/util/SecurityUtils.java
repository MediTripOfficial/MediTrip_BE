package com.meditrip.common.util;

public final class SecurityUtils {

    public static String convertToMaskedEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String id = parts[0];
        String domain = parts[1];

        if (id.length() <= 2) {
            return id.substring(0, 1) + "*" + "@" + domain;
        }

        String maskedId = id.substring(0, 2) + "*".repeat(id.length() - 2);

        return maskedId + "@" + domain;
    }

}
