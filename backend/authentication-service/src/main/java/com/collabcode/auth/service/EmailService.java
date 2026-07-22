package com.collabcode.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;

/**
 * Sends HTML emails rendered from Thymeleaf templates.
 *
 * <p>Templates live in {@code src/main/resources/templates/}:
 * <ul>
 *   <li>{@code welcome-email.html} – sent on successful registration</li>
 *   <li>{@code email-verification.html} – resend-verification flow</li>
 *   <li>{@code password-reset.html} – forgot-password flow</li>
 * </ul>
 *
 * <p>SMTP is configured via {@code spring.mail.*} in {@code application.yml}.
 * Set {@code MAIL_HOST}, {@code MAIL_PORT}, {@code MAIL_USERNAME}, {@code MAIL_PASSWORD}
 * environment variables (or Mailtrap / Gmail App Password) to enable actual sending.
 */
@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromEmail;
    private final String fromName;
    private final String baseUrl;

    public EmailService(JavaMailSender mailSender,
                        TemplateEngine templateEngine,
                        @Value("${app.mail.from}") String fromEmail,
                        @Value("${app.mail.from-name}") String fromName,
                        @Value("${app.base-url}") String baseUrl) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.baseUrl = baseUrl;
    }

    // ─── Public Send Methods ──────────────────────────────────────────────────

    /**
     * Sends the combined welcome + email-verification email on registration.
     */
    public void sendWelcomeEmail(String toEmail, String userName, String verificationToken) {
        String verificationLink = buildVerificationLink(verificationToken);

        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        ctx.setVariable("verificationLink", verificationLink);
        ctx.setVariable("platformName", "CollabCode");
        ctx.setVariable("supportEmail", "support@collabcode.io");

        String html = templateEngine.process("welcome-email", ctx);
        doSend(toEmail, "Welcome to CollabCode – Please verify your email", html);
    }

    /**
     * Sends a standalone verification email (resend-verification flow).
     */
    public void sendVerificationEmail(String toEmail, String userName, String verificationToken) {
        String verificationLink = buildVerificationLink(verificationToken);

        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        ctx.setVariable("verificationLink", verificationLink);
        ctx.setVariable("platformName", "CollabCode");
        ctx.setVariable("supportEmail", "support@collabcode.io");

        String html = templateEngine.process("email-verification", ctx);
        doSend(toEmail, "Verify your CollabCode email address", html);
    }

    /**
     * Sends a password-reset email containing a one-time link.
     */
    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        String resetLink = baseUrl + "/api/v1/auth/reset-password?token=" + resetToken;

        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        ctx.setVariable("resetLink", resetLink);
        ctx.setVariable("platformName", "CollabCode");
        ctx.setVariable("supportEmail", "support@collabcode.io");

        String html = templateEngine.process("password-reset", ctx);
        doSend(toEmail, "Reset your CollabCode password", html);
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private String buildVerificationLink(String token) {
        return baseUrl + "/api/v1/auth/verify-email?token=" + token;
    }

    private void doSend(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent to {} – subject: {}", to, subject);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        }
    }
}
