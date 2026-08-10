package com.flakomencia.agendaflow.scheduling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.flakomencia.agendaflow.branch.domain.Branch;
import com.flakomencia.agendaflow.branch.infrastructure.BranchRepository;
import com.flakomencia.agendaflow.common.exception.ConflictException;
import com.flakomencia.agendaflow.common.exception.InvalidRequestException;
import com.flakomencia.agendaflow.common.security.AuthenticatedOrganizationContext;
import com.flakomencia.agendaflow.common.security.TenantAccessGuard;
import com.flakomencia.agendaflow.identity.domain.AppUser;
import com.flakomencia.agendaflow.identity.infrastructure.AppUserRepository;
import com.flakomencia.agendaflow.organization.application.OrganizationNotFoundException;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.infrastructure.OrganizationRepository;
import com.flakomencia.agendaflow.scheduling.api.AvailabilityCreateRequest;
import com.flakomencia.agendaflow.scheduling.api.ScheduleBlockCreateRequest;
import com.flakomencia.agendaflow.scheduling.domain.AvailabilitySchedule;
import com.flakomencia.agendaflow.scheduling.domain.ScheduleBlock;
import com.flakomencia.agendaflow.scheduling.domain.ScheduleBlockType;
import com.flakomencia.agendaflow.scheduling.infrastructure.AvailabilityScheduleRepository;
import com.flakomencia.agendaflow.scheduling.infrastructure.ScheduleBlockRepository;
import com.flakomencia.agendaflow.specialist.domain.Specialist;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistRepository;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class SchedulingApplicationServiceTest {

    @Mock AvailabilityScheduleRepository availability;
    @Mock ScheduleBlockRepository blocks;
    @Mock OrganizationRepository organizations;
    @Mock SpecialistRepository specialists;
    @Mock BranchRepository branches;
    @Mock AppUserRepository users;
    @Mock TenantAccessGuard tenantAccess;
    @Mock EntityManager entityManager;
    @Mock Organization organization;
    @Mock Specialist specialist;
    @Mock Branch branch;

    private SchedulingApplicationService subject;

    @BeforeEach
    void setUp() {
        subject = new SchedulingApplicationService(
                availability, blocks, organizations, specialists, branches, users, tenantAccess, entityManager);
        lenient().when(tenantAccess.current()).thenReturn(new AuthenticatedOrganizationContext(
                1L, 1L, 1L, Set.of("PLATFORM_ADMIN"), Set.of()));
        lenient().when(organizations.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(organization));
        lenient().when(specialists.findByIdAndOrganization_IdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(specialist));
        lenient().when(branches.findByIdAndOrganization_IdAndDeletedAtIsNull(20L, 1L)).thenReturn(Optional.of(branch));
        lenient().when(organization.getId()).thenReturn(1L);
        lenient().when(specialist.getId()).thenReturn(10L);
        lenient().when(branch.getId()).thenReturn(20L);
    }

    @Test
    void createsValidRecurringAvailability() {
        when(availability.findAllByOrganization_IdAndSpecialist_IdAndBranch_IdAndDayOfWeekAndActiveTrue(
                1L, 10L, 20L, (short) 1)).thenReturn(List.of());

        var response = subject.createAvailability(1L, 10L, new AvailabilityCreateRequest(
                20L, (short) 1, LocalTime.of(9, 0), LocalTime.of(12, 0),
                LocalDate.of(2026, 1, 1), null, true));

        assertThat(response.startTime()).isEqualTo(LocalTime.of(9, 0));
        verify(availability).save(any(AvailabilitySchedule.class));
    }

    @Test
    void rejectsInvalidAndOverlappingAvailability() {
        assertThatThrownBy(() -> subject.createAvailability(1L, 10L, new AvailabilityCreateRequest(
                20L, (short) 1, LocalTime.NOON, LocalTime.NOON, null, null, true)))
                .isInstanceOf(InvalidRequestException.class);

        AvailabilitySchedule existing = org.mockito.Mockito.mock(AvailabilitySchedule.class);
        when(existing.getId()).thenReturn(99L);
        when(existing.getStartTime()).thenReturn(LocalTime.of(9, 0));
        when(existing.getEndTime()).thenReturn(LocalTime.of(12, 0));
        when(availability.findAllByOrganization_IdAndSpecialist_IdAndBranch_IdAndDayOfWeekAndActiveTrue(
                1L, 10L, 20L, (short) 1)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> subject.createAvailability(1L, 10L, new AvailabilityCreateRequest(
                20L, (short) 1, LocalTime.of(11, 0), LocalTime.of(13, 0), null, null, true)))
                .isInstanceOf(ConflictException.class).hasMessageContaining("overlaps");
    }

    @Test
    void rejectsInvalidBranchAndCrossTenantAvailability() {
        when(branches.findByIdAndOrganization_IdAndDeletedAtIsNull(30L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> subject.createAvailability(1L, 10L, new AvailabilityCreateRequest(
                30L, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0), null, null, true)))
                .hasMessageContaining("Branch");

        doThrow(new OrganizationNotFoundException(2L)).when(tenantAccess).requireTenant(2L);
        assertThatThrownBy(() -> subject.createAvailability(2L, 10L, new AvailabilityCreateRequest(
                20L, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0), null, null, true)))
                .isInstanceOf(OrganizationNotFoundException.class);
    }

    @Test
    void createsBlockAndRejectsInvalidOrMissingResource() {
        AppUser user = org.mockito.Mockito.mock(AppUser.class);
        when(user.getId()).thenReturn(1L);
        when(users.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        OffsetDateTime start = OffsetDateTime.parse("2026-08-10T09:00:00-06:00");
        OffsetDateTime end = start.plusHours(1);

        var response = subject.createBlock(1L, 10L, new ScheduleBlockCreateRequest(
                20L, ScheduleBlockType.MEETING, start, end, "Planning", true));
        assertThat(response.blockType()).isEqualTo(ScheduleBlockType.MEETING);
        verify(blocks).save(any(ScheduleBlock.class));

        assertThatThrownBy(() -> subject.createBlock(1L, 10L, new ScheduleBlockCreateRequest(
                20L, ScheduleBlockType.MEETING, end, start, null, true)))
                .isInstanceOf(InvalidRequestException.class);

        when(blocks.findByIdAndOrganization_IdAndSpecialist_Id(404L, 1L, 10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> subject.getBlock(1L, 10L, 404L))
                .isInstanceOf(ScheduleBlockNotFoundException.class);
    }
}
