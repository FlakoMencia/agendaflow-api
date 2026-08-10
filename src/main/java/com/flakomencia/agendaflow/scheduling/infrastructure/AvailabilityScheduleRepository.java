package com.flakomencia.agendaflow.scheduling.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flakomencia.agendaflow.scheduling.domain.AvailabilitySchedule;

public interface AvailabilityScheduleRepository extends JpaRepository<AvailabilitySchedule, Long> {

    List<AvailabilitySchedule> findAllByOrganization_IdAndSpecialist_IdOrderByDayOfWeekAscStartTimeAsc(
            Long organizationId, Long specialistId);

    Optional<AvailabilitySchedule> findByIdAndOrganization_IdAndSpecialist_Id(
            Long id, Long organizationId, Long specialistId);

    List<AvailabilitySchedule> findAllByOrganization_IdAndSpecialist_IdAndBranch_IdAndDayOfWeekAndActiveTrue(
            Long organizationId, Long specialistId, Long branchId, Short dayOfWeek);
}
