package com.raxrot.back.services;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
}
