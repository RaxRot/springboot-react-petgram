package com.raxrot.back.services.impl;

import com.raxrot.back.exceptions.ApiException;
import com.raxrot.back.services.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.properties.mail.smtp.from}")
    private String from;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("Preparing to send email to '{}'", to);

        if (to == null || to.isBlank()) {
            log.warn("Attempted to send email with empty recipient address");
            throw new ApiException("Recipient address is empty", HttpStatus.BAD_REQUEST);
        }

        if (subject == null) subject = "";
        if (body == null) body = "";

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            log.debug("Email details — From: {}, To: {}, Subject: '{}'", from, to, subject);
            mailSender.send(message);
            log.info("Email successfully sent to '{}'", to);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new ApiException("Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
