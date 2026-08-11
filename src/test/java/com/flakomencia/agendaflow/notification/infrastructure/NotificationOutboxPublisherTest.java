package com.flakomencia.agendaflow.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.flakomencia.agendaflow.identity.application.IssuedToken;
import com.flakomencia.agendaflow.identity.application.ServiceAccessTokenService;
import com.flakomencia.agendaflow.notification.config.NotificationIntegrationProperties;
import com.flakomencia.agendaflow.notification.domain.NotificationOutbox;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.ObjectMapper;

class NotificationOutboxPublisherTest {
    private HttpServer server;
    private final AtomicInteger status = new AtomicInteger(202);
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private NotificationOutboxPublisher publisher;
    private NotificationOutbox event;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/v1/notification-requests", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(status.get(), -1);
            exchange.close();
        });
        server.start();
        ServiceAccessTokenService tokens = mock(ServiceAccessTokenService.class);
        when(tokens.issueNotificationSubmissionToken()).thenReturn(new IssuedToken("service-token", 300));
        var outboxSettings = new NotificationIntegrationProperties.Outbox(false, Duration.ofSeconds(5), 10, 3,
                Duration.ofSeconds(10), Duration.ofMinutes(5));
        var properties = new NotificationIntegrationProperties(
                URI.create("http://localhost:" + server.getAddress().getPort()), outboxSettings);
        publisher = new NotificationOutboxPublisher(RestClient.builder(), properties, tokens, new ObjectMapper());
        event = mock(NotificationOutbox.class);
        when(event.getId()).thenReturn(19L);
        when(event.getAggregateId()).thenReturn(31L);
        when(event.getEventType()).thenReturn("APPOINTMENT_CREATED");
        when(event.getAttemptCount()).thenReturn(1);
        when(event.getPayload()).thenReturn("{\"eventId\":19,\"type\":\"CREATED\"}");
    }

    @AfterEach void tearDown() { server.stop(0); }

    @Test void publishesWithSubmissionTokenAndAcceptsOnly202() {
        var result = publisher.publish(event);
        assertThat(result.accepted()).isTrue();
        assertThat(authorization.get()).isEqualTo("Bearer service-token");
        assertThat(requestBody.get()).contains("\"eventId\":19");
    }

    @Test void doesNotAcceptAuthenticationAuthorizationOrServerFailures() {
        for (int responseStatus : new int[] {401, 403, 500}) {
            status.set(responseStatus);
            var result = publisher.publish(event);
            assertThat(result.accepted()).as("HTTP %s", responseStatus).isFalse();
            assertThat(result.error()).contains(Integer.toString(responseStatus));
        }
    }
}
