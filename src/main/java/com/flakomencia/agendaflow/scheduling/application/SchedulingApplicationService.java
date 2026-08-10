package com.flakomencia.agendaflow.scheduling.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flakomencia.agendaflow.branch.application.BranchNotFoundException;
import com.flakomencia.agendaflow.branch.domain.Branch;
import com.flakomencia.agendaflow.branch.infrastructure.BranchRepository;
import com.flakomencia.agendaflow.common.api.PageResponse;
import com.flakomencia.agendaflow.common.exception.ConflictException;
import com.flakomencia.agendaflow.common.exception.InvalidRequestException;
import com.flakomencia.agendaflow.common.security.TenantAccessGuard;
import com.flakomencia.agendaflow.identity.domain.AppUser;
import com.flakomencia.agendaflow.identity.infrastructure.AppUserRepository;
import com.flakomencia.agendaflow.organization.application.OrganizationNotFoundException;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.domain.OrganizationStatus;
import com.flakomencia.agendaflow.organization.infrastructure.OrganizationRepository;
import com.flakomencia.agendaflow.scheduling.api.AvailabilityCreateRequest;
import com.flakomencia.agendaflow.scheduling.api.AvailabilityResponse;
import com.flakomencia.agendaflow.scheduling.api.AvailabilityUpdateRequest;
import com.flakomencia.agendaflow.scheduling.api.ScheduleBlockCreateRequest;
import com.flakomencia.agendaflow.scheduling.api.ScheduleBlockResponse;
import com.flakomencia.agendaflow.scheduling.api.ScheduleBlockUpdateRequest;
import com.flakomencia.agendaflow.scheduling.domain.AvailabilitySchedule;
import com.flakomencia.agendaflow.scheduling.domain.ScheduleBlock;
import com.flakomencia.agendaflow.scheduling.infrastructure.AvailabilityScheduleRepository;
import com.flakomencia.agendaflow.scheduling.infrastructure.ScheduleBlockRepository;
import com.flakomencia.agendaflow.specialist.application.SpecialistNotFoundException;
import com.flakomencia.agendaflow.specialist.domain.Specialist;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistRepository;

import jakarta.persistence.EntityManager;

@Service
public class SchedulingApplicationService {

    private static final Set<String> BLOCK_SORTS = Set.of(
            "id", "blockType", "startsAt", "endsAt", "active", "createdAt", "updatedAt");

    private final AvailabilityScheduleRepository availability;
    private final ScheduleBlockRepository blocks;
    private final OrganizationRepository organizations;
    private final SpecialistRepository specialists;
    private final BranchRepository branches;
    private final AppUserRepository users;
    private final TenantAccessGuard tenantAccess;
    private final EntityManager entityManager;

