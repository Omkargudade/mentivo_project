package com.mentivo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transactional email service supporting Brevo REST API v3 (primary, works across all firewalls on HTTPS 443)
 * and Brevo SMTP relay (fallback via JavaMailSender on port 587).
 * Reads all credentials from environment variables (BREVO_API_KEY, BREVO_SMTP_USER, BREVO_SMTP_KEY, etc.).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    @Value("${brevo.from-email:support@mentivo.com}")
    private String fromEmail;

    @Value("${brevo.from-name:Mentivo}")
    private String fromName;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.host:smtp-relay.brevo.com}")
    private String mailHost;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isEmailConfigured() {
        return isBrevoApiConfigured() || isSmtpConfigured();
    }

    public boolean isBrevoApiConfigured() {
        return brevoApiKey != null && !brevoApiKey.trim().isEmpty() && !brevoApiKey.startsWith("${");
    }

    public boolean isSmtpConfigured() {
        return mailUsername != null && !mailUsername.trim().isEmpty() && !mailUsername.startsWith("${") &&
                mailHost != null && !mailHost.trim().isEmpty() &&
                !"localhost".equalsIgnoreCase(mailHost.trim());
    }

    /**
     * Unified internal dispatcher: routes through Brevo REST API if API key is configured,
     * otherwise routes through SMTP relay if SMTP credentials are provided,
     * or logs diagnostics without failing database transactions.
     */
    public void sendHtmlEmail(String toEmail, String toName, String subject, String htmlContent) {
        if (isBrevoApiConfigured()) {
            sendViaBrevoApi(toEmail, toName, subject, htmlContent);
        } else if (isSmtpConfigured()) {
            sendViaSmtp(toEmail, toName, subject, htmlContent);
        } else {
            log.info("Email dispatch skipped: neither Brevo API (BREVO_API_KEY) nor Brevo SMTP (BREVO_SMTP_USER/KEY) credentials configured. " +
                            "Target: {}, Subject: '{}'. To enable live email delivery, configure BREVO_API_KEY as an environment variable.",
                    maskEmail(toEmail), subject);
        }
    }

    private void sendViaBrevoApi(String toEmail, String toName, String subject, String htmlContent) {
        try {
            String effectiveSenderEmail = (fromEmail != null && !fromEmail.trim().isEmpty() && fromEmail.contains("@"))
                    ? fromEmail.trim()
                    : (mailUsername != null && mailUsername.contains("@") ? mailUsername.trim() : "support@mentivo.com");
            String effectiveSenderName = (fromName != null && !fromName.trim().isEmpty()) ? fromName.trim() : "Mentivo";
            String effectiveRecipientName = (toName != null && !toName.trim().isEmpty()) ? toName.trim() : toEmail;

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("sender", Map.of("name", effectiveSenderName, "email", effectiveSenderEmail));
            payloadMap.put("to", List.of(Map.of("email", toEmail, "name", effectiveRecipientName)));
            payloadMap.put("subject", subject);
            payloadMap.put("htmlContent", htmlContent);

            String payload = objectMapper.writeValueAsString(payloadMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", brevoApiKey.trim())
                    .header("content-type", "application/json")
                    .timeout(Duration.ofSeconds(12))
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Successfully dispatched '{}' email via Brevo API to {}", subject, maskEmail(toEmail));
            } else {
                log.error("Brevo API returned HTTP {} for '{}' to {}. Response: {}",
                        response.statusCode(), subject, maskEmail(toEmail), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to send email via Brevo API to {}: {}", maskEmail(toEmail), e.getMessage());
        }
    }

    private void sendViaSmtp(String toEmail, String toName, String subject, String htmlContent) {
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                log.warn("JavaMailSender bean not available. Unable to send email to {}", maskEmail(toEmail));
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String effectiveSender = (mailUsername != null && !mailUsername.trim().isEmpty())
                    ? mailUsername.trim()
                    : fromEmail;
            helper.setFrom(effectiveSender, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Successfully dispatched '{}' email via SMTP relay to {}", subject, maskEmail(toEmail));
        } catch (Exception e) {
            log.error("Failed to send email via SMTP relay to {}: {}", maskEmail(toEmail), e.getMessage());
        }
    }

    @Async
    public void sendBookingConfirmationToLearner(String learnerEmail, String learnerName, String mentorName,
                                                 String sessionTitle, String date, String time) {
        String html = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px;\">" +
                "<h2 style=\"color: #10b981; margin-bottom: 16px;\">Session Booking Confirmed!</h2>" +
                "<p style=\"color: #334155; font-size: 16px;\">Hi <strong>" + escape(learnerName) + "</strong>,</p>" +
                "<p style=\"color: #475569;\">Your session with <strong>" + escape(mentorName) + "</strong> has been confirmed.</p>" +
                "<div style=\"background-color: #f8fafc; border-left: 4px solid #10b981; padding: 16px; margin: 20px 0;\">" +
                "<p style=\"margin: 4px 0;\"><strong>Topic:</strong> " + escape(sessionTitle) + "</p>" +
                "<p style=\"margin: 4px 0;\"><strong>Date:</strong> " + escape(date) + "</p>" +
                "<p style=\"margin: 4px 0;\"><strong>Time:</strong> " + escape(time) + "</p>" +
                "</div>" +
                "<p style=\"color: #64748b; font-size: 14px;\">You can join your live video session directly on Mentivo at the scheduled time.</p>" +
                "</div>";

        sendHtmlEmail(learnerEmail, learnerName, "Booking Confirmed: " + sessionTitle, html);
    }

    @Async
    public void sendBookingAlertToMentor(String mentorEmail, String mentorName, String learnerName,
                                        String sessionTitle, String date, String time) {
        String html = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px;\">" +
                "<h2 style=\"color: #4f46e5; margin-bottom: 16px;\">New Session Booked!</h2>" +
                "<p style=\"color: #334155; font-size: 16px;\">Hi <strong>" + escape(mentorName) + "</strong>,</p>" +
                "<p style=\"color: #475569;\"><strong>" + escape(learnerName) + "</strong> has booked a session with you.</p>" +
                "<div style=\"background-color: #f8fafc; border-left: 4px solid #4f46e5; padding: 16px; margin: 20px 0;\">" +
                "<p style=\"margin: 4px 0;\"><strong>Topic:</strong> " + escape(sessionTitle) + "</p>" +
                "<p style=\"margin: 4px 0;\"><strong>Date:</strong> " + escape(date) + "</p>" +
                "<p style=\"margin: 4px 0;\"><strong>Time:</strong> " + escape(time) + "</p>" +
                "</div>" +
                "<p style=\"color: #64748b; font-size: 14px;\">Please check your Mentor Dashboard on Mentivo for complete booking details.</p>" +
                "</div>";

        sendHtmlEmail(mentorEmail, mentorName, "New Booking: " + learnerName + " booked a session", html);
    }

    @Async
    public void sendOfflineContactRequest(String mentorEmail, String mentorName, String learnerName,
                                          String learnerEmail, String messageText) {
        String html = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px;\">" +
                "<h2 style=\"color: #4f46e5;\">New Offline Contact Request</h2>" +
                "<p>Hi <strong>" + escape(mentorName) + "</strong>,</p>" +
                "<p><strong>" + escape(learnerName) + "</strong> tried to reach you while you were offline.</p>" +
                "<div style=\"background-color: #f1f5f9; padding: 16px; border-radius: 6px; margin: 16px 0;\">" +
                "<p style=\"margin: 0; font-style: italic; color: #334155;\">\"" + escape(messageText) + "\"</p>" +
                "</div>" +
                "<p>You can reply directly to this email to contact " + escape(learnerName) + " (" + escape(learnerEmail) + ") or message them on Mentivo.</p>" +
                "</div>";

        sendHtmlEmail(mentorEmail, mentorName, "Mentivo: Offline Contact Request from " + learnerName, html);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String html = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px;\">" +
                "<h2 style=\"color: #4f46e5; margin-bottom: 16px;\">Password Reset Request</h2>" +
                "<p style=\"color: #475569; font-size: 15px;\">We received a request to reset the password for your Mentivo account.</p>" +
                "<p style=\"color: #475569; font-size: 15px;\">Click the button below to choose a new password. This link is valid for <strong>15 minutes</strong> and can only be used once:</p>" +
                "<div style=\"text-align: center; margin: 28px 0;\">" +
                "<a href=\"" + resetLink + "\" style=\"background-color: #4f46e5; color: #ffffff; padding: 12px 28px; font-weight: bold; font-size: 15px; text-decoration: none; border-radius: 6px; display: inline-block;\">Reset Password</a>" +
                "</div>" +
                "<p style=\"color: #64748b; font-size: 13px;\">If the button does not work, copy and paste the following link into your browser:</p>" +
                "<p style=\"color: #6366f1; font-size: 12px; word-break: break-all;\">" + resetLink + "</p>" +
                "<p style=\"color: #94a3b8; font-size: 13px; margin-top: 24px;\">If you did not make this request, you can safely ignore this email. Your current password remains secure.</p>" +
                "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;\" />" +
                "<p style=\"color: #94a3b8; font-size: 12px;\">© Mentivo Platform. Secure knowledge marketplace.</p>" +
                "</div>";

        sendHtmlEmail(toEmail, "Mentivo User", "Reset Your Mentivo Password", html);
    }

    @Async
    public void sendPasswordChangedAlert(String toEmail) {
        String html = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px;\">" +
                "<h2 style=\"color: #10b981; margin-bottom: 16px;\">Password Changed Successfully</h2>" +
                "<p style=\"color: #475569; font-size: 15px;\">Your Mentivo account password was recently updated.</p>" +
                "<p style=\"color: #64748b; font-size: 14px;\">If you initiated this change, no further action is required.</p>" +
                "<p style=\"color: #ef4444; font-size: 14px; font-weight: bold;\">If you did NOT change your password, please contact our support team immediately to secure your account.</p>" +
                "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;\" />" +
                "<p style=\"color: #94a3b8; font-size: 12px;\">© Mentivo Platform. Secure knowledge marketplace.</p>" +
                "</div>";

        sendHtmlEmail(toEmail, "Mentivo User", "Security Alert: Your Mentivo Password Has Been Changed", html);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return "*@" + email.substring(atIndex + 1);
        return email.substring(0, 2) + "***@" + email.substring(atIndex + 1);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String jsonEscape(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < ' ') {
                        String t = "000" + Integer.toHexString(ch);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }
}
