package com.flakomencia.agendaflow.specialist.infrastructure;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.flakomencia.agendaflow.specialist.domain.Specialist;

public interface SpecialistRepository extends JpaRepository<Specialist, Long> {

    Page<Specialist> findAllByOrganization_IdAndDeletedAtIsNull(Long organizationId, Pageable pageable);

    Optional<Specialist> findByIdAndOrganization_IdAndDeletedAtIsNull(Long id, Long organizationId);

    boolean existsByOrganization_IdAndUser_IdAndDeletedAtIsNull(Long organizationId, Long userId);

    boolean existsByOrganization_IdAndUser_IdAndDeletedAtIsNullAndIdNot(
            Long organizationId, Long userId, Long id);
}