    public SchedulingApplicationService(
            AvailabilityScheduleRepository availability,
            ScheduleBlockRepository blocks,
            OrganizationRepository organizations,
            SpecialistRepository specialists,
            BranchRepository branches,
            AppUserRepository users,
            TenantAccessGuard tenantAccess,
            EntityManager entityManager) {
        this.availability = availability;
        this.blocks = blocks;
        this.organizations = organizations;
        this.specialists = specialists;
        this.branches = branches;
        this.users = users;
        this.tenantAccess = tenantAccess;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> listAvailability(Long organizationId, Long specialistId) {
        requireOrganization(organizationId);
        requireSpecialist(organizationId, specialistId);
        return availability.findAllByOrganization_IdAndSpecialist_IdOrderByDayOfWeekAscStartTimeAsc(
                organizationId, specialistId).stream().map(this::availabilityResponse).toList();
    }

    @Transactional
    public AvailabilityResponse createAvailability(
            Long organizationId, Long specialistId, AvailabilityCreateRequest request) {
        Organization organization = requireOrganization(organizationId);
        Specialist specialist = requireSpecialist(organizationId, specialistId);
        Branch branch = requireBranch(organizationId, request.branchId());
        validateAvailabilityRange(request.startTime(), request.endTime(), request.validFrom(), request.validUntil());
        boolean active = request.active() == null || request.active();
        validateOverlap(organizationId, specialistId, branch.getId(), request.dayOfWeek(), request.startTime(),
                request.endTime(), request.validFrom(), request.validUntil(), active, null);
        AvailabilitySchedule schedule = new AvailabilitySchedule(organization, specialist);
        applyAvailability(schedule, branch, request.dayOfWeek(), request.startTime(), request.endTime(),
                request.validFrom(), request.validUntil(), active);
        availability.save(schedule);
        refresh(schedule);
        return availabilityResponse(schedule);
    }

    @Transactional
    public AvailabilityResponse updateAvailability(
            Long organizationId, Long specialistId, Long scheduleId, AvailabilityUpdateRequest request) {
        requireOrganization(organizationId);
        requireSpecialist(organizationId, specialistId);
        AvailabilitySchedule schedule = availability
                .findByIdAndOrganization_IdAndSpecialist_Id(scheduleId, organizationId, specialistId)
                .orElseThrow(() -> new AvailabilityNotFoundException(organizationId, specialistId, scheduleId));
        Branch branch = requireBranch(organizationId, request.branchId());
        validateAvailabilityRange(request.startTime(), request.endTime(), request.validFrom(), request.validUntil());
        boolean active = request.active() == null ? schedule.getActive() : request.active();
        validateOverlap(organizationId, specialistId, branch.getId(), request.dayOfWeek(), request.startTime(),
                request.endTime(), request.validFrom(), request.validUntil(), active, scheduleId);
        applyAvailability(schedule, branch, request.dayOfWeek(), request.startTime(), request.endTime(),
                request.validFrom(), request.validUntil(), active);
        refresh(schedule);
        return availabilityResponse(schedule);
    }

    @Transactional
    public ScheduleBlockResponse createBlock(
            Long organizationId, Long specialistId, ScheduleBlockCreateRequest request) {
        Organization organization = requireOrganization(organizationId);
        Specialist specialist = requireSpecialist(organizationId, specialistId);
        Branch branch = request.branchId() == null ? null : requireBranch(organizationId, request.branchId());
        validateBlockRange(request.startsAt(), request.endsAt());
        AppUser creator = users.findByIdAndDeletedAtIsNull(tenantAccess.current().userId())
                .orElseThrow(() -> new InvalidRequestException("Authenticated user no longer exists"));
        ScheduleBlock block = new ScheduleBlock(organization, specialist, creator);
        applyBlock(block, branch, request.blockType(), request.startsAt(), request.endsAt(), request.reason(),
                request.active() == null || request.active());
        blocks.save(block);
        refresh(block);
        return blockResponse(block);
    }

    @Transactional(readOnly = true)
    public PageResponse<ScheduleBlockResponse> listBlocks(
            Long organizationId, Long specialistId, Pageable pageable) {
        requireOrganization(organizationId);
        requireSpecialist(organizationId, specialistId);
        validateBlockSort(pageable);
        return PageResponse.from(blocks.findAllByOrganization_IdAndSpecialist_Id(
                organizationId, specialistId, pageable), this::blockResponse);
    }

    @Transactional(readOnly = true)
    public ScheduleBlockResponse getBlock(Long organizationId, Long specialistId, Long blockId) {
        requireOrganization(organizationId);
        requireSpecialist(organizationId, specialistId);
        return blockResponse(requireBlock(organizationId, specialistId, blockId));
    }

    @Transactional
    public ScheduleBlockResponse updateBlock(
            Long organizationId, Long specialistId, Long blockId, ScheduleBlockUpdateRequest request) {
        requireOrganization(organizationId);
        requireSpecialist(organizationId, specialistId);
        ScheduleBlock block = requireBlock(organizationId, specialistId, blockId);
        Branch branch = request.branchId() == null ? null : requireBranch(organizationId, request.branchId());
        validateBlockRange(request.startsAt(), request.endsAt());
        applyBlock(block, branch, request.blockType(), request.startsAt(), request.endsAt(), request.reason(),
                request.active() == null ? block.getActive() : request.active());
        refresh(block);
        return blockResponse(block);
    }

    private void applyAvailability(
            AvailabilitySchedule schedule, Branch branch, Short day, LocalTime start, LocalTime end,
            LocalDate validFrom, LocalDate validUntil, boolean active) {
        schedule.setBranch(branch);
        schedule.setDayOfWeek(day);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        schedule.setValidFrom(validFrom);
        schedule.setValidUntil(validUntil);
        schedule.setActive(active);
    }

    private void applyBlock(
            ScheduleBlock block, Branch branch, com.flakomencia.agendaflow.scheduling.domain.ScheduleBlockType type,
            OffsetDateTime startsAt, OffsetDateTime endsAt, String reason, boolean active) {
        block.setBranch(branch);
        block.setBlockType(type);
        block.setStartsAt(startsAt);
        block.setEndsAt(endsAt);
        block.setReason(optionalText(reason));
        block.setActive(active);
    }

    private void validateAvailabilityRange(
            LocalTime start, LocalTime end, LocalDate validFrom, LocalDate validUntil) {
        if (!start.isBefore(end)) throw new InvalidRequestException("Availability startTime must be before endTime");
        if (validFrom != null && validUntil != null && validUntil.isBefore(validFrom)) {
            throw new InvalidRequestException("Availability validUntil must not be before validFrom");
        }
    }

    private void validateOverlap(
            Long organizationId, Long specialistId, Long branchId, Short day, LocalTime start, LocalTime end,
            LocalDate validFrom, LocalDate validUntil, boolean active, Long currentId) {
        if (!active) return;
        List<AvailabilitySchedule> candidates = availability
                .findAllByOrganization_IdAndSpecialist_IdAndBranch_IdAndDayOfWeekAndActiveTrue(
                        organizationId, specialistId, branchId, day);
        for (AvailabilitySchedule existing : candidates) {
            if (Objects.equals(existing.getId(), currentId) || !dateRangesOverlap(
                    existing.getValidFrom(), existing.getValidUntil(), validFrom, validUntil)) continue;
            boolean exact = existing.getStartTime().equals(start) && existing.getEndTime().equals(end)
                    && Objects.equals(existing.getValidFrom(), validFrom)
                    && Objects.equals(existing.getValidUntil(), validUntil);
            if (exact) throw new ConflictException("RESOURCE_CONFLICT", "Identical availability already exists");
            if (start.isBefore(existing.getEndTime()) && existing.getStartTime().isBefore(end)) {
                throw new ConflictException("SCHEDULE_OVERLAP", "Availability overlaps an existing schedule");
            }
        }
    }

    private boolean dateRangesOverlap(LocalDate firstStart, LocalDate firstEnd, LocalDate secondStart, LocalDate secondEnd) {
        return (firstEnd == null || secondStart == null || !firstEnd.isBefore(secondStart))
                && (secondEnd == null || firstStart == null || !secondEnd.isBefore(firstStart));
    }

    private void validateBlockRange(OffsetDateTime start, OffsetDateTime end) {
        if (!start.isBefore(end)) throw new InvalidRequestException("Schedule block startsAt must be before endsAt");
    }

    private Organization requireOrganization(Long organizationId) {
        tenantAccess.requireTenant(organizationId);
        if (!tenantAccess.current().isPlatformAdministrator()) {
            return organizations.findByIdAndStatusAndDeletedAtIsNull(organizationId, OrganizationStatus.ACTIVE)
                    .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        }
        return organizations.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    private Specialist requireSpecialist(Long organizationId, Long specialistId) {
        return specialists.findByIdAndOrganization_IdAndDeletedAtIsNull(specialistId, organizationId)
                .orElseThrow(() -> new SpecialistNotFoundException(organizationId, specialistId));
    }

    private Branch requireBranch(Long organizationId, Long branchId) {
        return branches.findByIdAndOrganization_IdAndDeletedAtIsNull(branchId, organizationId)
                .orElseThrow(() -> new BranchNotFoundException(organizationId, branchId));
    }

    private ScheduleBlock requireBlock(Long organizationId, Long specialistId, Long blockId) {
        return blocks.findByIdAndOrganization_IdAndSpecialist_Id(blockId, organizationId, specialistId)
                .orElseThrow(() -> new ScheduleBlockNotFoundException(organizationId, specialistId, blockId));
    }

    private void validateBlockSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(order -> !BLOCK_SORTS.contains(order.getProperty()))) {
            throw new InvalidRequestException("Unsupported schedule block sort property");
        }
    }

    private AvailabilityResponse availabilityResponse(AvailabilitySchedule schedule) {
        return new AvailabilityResponse(schedule.getId(), schedule.getOrganization().getId(),
                schedule.getSpecialist().getId(), schedule.getBranch().getId(), schedule.getDayOfWeek(),
                schedule.getStartTime(), schedule.getEndTime(), schedule.getValidFrom(), schedule.getValidUntil(),
                schedule.getActive(), schedule.getCreatedAt(), schedule.getUpdatedAt());
    }

    private ScheduleBlockResponse blockResponse(ScheduleBlock block) {
        return new ScheduleBlockResponse(block.getId(), block.getOrganization().getId(), block.getSpecialist().getId(),
                block.getBranch() == null ? null : block.getBranch().getId(), block.getBlockType(), block.getStartsAt(),
                block.getEndsAt(), block.getReason(), block.getActive(),
                block.getCreatedBy() == null ? null : block.getCreatedBy().getId(), block.getCreatedAt(), block.getUpdatedAt());
    }

    private void refresh(Object entity) { entityManager.flush(); entityManager.refresh(entity); }
    private String optionalText(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
