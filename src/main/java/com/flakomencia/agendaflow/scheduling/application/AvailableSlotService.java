package com.flakomencia.agendaflow.scheduling.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flakomencia.agendaflow.appointment.domain.Appointment;
import com.flakomencia.agendaflow.appointment.domain.AppointmentStatus;
import com.flakomencia.agendaflow.appointment.infrastructure.AppointmentRepository;
import com.flakomencia.agendaflow.branch.application.BranchNotFoundException;
import com.flakomencia.agendaflow.branch.domain.Branch;
import com.flakomencia.agendaflow.branch.infrastructure.BranchRepository;
import com.flakomencia.agendaflow.common.exception.InvalidRequestException;
import com.flakomencia.agendaflow.common.security.TenantAccessGuard;
import com.flakomencia.agendaflow.organization.application.OrganizationNotFoundException;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.domain.OrganizationStatus;
import com.flakomencia.agendaflow.organization.infrastructure.OrganizationRepository;
import com.flakomencia.agendaflow.scheduling.api.AvailableSlot;
import com.flakomencia.agendaflow.scheduling.api.AvailableSlotsResponse;
import com.flakomencia.agendaflow.scheduling.domain.AvailabilitySchedule;
import com.flakomencia.agendaflow.scheduling.infrastructure.AvailabilityScheduleRepository;
import com.flakomencia.agendaflow.scheduling.infrastructure.ScheduleBlockRepository;
import com.flakomencia.agendaflow.servicecatalog.application.CatalogServiceNotFoundException;
import com.flakomencia.agendaflow.servicecatalog.domain.BranchServiceId;
import com.flakomencia.agendaflow.servicecatalog.domain.CatalogService;
import com.flakomencia.agendaflow.servicecatalog.infrastructure.BranchServiceAssignmentRepository;
import com.flakomencia.agendaflow.servicecatalog.infrastructure.CatalogServiceRepository;
import com.flakomencia.agendaflow.specialist.application.SpecialistNotFoundException;
import com.flakomencia.agendaflow.specialist.domain.Specialist;
import com.flakomencia.agendaflow.specialist.domain.SpecialistBranchId;
import com.flakomencia.agendaflow.specialist.domain.SpecialistServiceAssignment;
import com.flakomencia.agendaflow.specialist.domain.SpecialistServiceId;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistBranchAssignmentRepository;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistRepository;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistServiceAssignmentRepository;

@Service
public class AvailableSlotService {
    private static final Set<AppointmentStatus> NON_CAPACITY_STATUSES =
            Set.of(AppointmentStatus.CANCELLED, AppointmentStatus.RESCHEDULED);

    private final OrganizationRepository organizations;
    private final BranchRepository branches;
    private final CatalogServiceRepository services;
    private final BranchServiceAssignmentRepository branchServices;
    private final SpecialistRepository specialists;
    private final SpecialistBranchAssignmentRepository specialistBranches;
    private final SpecialistServiceAssignmentRepository specialistServices;
    private final AvailabilityScheduleRepository schedules;
    private final ScheduleBlockRepository blocks;
    private final AppointmentRepository appointments;
    private final TenantAccessGuard tenants;
    private final int intervalMinutes;

    public AvailableSlotService(OrganizationRepository organizations, BranchRepository branches,
            CatalogServiceRepository services, BranchServiceAssignmentRepository branchServices,
            SpecialistRepository specialists, SpecialistBranchAssignmentRepository specialistBranches,
            SpecialistServiceAssignmentRepository specialistServices, AvailabilityScheduleRepository schedules,
            ScheduleBlockRepository blocks, AppointmentRepository appointments, TenantAccessGuard tenants,
            @Value("${agendaflow.scheduling.slot-interval-minutes:15}") int intervalMinutes) {
        if (intervalMinutes <= 0) throw new IllegalArgumentException("Slot interval must be positive");
        this.organizations = organizations; this.branches = branches; this.services = services;
        this.branchServices = branchServices; this.specialists = specialists;
        this.specialistBranches = specialistBranches; this.specialistServices = specialistServices;
        this.schedules = schedules; this.blocks = blocks; this.appointments = appointments;
        this.tenants = tenants; this.intervalMinutes = intervalMinutes;
    }

