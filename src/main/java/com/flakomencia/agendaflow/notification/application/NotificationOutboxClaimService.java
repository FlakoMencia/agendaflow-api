package com.flakomencia.agendaflow.notification.application;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flakomencia.agendaflow.notification.domain.NotificationOutbox;
import com.flakomencia.agendaflow.notification.infrastructure.NotificationOutboxRepository;

import jakarta.persistence.EntityManager;

@Service
public class NotificationOutboxClaimService {
    private final EntityManager entityManager;
    private final NotificationOutboxRepository outbox;

    public NotificationOutboxClaimService(EntityManager entityManager, NotificationOutboxRepository outbox) {
        this.entityManager = entityManager;
        this.outbox = outbox;
    }

    @Transactional
    public List<NotificationOutbox> claim(int batchSize, OffsetDateTime now) {
        @SuppressWarnings("unchecked")
        List<Number> ids = entityManager.createNativeQuery("""
                SELECT id
                  FROM agendaflow.notification_outbox
                 WHERE status = 'PENDING'
                   AND next_attempt_at <= :now
                 ORDER BY next_attempt_at, id
                 FOR UPDATE SKIP LOCKED
                """).setParameter("now", now).setMaxResults(batchSize).getResultList();
        List<NotificationOutbox> claimed = ids.stream()
                .map(Number::longValue)
                .map(outbox::findById)
                .flatMap(java.util.Optional::stream)
                .toList();
        claimed.forEach(event -> event.claim(now));
        return claimed;
    }

    @Transactional
    public int recoverStale(OffsetDateTime staleBefore, OffsetDateTime now) {
        @SuppressWarnings("unchecked")
        List<Number> ids = entityManager.createNativeQuery("""
                SELECT id
                  FROM agendaflow.notification_outbox
                 WHERE status = 'PROCESSING'
                   AND processing_started_at < :staleBefore
                 FOR UPDATE SKIP LOCKED
                """).setParameter("staleBefore", staleBefore).getResultList();
        ids.stream().map(Number::longValue).map(outbox::findById).flatMap(java.util.Optional::stream)
                .forEach(event -> event.recover(now));
        return ids.size();
    }
}
