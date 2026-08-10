package com.flakomencia.agendaflow.scheduling.infrastructure;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.flakomencia.agendaflow.scheduling.domain.ScheduleBlock;

public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, Long> {

    Page<ScheduleBlock> findAllByOrganization_IdAndSpecialist_Id(
            Long organizationId, Long specialistId, Pageable pageable);

    Optional<ScheduleBlock> findByIdAndOrganization_IdAndSpecialist_Id(
            Long id, Long organizationId, Long specialistId);
}
