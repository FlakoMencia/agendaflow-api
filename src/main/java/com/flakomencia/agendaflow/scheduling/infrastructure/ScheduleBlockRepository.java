package com.flakomencia.agendaflow.scheduling.infrastructure;

import java.util.Optional;
import java.util.List;
import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flakomencia.agendaflow.scheduling.domain.ScheduleBlock;

public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, Long> {

    Page<ScheduleBlock> findAllByOrganization_IdAndSpecialist_Id(
            Long organizationId, Long specialistId, Pageable pageable);

    Optional<ScheduleBlock> findByIdAndOrganization_IdAndSpecialist_Id(
            Long id, Long organizationId, Long specialistId);

    @Query("""
            select block from ScheduleBlock block
            where block.organization.id = :organizationId
              and block.active = true
              and (block.specialist is null or block.specialist.id = :specialistId)
              and (block.branch is null or block.branch.id = :branchId)
              and block.startsAt < :endsAt
              and block.endsAt > :startsAt
            """)
    List<ScheduleBlock> findEffectiveBlocks(
            @Param("organizationId") Long organizationId,
            @Param("specialistId") Long specialistId,
            @Param("branchId") Long branchId,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt);
}
