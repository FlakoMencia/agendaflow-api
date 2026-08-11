package com.flakomencia.agendaflow.notification.application;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.flakomencia.agendaflow.notification.config.NotificationIntegrationProperties;
import com.flakomencia.agendaflow.notification.infrastructure.NotificationOutboxPublisher;

@Component
@ConditionalOnProperty(name = "app.notification.outbox.enabled", havingValue = "true")
public class NotificationOutboxWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationOutboxWorker.class);
    private final NotificationOutboxClaimService claims;
    private final NotificationOutboxPublisher publisher;
    private final NotificationOutboxStateService states;
    private final NotificationIntegrationProperties properties;
    private final Clock clock;

    public NotificationOutboxWorker(NotificationOutboxClaimService claims, NotificationOutboxPublisher publisher,
            NotificationOutboxStateService states, NotificationIntegrationProperties properties, Clock clock) {
        this.claims = claims; this.publisher = publisher; this.states = states;
        this.properties = properties; this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.notification.outbox.poll-interval:5s}")
    public void process() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        int recovered = claims.recoverStale(now.minus(properties.outbox().processingTimeout()), now);
        if (recovered > 0) LOGGER.info("Recovered {} stale notification outbox claims", recovered);
        var events = claims.claim(properties.outbox().batchSize(), now);
        for (var event : events) {
            var result = publisher.publish(event);
            OffsetDateTime completedAt = OffsetDateTime.now(clock);
            if (result.accepted()) states.markPublished(event.getId(), completedAt);
            else states.markFailed(event.getId(), result.error(), completedAt,
                    properties.outbox().maxAttempts(), properties.outbox().retryBaseDelay());
        }
    }
}
