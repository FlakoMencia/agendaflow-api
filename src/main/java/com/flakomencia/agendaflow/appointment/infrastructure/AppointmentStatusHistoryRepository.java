package com.flakomencia.agendaflow.appointment.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flakomencia.agendaflow.appointment.domain.AppointmentStatusHistory;

public interface AppointmentStatusHistoryRepository extends JpaRepository<AppointmentStatusHistory, Long> {
    List<AppointmentStatusHistory> findAllByAppointment_IdAndAppointment_Organization_IdOrderByChangedAtAsc(
            Long appointmentId, Long organizationId);
}
