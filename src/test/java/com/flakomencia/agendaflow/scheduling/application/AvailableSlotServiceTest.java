package com.flakomencia.agendaflow.scheduling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.flakomencia.agendaflow.appointment.domain.Appointment;
import com.flakomencia.agendaflow.appointment.infrastructure.AppointmentRepository;
import com.flakomencia.agendaflow.branch.domain.Branch;
import com.flakomencia.agendaflow.branch.infrastructure.BranchRepository;
import com.flakomencia.agendaflow.common.security.AuthenticatedOrganizationContext;
import com.flakomencia.agendaflow.common.security.TenantAccessGuard;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.infrastructure.OrganizationRepository;
import com.flakomencia.agendaflow.scheduling.domain.AvailabilitySchedule;
import com.flakomencia.agendaflow.scheduling.domain.ScheduleBlock;
import com.flakomencia.agendaflow.scheduling.infrastructure.AvailabilityScheduleRepository;
import com.flakomencia.agendaflow.scheduling.infrastructure.ScheduleBlockRepository;
import com.flakomencia.agendaflow.servicecatalog.domain.BranchServiceAssignment;
import com.flakomencia.agendaflow.servicecatalog.domain.CatalogService;
import com.flakomencia.agendaflow.servicecatalog.infrastructure.BranchServiceAssignmentRepository;
import com.flakomencia.agendaflow.servicecatalog.infrastructure.CatalogServiceRepository;
import com.flakomencia.agendaflow.specialist.domain.Specialist;
import com.flakomencia.agendaflow.specialist.domain.SpecialistBranchAssignment;
import com.flakomencia.agendaflow.specialist.domain.SpecialistServiceAssignment;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistBranchAssignmentRepository;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistRepository;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistServiceAssignmentRepository;

class AvailableSlotServiceTest {
    private OrganizationRepository organizations; private BranchRepository branches;
    private CatalogServiceRepository services; private BranchServiceAssignmentRepository branchServices;
    private SpecialistRepository specialists; private SpecialistBranchAssignmentRepository specialistBranches;
    private SpecialistServiceAssignmentRepository specialistServices; private AvailabilityScheduleRepository schedules;
    private ScheduleBlockRepository blocks; private AppointmentRepository appointments;
    private AvailableSlotService service;
    private CatalogService catalogService; private Specialist specialist;

    @BeforeEach
    void setUp() {
        organizations=mock(OrganizationRepository.class); branches=mock(BranchRepository.class);
        services=mock(CatalogServiceRepository.class); branchServices=mock(BranchServiceAssignmentRepository.class);
        specialists=mock(SpecialistRepository.class); specialistBranches=mock(SpecialistBranchAssignmentRepository.class);
        specialistServices=mock(SpecialistServiceAssignmentRepository.class); schedules=mock(AvailabilityScheduleRepository.class);
        blocks=mock(ScheduleBlockRepository.class); appointments=mock(AppointmentRepository.class);
        TenantAccessGuard tenants=mock(TenantAccessGuard.class);
        when(tenants.current()).thenReturn(new AuthenticatedOrganizationContext(1L,1L,1L,Set.of("PLATFORM_ADMIN"),Set.of()));
        service = new AvailableSlotService(organizations, branches, services, branchServices, specialists,
                specialistBranches, specialistServices, schedules, blocks, appointments, tenants, 15);
        configureValidContext(30, 0, 0, null, 1);
    }