    @Transactional(readOnly = true)
    public AvailableSlotsResponse findSlots(Long organizationId, Long branchId, Long serviceId,
            LocalDate date, Long specialistId) {
        tenants.requireTenant(organizationId);
        Organization organization = requireOrganization(organizationId);
        Branch branch = requireActiveBranch(organizationId, branchId);
        CatalogService service = requireActiveService(organizationId, serviceId);
        requireActiveBranchService(organizationId, branchId, serviceId);
        ZoneId zone = timezone(branch, organization);
        List<Specialist> candidates = specialistId == null
                ? specialists.findAllByOrganization_IdAndActiveTrueAndDeletedAtIsNullOrderByProfessionalNameAsc(organizationId)
                : List.of(requireActiveSpecialist(organizationId, specialistId));
        List<AvailableSlot> result = new ArrayList<>();
        for (Specialist specialist : candidates) {
            SpecialistServiceAssignment assignment = activeAssignments(organizationId, branchId, serviceId, specialist);
            if (assignment == null) continue;
            addSlots(result, organizationId, branch, service, specialist, assignment, date, zone, null);
        }
        result.sort(Comparator.comparing(AvailableSlot::start).thenComparing(AvailableSlot::specialistId));
        return new AvailableSlotsResponse(date, branchId, serviceId, zone.getId(), result.stream().distinct().toList());
    }

    @Transactional(readOnly = true)
    public BookingSlot requireAvailable(Long organizationId, Long branchId, Long serviceId,
            Long specialistId, OffsetDateTime requestedStart, Long excludedAppointmentId) {
        tenants.requireTenant(organizationId);
        Organization organization = requireOrganization(organizationId);
        Branch branch = requireActiveBranch(organizationId, branchId);
        CatalogService service = requireActiveService(organizationId, serviceId);
        requireActiveBranchService(organizationId, branchId, serviceId);
        Specialist specialist = requireActiveSpecialist(organizationId, specialistId);
        SpecialistServiceAssignment assignment = activeAssignments(organizationId, branchId, serviceId, specialist);
        if (assignment == null) throw new SlotNotAvailableException();
        ZoneId zone = timezone(branch, organization);
        LocalDate date = requestedStart.atZoneSameInstant(zone).toLocalDate();
        List<AvailableSlot> result = new ArrayList<>();
        addSlots(result, organizationId, branch, service, specialist, assignment, date, zone, excludedAppointmentId);
        AvailableSlot match = result.stream()
                .filter(slot -> slot.start().toInstant().equals(requestedStart.toInstant()))
                .findFirst().orElseThrow(SlotNotAvailableException::new);
        return new BookingSlot(organization, branch, service, specialist, match.start(), match.end(),
                service.getPreparationMinutes(), service.getCleanupMinutes());
    }

    private void addSlots(List<AvailableSlot> target, Long organizationId, Branch branch, CatalogService service,
            Specialist specialist, SpecialistServiceAssignment assignment, LocalDate date, ZoneId zone,
            Long excludedAppointmentId) {
        short day = (short) (date.getDayOfWeek().getValue() % 7);
        int duration = assignment.getCustomDurationMinutes() == null
                ? service.getDurationMinutes() : assignment.getCustomDurationMinutes();
        List<AvailabilitySchedule> windows = schedules
                .findAllByOrganization_IdAndSpecialist_IdAndBranch_IdAndDayOfWeekAndActiveTrue(
                        organizationId, specialist.getId(), branch.getId(), day);
        for (AvailabilitySchedule window : windows) {
            if (!isEffective(window, date)) continue;
            OffsetDateTime windowStart = atZone(date.atTime(window.getStartTime()), zone);
            OffsetDateTime windowEnd = atZone(date.atTime(window.getEndTime()), zone);
            for (OffsetDateTime start = windowStart; !start.plusMinutes(duration).isAfter(windowEnd);
                    start = start.plusMinutes(intervalMinutes)) {
                OffsetDateTime end = start.plusMinutes(duration);
                OffsetDateTime blockedStart = start.minusMinutes(service.getPreparationMinutes());
                OffsetDateTime blockedEnd = end.plusMinutes(service.getCleanupMinutes());
                if (blockedStart.isBefore(windowStart) || blockedEnd.isAfter(windowEnd)) continue;
                if (hasScheduleBlock(organizationId, specialist.getId(), branch.getId(), blockedStart, blockedEnd)) continue;
                if (occupiedCapacity(organizationId, specialist, blockedStart, blockedEnd, excludedAppointmentId)) continue;
                target.add(new AvailableSlot(specialist.getId(), start, end));
            }
        }
    }

