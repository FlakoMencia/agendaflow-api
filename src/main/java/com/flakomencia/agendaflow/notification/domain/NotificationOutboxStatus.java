package com.flakomencia.agendaflow.notification.domain;

public enum NotificationOutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    EXHAUSTED
}
