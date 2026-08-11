package com.flakomencia.agendaflow.notification.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.notification")
public record NotificationIntegrationProperties(URI baseUrl, Outbox outbox) {
    public NotificationIntegrationProperties {
        if (baseUrl == null) baseUrl = URI.create("http://localhost:8081");
        if (outbox == null) outbox = new Outbox(false, Duration.ofSeconds(5), 20, 5,
                Duration.ofSeconds(10), Duration.ofMinutes(5));
    }

    public record Outbox(boolean enabled, Duration pollInterval, int batchSize, int maxAttempts,
            Duration retryBaseDelay, Duration processingTimeout) {
        public Outbox {
            if (pollInterval == null || pollInterval.isNegative() || pollInterval.isZero()) pollInterval = Duration.ofSeconds(5);
            if (batchSize <= 0) batchSize = 20;
            if (maxAttempts <= 0) maxAttempts = 5;
            if (retryBaseDelay == null || retryBaseDelay.isNegative() || retryBaseDelay.isZero()) retryBaseDelay = Duration.ofSeconds(10);
            if (processingTimeout == null || processingTimeout.isNegative() || processingTimeout.isZero()) processingTimeout = Duration.ofMinutes(5);
        }
    }
}
