package com.raxrot.back.services.impl;

import com.raxrot.back.exceptions.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(emailService, "from", "no-reply@test.io");
    }

    @Test
    void sendEmail_shouldSendWithGivenFields() {
        emailService.sendEmail("user@test.io", "Hello", "Body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertArrayEquals(new String[]{"user@test.io"}, msg.getTo());
        assertEquals("no-reply@test.io", msg.getFrom());
        assertEquals("Hello", msg.getSubject());
        assertEquals("Body", msg.getText());
    }

    @Test
    void sendEmail_whenSubjectAndBodyNull_shouldFallbackToEmptyStrings() {
        emailService.sendEmail("user@test.io", null, null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertEquals("", msg.getSubject());
        assertEquals("", msg.getText());
    }

    @Test
    void sendEmail_whenRecipientNull_shouldThrowApiException() {
        ApiException ex = assertThrows(ApiException.class,
                () -> emailService.sendEmail(null, "s", "b"));
        assertEquals("Recipient address is empty", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void sendEmail_whenRecipientBlank_shouldThrowApiException() {
        ApiException ex = assertThrows(ApiException.class,
                () -> emailService.sendEmail("   ", "s", "b"));
        assertEquals("Recipient address is empty", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void sendEmail_whenMailSenderFails_shouldWrapInApiException() {
        doThrow(new MailSendException("boom"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        ApiException ex = assertThrows(ApiException.class,
                () -> emailService.sendEmail("user@test.io", "s", "b"));
        assertEquals("Failed to send email", ex.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
    }
}
