package com.flakomencia.agendaflow.specialist.infrastructure;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flakomencia.agendaflow.specialist.domain.Specialist;

import jakarta.persistence.LockModeType;

public interface SpecialistRepository extends JpaRepository<Specialist, Long> {

    Page<Specialist> findAllByOrganization_IdAndDeletedAtIsNull(Long organizationId, Pageable pageable);

    Optional<Specialist> findByIdAndOrganization_IdAndDeletedAtIsNull(Long id, Long organizationId);

    List<Specialist> findAllByOrganization_IdAndActiveTrueAndDeletedAtIsNullOrderByProfessionalNameAsc(
            Long organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select specialist from Specialist specialist
            where specialist.id = :specialistId
              and specialist.organization.id = :organizationId
              and specialist.deletedAt is null
            """)
    Optional<Specialist> findForBooking(
            @Param("organizationId") Long organizationId,
            @Param("specialistId") Long specialistId);

    boolean existsByOrganization_IdAndUser_IdAndDeletedAtIsNull(Long organizationId, Long userId);

    boolean existsByOrganization_IdAndUser_IdAndDeletedAtIsNullAndIdNot(
            Long organizationId, Long userId, Long id);
}
