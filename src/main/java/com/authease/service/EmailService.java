package com.authease.service;

import com.authease.config.AppProperties;
import com.authease.model.EmailOutbox;
import com.authease.repository.EmailOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final AppProperties appProperties;
    private final EmailOutboxRepository outboxRepository;
    private final Optional<JavaMailSender> mailSender;

    public EmailService(AppProperties appProperties,
                        EmailOutboxRepository outboxRepository,
                        @Autowired(required = false) Optional<JavaMailSender> mailSender) {
        this.appProperties = appProperties;
        this.outboxRepository = outboxRepository;
        this.mailSender = mailSender != null ? mailSender : Optional.empty();
    }

    public void sendEmail(String demoSessionId, String to, String subject, String body) {
        log.info("Sending email to: {}, Subject: {}", to, subject);

        // Record in Outbox (for Dev/Demo outbox panel and non-SMTP environments)
        try {
            EmailOutbox outbox = new EmailOutbox(demoSessionId, to, subject, body);
            outbox.setTs(Instant.now());
            outboxRepository.save(outbox);
        } catch (Exception e) {
            log.error("Failed to save email to outbox", e);
        }

        // If real JavaMailSender configured and not purely demo
        mailSender.ifPresent(sender -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                sender.send(message);
                log.info("Real SMTP message dispatched to {}", to);
            } catch (Exception e) {
                log.warn("SMTP send failed (saved in outbox instead): {}", e.getMessage());
            }
        });
    }

    public void sendVerificationEmail(String demoSessionId, String to, String rawToken) {
        String link = appProperties.getBaseUrl() + "/verify-email.html?token=" + rawToken;
        String subject = "Verify your AuthEase account";
        String body = "Welcome to AuthEase!\n\n"
                + "Please verify your email address by opening the following link:\n"
                + link + "\n\n"
                + "This link will expire in 24 hours.\n"
                + "If you did not sign up for AuthEase, you can safely ignore this email.";
        sendEmail(demoSessionId, to, subject, body);
    }

    public void sendAlreadyRegisteredEmail(String demoSessionId, String to) {
        String link = appProperties.getBaseUrl() + "/login.html";
        String subject = "AuthEase account sign-in notice";
        String body = "Hello,\n\n"
                + "Someone requested an account registration with this email address, but you already have an account.\n"
                + "You can sign in directly at:\n"
                + link + "\n\n"
                + "If you forgot your password, you can reset it from the sign-in page.";
        sendEmail(demoSessionId, to, subject, body);
    }

    public void sendEmailOtp(String demoSessionId, String to, String code) {
        String subject = "Your AuthEase verification code";
        String body = "Your one-time security code is:\n\n"
                + code + "\n\n"
                + "This code will expire in 5 minutes.\n"
                + "Never share this code with anyone.";
        sendEmail(demoSessionId, to, subject, body);
    }

    public void sendEmailApproval(String demoSessionId, String to, String rawToken) {
        String link = appProperties.getBaseUrl() + "/approve.html?token=" + rawToken;
        String subject = "Sign-in authorization needed - AuthEase";
        String body = "A high-risk sign-in attempt was detected for your account.\n\n"
                + "To approve or deny this sign-in attempt, open the link below:\n"
                + link + "\n\n"
                + "This link expires in 10 minutes.\n"
                + "If this was not you, select DENY to immediately protect your account.";
        sendEmail(demoSessionId, to, subject, body);
    }

    public void sendPasswordChangedAlert(String demoSessionId, String to) {
        String subject = "Your AuthEase password was changed";
        String body = "Your AuthEase password was recently changed.\n\n"
                + "All previous sessions and trusted devices have been signed out.\n"
                + "If you did not make this change, please contact support immediately.";
        sendEmail(demoSessionId, to, subject, body);
    }
}
