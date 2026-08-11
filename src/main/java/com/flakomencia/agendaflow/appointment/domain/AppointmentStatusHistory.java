package com.flakomencia.agendaflow.appointment.domain;

import java.time.OffsetDateTime;

import com.flakomencia.agendaflow.identity.domain.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointment_status_history", schema = "agendaflow")
public class AppointmentStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, updatable = false) private Appointment appointment;
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", length = 30) private AppointmentStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false, length = 30) private AppointmentStatus newStatus;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "changed_by", updatable = false) private AppUser changedBy;
    @Column(name = "change_reason", updatable = false) private String changeReason;
    @Column(name = "changed_at", nullable = false, insertable = false, updatable = false) private OffsetDateTime changedAt;
    protected AppointmentStatusHistory() {}
    public AppointmentStatusHistory(Appointment appointment, AppointmentStatus previousStatus,
            AppointmentStatus newStatus, AppUser changedBy, String changeReason) {
        this.appointment = appointment; this.previousStatus = previousStatus; this.newStatus = newStatus;
        this.changedBy = changedBy; this.changeReason = changeReason;
    }
    public Long getId() { return id; }
    public Appointment getAppointment() { return appointment; }
    public AppointmentStatus getPreviousStatus() { return previousStatus; }
    public AppointmentStatus getNewStatus() { return newStatus; }
    public AppUser getChangedBy() { return changedBy; }
    public String getChangeReason() { return changeReason; }
    public OffsetDateTime getChangedAt() { return changedAt; }
}
