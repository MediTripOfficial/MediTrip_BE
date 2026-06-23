package com.meditrip.user.infra.mail;

import com.meditrip.common.util.SecurityUtils;
import com.meditrip.user.application.EmailSender;
import jakarta.annotation.PostConstruct;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GmailEmailSender implements EmailSender {

    @Value("${email.google.email}")
    private String adminEmail;

    @Value("${email.google.password}")
    private String adminPassword;

    private Session mailSession;

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        this.mailSession = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(adminEmail, adminPassword);
            }
        });
    }

    @Override
    public void send(String toEmail, String authCode) {
        try {
            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(adminEmail, "Meditrip"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("[Meditrip] Email Verification Request");
            message.setText(buildEmailBody(authCode));
            Transport.send(message);

            log.info("인증 메일 발송 완료. email: {}", SecurityUtils.convertToMaskedEmail(toEmail));
        } catch (Exception e) {
            log.error("이메일 발송 실패. email: {}", SecurityUtils.convertToMaskedEmail(toEmail), e);
            throw new MailSendException("Failed to send verification email.");
        }
    }

    private String buildEmailBody(String authCode) {
        return "Hello,\n\n"
                + "Thank you for registering with Meditrip.\n"
                + "Your verification code is: " + authCode + "\n\n"
                + "Please enter this code to complete your verification.\n"
                + "If you did not request this code, please ignore this email.\n\n"
                + "Best regards,\n"
                + "The Meditrip Team";
    }

}
