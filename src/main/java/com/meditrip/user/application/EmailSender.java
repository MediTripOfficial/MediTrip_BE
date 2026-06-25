package com.meditrip.user.application;

public interface EmailSender {
    void send(String toEmail, String authCode);
}
