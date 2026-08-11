package com.flakomencia.agendaflow.notification.application;

import org.springframework.http.HttpStatus;

import com.flakomencia.agendaflow.common.exception.ApiException;

public class NotificationOutboxWriteException extends ApiException {
    public NotificationOutboxWriteException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "NOTIFICATION_OUTBOX_WRITE_FAILED",
                "Appointment operation could not be committed durably");
    }
}
