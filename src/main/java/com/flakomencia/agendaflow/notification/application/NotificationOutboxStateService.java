package com.flakomencia.agendaflow.notification.application;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flakomencia.agendaflow.notification.infrastructure.NotificationOutboxRepository;

@Service
public class NotificationOutboxStateService {
    private static final Duration MAXIMUM_BACKOFF = Duration.ofHours(1);
    private final NotificationOutboxRepository outbox;

    public NotificationOutboxStateService(NotificationOutboxRepository outbox) { this.outbox = outbox; }

    @Transactional
    public void markPublished(Long id, OffsetDateTime now) {
        outbox.findById(id).ifPresent(event -> event.publish(now));
    }

    @Transactional
    public void markFailed(Long id, String error, OffsetDateTime now, int maxAttempts, Duration baseDelay) {
        outbox.findById(id).ifPresent(event -> {
            long multiplier = 1L << Math.min(Math.max(event.getAttemptCount() - 1, 0), 12);
            Duration delay = baseDelay.multipliedBy(multiplier);
            if (delay.compareTo(MAXIMUM_BACKOFF) > 0) delay = MAXIMUM_BACKOFF;
            event.fail(now, now.plus(delay), safeError(error), maxAttempts);
        });
    }

    private String safeError(String error) {
        String value = error == null || error.isBlank() ? "Notification service request failed" : error;
        value = value.replaceAll("[\\r\\n]+", " ");
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
