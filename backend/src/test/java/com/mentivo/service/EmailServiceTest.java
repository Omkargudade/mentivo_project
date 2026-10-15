package com.mentivo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSenderProvider);
    }

    @Test
    @DisplayName("isEmailConfigured returns false when neither Brevo API key nor SMTP credentials are provided")
    void testIsEmailConfigured_DefaultUnconfigured() {
        assertThat(emailService.isEmailConfigured()).isFalse();
        assertThat(emailService.isBrevoApiConfigured()).isFalse();
        assertThat(emailService.isSmtpConfigured()).isFalse();
    }

    @Test
    @DisplayName("isEmailConfigured returns true when BREVO_API_KEY is configured")
    void testIsEmailConfigured_WithBrevoApiKey() {
        ReflectionTestUtils.setField(emailService, "brevoApiKey", "xkeysib-test-key-12345");

        assertThat(emailService.isEmailConfigured()).isTrue();
        assertThat(emailService.isBrevoApiConfigured()).isTrue();
        assertThat(emailService.isSmtpConfigured()).isFalse();
    }

    @Test
    @DisplayName("isEmailConfigured returns true when SMTP credentials are configured")
    void testIsEmailConfigured_WithSmtpCredentials() {
        ReflectionTestUtils.setField(emailService, "mailUsername", "user@example.com");
        ReflectionTestUtils.setField(emailService, "mailHost", "smtp-relay.brevo.com");

        assertThat(emailService.isEmailConfigured()).isTrue();
        assertThat(emailService.isBrevoApiConfigured()).isFalse();
        assertThat(emailService.isSmtpConfigured()).isTrue();
    }

    @Test
    @DisplayName("isBrevoApiConfigured returns false when key contains unreplaced environment variable placeholder")
    void testIsBrevoApiConfigured_WithPlaceholder() {
        ReflectionTestUtils.setField(emailService, "brevoApiKey", "${BREVO_API_KEY:}");
        assertThat(emailService.isBrevoApiConfigured()).isFalse();
    }

    @Test
    @DisplayName("Email dispatch operations do not crash when unconfigured")
    void testEmailDispatchSafety_WhenUnconfigured() {
        assertThatCode(() -> {
            emailService.sendPasswordResetEmail("test@example.com", "http://localhost:5173/reset-password?token=abc");
            emailService.sendPasswordChangedAlert("test@example.com");
            emailService.sendBookingConfirmationToLearner("learner@example.com", "Learner", "Mentor", "Session", "2026-10-20", "10:00 - 11:00");
            emailService.sendBookingAlertToMentor("mentor@example.com", "Mentor", "Learner", "Session", "2026-10-20", "10:00 - 11:00");
            emailService.sendOfflineContactRequest("mentor@example.com", "Mentor", "Learner", "learner@example.com", "Hello");
        }).doesNotThrowAnyException();
    }
}
