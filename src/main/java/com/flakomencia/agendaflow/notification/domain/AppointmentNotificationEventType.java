package com.flakomencia.agendaflow.notification.domain;

public enum AppointmentNotificationEventType {
    APPOINTMENT_CREATED("CREATED"),
    APPOINTMENT_RESCHEDULED("RESCHEDULED"),
    APPOINTMENT_CANCELLED("CANCELLED");

    private final String payloadType;

    AppointmentNotificationEventType(String payloadType) { this.payloadType = payloadType; }
    public String payloadType() { return payloadType; }
}
