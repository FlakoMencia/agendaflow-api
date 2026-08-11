package com.flakomencia.agendaflow.appointment.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flakomencia.agendaflow.appointment.domain.Appointment;
import com.flakomencia.agendaflow.appointment.domain.AppointmentStatus;

import jakarta.persistence.LockModeType;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {
    @EntityGraph(attributePaths = {"organization", "branch", "customer", "service", "specialist"})
    Optional<Appointment> findByIdAndOrganization_IdAndDeletedAtIsNull(Long id, Long organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"organization", "branch", "customer", "service", "specialist"})
    @Query("""
            select appointment from Appointment appointment
            where appointment.id = :appointmentId
              and appointment.organization.id = :organizationId
              and appointment.deletedAt is null
            """)
    Optional<Appointment> findForUpdate(
            @Param("organizationId") Long organizationId,
            @Param("appointmentId") Long appointmentId);

    @EntityGraph(attributePaths = "service")
    List<Appointment> findAllByOrganization_IdAndSpecialist_IdAndDeletedAtIsNullAndStatusNotIn(
            Long organizationId, Long specialistId, Collection<AppointmentStatus> statuses);
}
