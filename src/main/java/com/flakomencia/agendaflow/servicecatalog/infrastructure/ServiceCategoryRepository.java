package com.flakomencia.agendaflow.servicecatalog.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flakomencia.agendaflow.servicecatalog.domain.ServiceCategory;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    List<ServiceCategory> findAllByOrganization_IdOrderByNameAsc(Long organizationId);

    Optional<ServiceCategory> findByIdAndOrganization_Id(Long id, Long organizationId);

    boolean existsByOrganization_IdAndName(Long organizationId, String name);

    boolean existsByOrganization_IdAndNameAndIdNot(Long organizationId, String name, Long id);
}
