package com.flakomencia.agendaflow.specialist.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flakomencia.agendaflow.specialist.domain.SpecialistServiceAssignment;
import com.flakomencia.agendaflow.specialist.domain.SpecialistServiceId;

public interface SpecialistServiceAssignmentRepository
        extends JpaRepository<SpecialistServiceAssignment, SpecialistServiceId> {

    List<SpecialistServiceAssignment> findAllBySpecialist_IdAndSpecialist_Organization_IdOrderByService_NameAsc(
            Long specialistId, Long organizationId);

    Optional<SpecialistServiceAssignment> findByIdAndSpecialist_Organization_Id(
            SpecialistServiceId id, Long organizationId);
}