    @Test void calculatesNormalAvailabilityInBranchTimezone() {
        var response=service.findSlots(1L,2L,3L,LocalDate.of(2026,8,17),4L);
        assertThat(response.timezone()).isEqualTo("America/El_Salvador");
        assertThat(response.slots()).hasSize(3);
        assertThat(response.slots().getFirst().start().getOffset().toString()).isEqualTo("-06:00");
    }
    @Test void scheduleBlockRemovesSlots() {
        when(blocks.findEffectiveBlocks(anyLong(),anyLong(),anyLong(),any(),any())).thenReturn(List.of(mock(ScheduleBlock.class)));
        assertThat(service.findSlots(1L,2L,3L,LocalDate.of(2026,8,17),4L).slots()).isEmpty();
    }
    @Test void existingAppointmentConsumesCapacityButAdjacentIntervalRemainsValid() {
        Appointment existing=mock(Appointment.class);
        when(existing.getId()).thenReturn(99L); when(existing.getService()).thenReturn(catalogService);
        when(existing.getStartsAt()).thenReturn(OffsetDateTime.parse("2026-08-17T09:00:00-06:00"));
        when(existing.getEndsAt()).thenReturn(OffsetDateTime.parse("2026-08-17T09:30:00-06:00"));
        when(appointments.findAllByOrganization_IdAndSpecialist_IdAndDeletedAtIsNullAndStatusNotIn(anyLong(),anyLong(),any()))
                .thenReturn(List.of(existing));
        var slots=service.findSlots(1L,2L,3L,LocalDate.of(2026,8,17),4L).slots();
        assertThat(slots).extracting(slot -> slot.start().toLocalTime()).containsExactly(LocalTime.of(9,30));
    }
    @Test void customDurationOverridesServiceDuration() {
        configureValidContext(30,0,0,45,1);
        assertThat(service.findSlots(1L,2L,3L,LocalDate.of(2026,8,17),4L).slots()).hasSize(2)
                .allMatch(slot -> java.time.Duration.between(slot.start(),slot.end()).toMinutes()==45);
    }
    @Test void preparationAndCleanupBuffersMustFitAndPreventConflicts() {
        configureValidContext(30,10,10,null,1);
        assertThat(service.findSlots(1L,2L,3L,LocalDate.of(2026,8,17),4L).slots())
                .extracting(slot -> slot.start().toLocalTime()).containsExactly(LocalTime.of(9,15));
    }
    @Test void capacityGreaterThanOneAllowsOverlappingAppointment() {
        configureValidContext(30,0,0,null,2);
        Appointment existing=mock(Appointment.class); when(existing.getId()).thenReturn(99L); when(existing.getService()).thenReturn(catalogService);
        when(existing.getStartsAt()).thenReturn(OffsetDateTime.parse("2026-08-17T09:00:00-06:00"));
        when(existing.getEndsAt()).thenReturn(OffsetDateTime.parse("2026-08-17T09:30:00-06:00"));
        when(appointments.findAllByOrganization_IdAndSpecialist_IdAndDeletedAtIsNullAndStatusNotIn(anyLong(),anyLong(),any())).thenReturn(List.of(existing));
        assertThat(service.findSlots(1L,2L,3L,LocalDate.of(2026,8,17),4L).slots().getFirst().start().toLocalTime())
                .isEqualTo(LocalTime.of(9,0));
    }
    @Test void specialistNotEnabledForServiceProducesNoSlots() {
        when(specialistServices.findByIdAndSpecialist_Organization_Id(any(),anyLong())).thenReturn(Optional.empty());
        assertThat(service.findSlots(1L,2L,3L,LocalDate.of(2026,8,17),4L).slots()).isEmpty();
    }
    @Test void unavailableExactSlotIsRejected() {
        assertThatThrownBy(() -> service.requireAvailable(1L,2L,3L,4L,
                OffsetDateTime.parse("2026-08-17T09:07:00-06:00"),null)).isInstanceOf(SlotNotAvailableException.class);
    }

    private void configureValidContext(int duration,int preparation,int cleanup,Integer customDuration,int capacity) {
        Organization organization=mock(Organization.class); when(organization.getTimezone()).thenReturn("UTC");
        Branch branch=mock(Branch.class); when(branch.getId()).thenReturn(2L); when(branch.getActive()).thenReturn(true);
        when(branch.getTimezone()).thenReturn("America/El_Salvador");
        catalogService=mock(CatalogService.class); when(catalogService.getId()).thenReturn(3L); when(catalogService.getActive()).thenReturn(true);
        when(catalogService.getDurationMinutes()).thenReturn(duration); when(catalogService.getPreparationMinutes()).thenReturn(preparation);
        when(catalogService.getCleanupMinutes()).thenReturn(cleanup);
        specialist=mock(Specialist.class); when(specialist.getId()).thenReturn(4L); when(specialist.getActive()).thenReturn(true);
        when(specialist.getSimultaneousCapacity()).thenReturn(capacity);
        BranchServiceAssignment bs=mock(BranchServiceAssignment.class); when(bs.getActive()).thenReturn(true);
        SpecialistBranchAssignment sb=mock(SpecialistBranchAssignment.class); when(sb.getActive()).thenReturn(true);
        SpecialistServiceAssignment ss=mock(SpecialistServiceAssignment.class); when(ss.getActive()).thenReturn(true);
        when(ss.getCustomDurationMinutes()).thenReturn(customDuration);
        AvailabilitySchedule schedule=mock(AvailabilitySchedule.class); when(schedule.getStartTime()).thenReturn(LocalTime.of(9,0));
        when(schedule.getEndTime()).thenReturn(LocalTime.of(10,0));
        when(organizations.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(organization));
        when(branches.findByIdAndOrganization_IdAndDeletedAtIsNull(2L,1L)).thenReturn(Optional.of(branch));
        when(services.findByIdAndOrganization_IdAndDeletedAtIsNull(3L,1L)).thenReturn(Optional.of(catalogService));
        when(branchServices.findByIdAndBranch_Organization_Id(any(),anyLong())).thenReturn(Optional.of(bs));
        when(specialists.findByIdAndOrganization_IdAndDeletedAtIsNull(4L,1L)).thenReturn(Optional.of(specialist));
        when(specialistBranches.findByIdAndSpecialist_Organization_Id(any(),anyLong())).thenReturn(Optional.of(sb));
        when(specialistServices.findByIdAndSpecialist_Organization_Id(any(),anyLong())).thenReturn(Optional.of(ss));
        when(schedules.findAllByOrganization_IdAndSpecialist_IdAndBranch_IdAndDayOfWeekAndActiveTrue(1L,4L,2L,(short)1))
                .thenReturn(List.of(schedule));
        when(blocks.findEffectiveBlocks(anyLong(),anyLong(),anyLong(),any(),any())).thenReturn(List.of());
        when(appointments.findAllByOrganization_IdAndSpecialist_IdAndDeletedAtIsNullAndStatusNotIn(anyLong(),anyLong(),any()))
                .thenReturn(List.of());
    }
}
