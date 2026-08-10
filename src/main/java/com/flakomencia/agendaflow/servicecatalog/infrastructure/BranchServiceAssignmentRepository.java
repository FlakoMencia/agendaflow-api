package com.flakomencia.agendaflow.servicecatalog.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flakomencia.agendaflow.servicecatalog.domain.BranchServiceAssignment;
import com.flakomencia.agendaflow.servicecatalog.domain.BranchServiceId;

public interface BranchServiceAssignmentRepository extends JpaRepository<BranchServiceAssignment, BranchServiceId> {

    List<BranchServiceAssignment> findAllByBranch_IdAndBranch_Organization_IdOrderByService_NameAsc(
            Long branchId, Long organizationId);

    Optional<BranchServiceAssignment> findByIdAndBranch_Organization_Id(
            BranchServiceId id, Long organizationId);
}
