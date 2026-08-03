package com.flakomencia.agendaflow.branch.infrastructure;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.flakomencia.agendaflow.branch.domain.Branch;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByIdAndOrganization_IdAndDeletedAtIsNull(Long id, Long organizationId);

    Page<Branch> findAllByOrganization_IdAndDeletedAtIsNull(Long organizationId, Pageable pageable);

    boolean existsByOrganization_IdAndName(Long organizationId, String name);

    boolean existsByOrganization_IdAndNameAndIdNot(Long organizationId, String name, Long id);

    boolean existsByOrganization_IdAndCode(Long organizationId, String code);

    boolean existsByOrganization_IdAndCodeAndIdNot(Long organizationId, String code, Long id);
}
