package com.flakomencia.agendaflow.servicecatalog.infrastructure;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.flakomencia.agendaflow.servicecatalog.domain.CatalogService;

public interface CatalogServiceRepository extends JpaRepository<CatalogService, Long> {

    Page<CatalogService> findAllByOrganization_IdAndDeletedAtIsNull(Long organizationId, Pageable pageable);

    Optional<CatalogService> findByIdAndOrganization_IdAndDeletedAtIsNull(Long id, Long organizationId);

    boolean existsByOrganization_IdAndNameAndDeletedAtIsNull(Long organizationId, String name);

    boolean existsByOrganization_IdAndNameAndDeletedAtIsNullAndIdNot(Long organizationId, String name, Long id);
}
