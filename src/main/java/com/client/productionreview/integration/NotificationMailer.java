package com.client.productionreview.integration;

import com.client.productionreview.config.AsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** Envia os e-mails de notificação em segundo plano; falha no SMTP vira log, nunca erro para quem chamou. */
@Slf4j
@Component
public class NotificationMailer {

    private final MailIntegration mailIntegration;

    public NotificationMailer(MailIntegration mailIntegration) {
        this.mailIntegration = mailIntegration;
    }

    @Async(AsyncConfig.MAIL_EXECUTOR)
    public void send(String to, String subject, String body) {
        try {
            mailIntegration.send(to, body, subject);
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail de notificação para {}: {}", to, e.getMessage());
        }
    }
}
