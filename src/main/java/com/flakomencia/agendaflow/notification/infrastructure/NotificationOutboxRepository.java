package com.flakomencia.agendaflow.notification.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flakomencia.agendaflow.notification.domain.NotificationOutbox;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
}