    private boolean occupiedCapacity(Long organizationId, Specialist specialist, OffsetDateTime start,
            OffsetDateTime end, Long excludedAppointmentId) {
        long overlapping = appointments
                .findAllByOrganization_IdAndSpecialist_IdAndDeletedAtIsNullAndStatusNotIn(
                        organizationId, specialist.getId(), NON_CAPACITY_STATUSES)
                .stream().filter(existing -> !existing.getId().equals(excludedAppointmentId))
                .filter(existing -> overlaps(existing.getStartsAt().minusMinutes(existing.getService().getPreparationMinutes()),
                        existing.getEndsAt().plusMinutes(existing.getService().getCleanupMinutes()), start, end))
                .count();
        return overlapping >= specialist.getSimultaneousCapacity();
    }

    private boolean hasScheduleBlock(Long organizationId, Long specialistId, Long branchId,
            OffsetDateTime start, OffsetDateTime end) {
        return !blocks.findEffectiveBlocks(organizationId, specialistId, branchId, start, end).isEmpty();
    }

    private SpecialistServiceAssignment activeAssignments(Long organizationId, Long branchId, Long serviceId,
            Specialist specialist) {
        var branchAssignment = specialistBranches
                .findByIdAndSpecialist_Organization_Id(new SpecialistBranchId(specialist.getId(), branchId), organizationId);
        var serviceAssignment = specialistServices
                .findByIdAndSpecialist_Organization_Id(new SpecialistServiceId(specialist.getId(), serviceId), organizationId);
        if (branchAssignment.isEmpty() || !Boolean.TRUE.equals(branchAssignment.get().getActive())
                || serviceAssignment.isEmpty() || !Boolean.TRUE.equals(serviceAssignment.get().getActive())) return null;
        return serviceAssignment.get();
    }

    private Organization requireOrganization(Long id) {
        if (!tenants.current().isPlatformAdministrator()) return organizations
                .findByIdAndStatusAndDeletedAtIsNull(id, OrganizationStatus.ACTIVE)
                .orElseThrow(() -> new OrganizationNotFoundException(id));
        return organizations.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new OrganizationNotFoundException(id));
    }
    private Branch requireActiveBranch(Long organizationId, Long id) {
        Branch branch = branches.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new BranchNotFoundException(organizationId, id));
        if (!Boolean.TRUE.equals(branch.getActive())) throw new SlotNotAvailableException();
        return branch;
    }
    private CatalogService requireActiveService(Long organizationId, Long id) {
        CatalogService service = services.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new CatalogServiceNotFoundException(organizationId, id));
        if (!Boolean.TRUE.equals(service.getActive())) throw new SlotNotAvailableException();
        return service;
    }
    private Specialist requireActiveSpecialist(Long organizationId, Long id) {
        Specialist specialist = specialists.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new SpecialistNotFoundException(organizationId, id));
        if (!Boolean.TRUE.equals(specialist.getActive())) throw new SlotNotAvailableException();
        return specialist;
    }
    private void requireActiveBranchService(Long organizationId, Long branchId, Long serviceId) {
        var assignment = branchServices.findByIdAndBranch_Organization_Id(new BranchServiceId(branchId, serviceId), organizationId);
        if (assignment.isEmpty() || !Boolean.TRUE.equals(assignment.get().getActive())) throw new SlotNotAvailableException();
    }
    private ZoneId timezone(Branch branch, Organization organization) {
        String value = branch.getTimezone() == null || branch.getTimezone().isBlank()
                ? organization.getTimezone() : branch.getTimezone();
        try { return ZoneId.of(value); }
        catch (RuntimeException exception) { throw new InvalidRequestException("Invalid branch or organization timezone"); }
    }
    private boolean isEffective(AvailabilitySchedule schedule, LocalDate date) {
        return (schedule.getValidFrom() == null || !date.isBefore(schedule.getValidFrom()))
                && (schedule.getValidUntil() == null || !date.isAfter(schedule.getValidUntil()));
    }
    private OffsetDateTime atZone(LocalDateTime dateTime, ZoneId zone) { return dateTime.atZone(zone).toOffsetDateTime(); }
    private boolean overlaps(OffsetDateTime aStart, OffsetDateTime aEnd, OffsetDateTime bStart, OffsetDateTime bEnd) {
        return aStart.toInstant().isBefore(bEnd.toInstant()) && aEnd.toInstant().isAfter(bStart.toInstant());
    }
}
