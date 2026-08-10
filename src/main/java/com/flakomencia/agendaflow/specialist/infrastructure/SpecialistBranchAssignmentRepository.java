package com.flakomencia.agendaflow.specialist.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flakomencia.agendaflow.specialist.domain.SpecialistBranchAssignment;
import com.flakomencia.agendaflow.specialist.domain.SpecialistBranchId;

public interface SpecialistBranchAssignmentRepository
        extends JpaRepository<SpecialistBranchAssignment, SpecialistBranchId> {

    List<SpecialistBranchAssignment> findAllBySpecialist_IdAndSpecialist_Organization_IdOrderByBranch_NameAsc(
            Long specialistId, Long organizationId);

    Optional<SpecialistBranchAssignment> findByIdAndSpecialist_Organization_Id(
            SpecialistBranchId id, Long organizationId);
}
