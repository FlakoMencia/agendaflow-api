package com.flakomencia.agendaflow.organization.infrastructure;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.domain.OrganizationStatus;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByIdAndDeletedAtIsNull(Long id);

    Page<Organization> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Organization> findAllByIdAndStatusAndDeletedAtIsNull(
            Long id, OrganizationStatus status, Pageable pageable);

    Optional<Organization> findByIdAndStatusAndDeletedAtIsNull(Long id, OrganizationStatus status);

    boolean existsByTaxIdentifierAndDeletedAtIsNull(String taxIdentifier);

    boolean existsByTaxIdentifierAndDeletedAtIsNullAndIdNot(String taxIdentifier, Long id);
}
