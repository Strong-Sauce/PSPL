package com.postSale.amcProject.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * EmailService sends password reset emails.
 *
 * If you haven't configured spring.mail.host in application.properties,
 * the reset URL is simply logged to the console (useful during development).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.mail.from:no-reply@pspl.local}")
    private String fromAddress;


    /**
     * Sends a password reset email with a clickable link.
     * Falls back to console logging if mail is not configured.
     */
    public void sendPasswordResetEmail(String toEmail, String userName, String resetUrl, LocalDateTime expiresAt) {
        if (mailSender == null || mailHost == null || mailHost.isBlank()) {
            // Mail not configured — log the URL so devs can test the reset flow manually
            log.info("=== PASSWORD RESET URL (mail not configured) ===");
            log.info("To: {}", toEmail);
            log.info("Reset URL: {}", resetUrl);
            log.info("Expires: {}", expiresAt);
            log.info("================================================");
            return;
        }

        // Build and send the actual email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("PSPL — Reset your password");
        message.setText(buildEmailBody(userName, resetUrl, expiresAt));
        mailSender.send(message);

        log.info("Password reset email sent to {}", toEmail);
    }

    private String buildEmailBody(String name, String resetUrl, LocalDateTime expiresAt) {
        return "Hello " + name + ",\n\n"
                + "You requested a password reset for your PSPL account.\n\n"
                + "Click the link below to set a new password:\n"
                + resetUrl + "\n\n"
                + "This link expires at: " + expiresAt + "\n\n"
                + "If you did not request this, you can safely ignore this email.\n\n"
                + "— PSPL Team";
    }
}

