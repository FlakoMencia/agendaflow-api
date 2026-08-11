package com.flakomencia.agendaflow.notification.application;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flakomencia.agendaflow.appointment.domain.Appointment;
import com.flakomencia.agendaflow.customer.domain.Customer;
import com.flakomencia.agendaflow.notification.domain.AppointmentNotificationEventType;
import com.flakomencia.agendaflow.notification.domain.NotificationOutbox;
import com.flakomencia.agendaflow.notification.infrastructure.NotificationOutboxRepository;

import tools.jackson.databind.ObjectMapper;

@Service
public class AppointmentNotificationOutboxService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentNotificationOutboxService.class);
    private static final String DEFAULT_LOCALE = "en-US";
    private static final java.util.regex.Pattern EMAIL =
            java.util.regex.Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final NotificationOutboxRepository outbox;
    private final ObjectMapper json;

    public AppointmentNotificationOutboxService(NotificationOutboxRepository outbox, ObjectMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    public void enqueue(Appointment appointment, AppointmentNotificationEventType eventType, OffsetDateTime occurredAt) {
        Customer customer = appointment.getCustomer();
        String recipient = normalizedEmail(customer);
        if (recipient == null || !Boolean.TRUE.equals(customer.getEmailConsent())) return;
        String locale = customer.getPreferredLanguage() == null || customer.getPreferredLanguage().isBlank()
                ? DEFAULT_LOCALE : customer.getPreferredLanguage().trim();
        try {
            NotificationOutbox event = new NotificationOutbox(appointment.getOrganization().getId(), appointment.getId(),
                    eventType, recipient, locale, occurredAt);
            outbox.saveAndFlush(event);
            event.setPayload(serialize(payload(event, appointment, eventType, occurredAt)));
            outbox.saveAndFlush(event);
        } catch (RuntimeException exception) {
            LOGGER.error("Durable notification event write failed: organizationId={}, appointmentId={}, eventType={}, error={}",
                    appointment.getOrganization().getId(), appointment.getId(), eventType, exception.getClass().getSimpleName());
            throw new NotificationOutboxWriteException();
        }
    }

    private AppointmentNotificationPayload payload(NotificationOutbox event, Appointment appointment,
            AppointmentNotificationEventType eventType, OffsetDateTime occurredAt) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("customerName", customerName(appointment.getCustomer()));
        variables.put("serviceName", appointment.getService().getName());
        variables.put("specialistName", appointment.getSpecialist().getProfessionalName());
        variables.put("appointmentDateTime", appointment.getStartsAt().toString());
        variables.put("branchName", appointment.getBranch().getName());
        return new AppointmentNotificationPayload(event.getId(), event.getOrganizationId(), event.getAggregateId(),
                eventType.payloadType(), event.getRecipient(), event.getLocale(), occurredAt, variables);
    }

    private String normalizedEmail(Customer customer) {
        if (customer.getEmail() == null || customer.getEmail().isBlank()) return null;
        String value = customer.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        return EMAIL.matcher(value).matches() ? value : null;
    }
    private String customerName(Customer customer) {
        return java.util.stream.Stream.of(customer.getFirstName(), customer.getMiddleName(),
                        customer.getLastName(), customer.getSecondLastName())
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining(" "));
    }
    private String serialize(AppointmentNotificationPayload payload) {
        try { return json.writeValueAsString(payload); }
        catch (Exception exception) { throw new IllegalStateException("Cannot serialize notification outbox payload", exception); }
    }
}
