package com.flakomencia.agendaflow.notification.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.flakomencia.agendaflow.identity.application.ServiceAccessTokenService;
import com.flakomencia.agendaflow.notification.config.NotificationIntegrationProperties;
import com.flakomencia.agendaflow.notification.domain.NotificationOutbox;

import tools.jackson.databind.ObjectMapper;

@Component
public class NotificationOutboxPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationOutboxPublisher.class);
    private final RestClient client;
    private final ServiceAccessTokenService tokens;
    private final ObjectMapper json;

    public NotificationOutboxPublisher(RestClient.Builder builder, NotificationIntegrationProperties properties,
            ServiceAccessTokenService tokens, ObjectMapper json) {
        this.client = builder.baseUrl(properties.baseUrl().toString()).build();
        this.tokens = tokens;
        this.json = json;
    }

    public PublicationResult publish(NotificationOutbox event) {
        try {
            var response = client.post().uri("/api/v1/notification-requests")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(tokens.issueNotificationSubmissionToken().value()))
                    .body(json.readTree(event.getPayload()))
                    .exchange((request, result) -> result.getStatusCode());
            if (response == HttpStatus.ACCEPTED) return PublicationResult.success();
            return PublicationResult.failed("Notification service returned HTTP " + response.value());
        } catch (Exception exception) {
            LOGGER.warn("Notification outbox publication failed: outboxId={}, appointmentId={}, eventType={}, attempt={}",
                    event.getId(), event.getAggregateId(), event.getEventType(), event.getAttemptCount());
            return PublicationResult.failed(exception.getClass().getSimpleName());
        }
    }

    public record PublicationResult(boolean accepted, String error) {
        public static PublicationResult success() { return new PublicationResult(true, null); }
        public static PublicationResult failed(String error) { return new PublicationResult(false, error); }
    }
}
