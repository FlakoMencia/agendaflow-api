package com.flakomencia.agendaflow.appointment.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flakomencia.agendaflow.appointment.api.AppointmentCancelRequest;
import com.flakomencia.agendaflow.appointment.api.AppointmentCreateRequest;
import com.flakomencia.agendaflow.appointment.api.AppointmentRescheduleRequest;
import com.flakomencia.agendaflow.appointment.api.AppointmentResponse;
import com.flakomencia.agendaflow.appointment.api.AppointmentStatusHistoryResponse;
import com.flakomencia.agendaflow.appointment.domain.Appointment;
import com.flakomencia.agendaflow.appointment.domain.AppointmentStatus;
import com.flakomencia.agendaflow.appointment.domain.AppointmentStatusHistory;
import com.flakomencia.agendaflow.appointment.infrastructure.AppointmentRepository;
import com.flakomencia.agendaflow.appointment.infrastructure.AppointmentStatusHistoryRepository;
import com.flakomencia.agendaflow.common.api.PageResponse;
import com.flakomencia.agendaflow.common.exception.InvalidRequestException;
import com.flakomencia.agendaflow.common.security.TenantAccessGuard;
import com.flakomencia.agendaflow.customer.application.CustomerNotFoundException;
import com.flakomencia.agendaflow.customer.domain.Customer;
import com.flakomencia.agendaflow.customer.infrastructure.CustomerRepository;
import com.flakomencia.agendaflow.identity.domain.AppUser;
import com.flakomencia.agendaflow.identity.infrastructure.AppUserRepository;
import com.flakomencia.agendaflow.notification.application.AppointmentNotificationOutboxService;
import com.flakomencia.agendaflow.notification.domain.AppointmentNotificationEventType;
import com.flakomencia.agendaflow.scheduling.application.AvailableSlotService;
import com.flakomencia.agendaflow.scheduling.application.BookingSlot;
import com.flakomencia.agendaflow.scheduling.application.SlotNotAvailableException;
import com.flakomencia.agendaflow.specialist.application.SpecialistNotFoundException;
import com.flakomencia.agendaflow.specialist.domain.Specialist;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;

@Service
public class AppointmentApplicationService {
    private static final Set<String> SORTS = Set.of("id", "startsAt", "endsAt", "status", "createdAt", "updatedAt");
    private final AppointmentRepository appointments;
    private final AppointmentStatusHistoryRepository history;
    private final CustomerRepository customers;
    private final SpecialistRepository specialists;
    private final AppUserRepository users;
    private final AvailableSlotService slots;
    private final AppointmentStatusPolicy statusPolicy;
    private final AppointmentMapper mapper;
    private final TenantAccessGuard tenants;
    private final EntityManager entityManager;
    private final Clock clock;
    private final AppointmentNotificationOutboxService notificationOutbox;

    public AppointmentApplicationService(AppointmentRepository appointments,
            AppointmentStatusHistoryRepository history, CustomerRepository customers,
            SpecialistRepository specialists, AppUserRepository users, AvailableSlotService slots,
            AppointmentStatusPolicy statusPolicy, AppointmentMapper mapper, TenantAccessGuard tenants,
            EntityManager entityManager, Clock clock, AppointmentNotificationOutboxService notificationOutbox) {
        this.appointments = appointments; this.history = history; this.customers = customers;
        this.specialists = specialists; this.users = users; this.slots = slots; this.statusPolicy = statusPolicy;
        this.mapper = mapper; this.tenants = tenants; this.entityManager = entityManager; this.clock = clock;
        this.notificationOutbox = notificationOutbox;
    }

    @Transactional
    public AppointmentResponse create(Long organizationId, AppointmentCreateRequest request) {
        tenants.requireTenant(organizationId);
        lockSpecialist(organizationId, request.specialistId());
        Customer customer = customers.findByIdAndOrganization_IdAndDeletedAtIsNull(request.customerId(), organizationId)
                .orElseThrow(() -> new CustomerNotFoundException(organizationId, request.customerId()));
        if (!Boolean.TRUE.equals(customer.getActive())) throw new CustomerNotFoundException(organizationId, request.customerId());
        BookingSlot slot = requireBookingSlot(organizationId, request.branchId(), request.serviceId(),
                request.specialistId(), request.startsAt(), null);
        AppUser actor = currentActor();
        Appointment appointment = new Appointment(slot.organization(), slot.branch(), customer, slot.service(),
                slot.specialist(), slot.startsAt(), slot.endsAt(), actor);
        appointment.setCustomerNotes(optional(request.customerNotes()));
        appointment.setInternalNotes(optional(request.internalNotes()));
        appointments.saveAndFlush(appointment);
        history.save(new AppointmentStatusHistory(appointment, null, AppointmentStatus.PENDING, actor, "CREATED"));
        notificationOutbox.enqueue(appointment, AppointmentNotificationEventType.APPOINTMENT_CREATED, now());
        entityManager.flush(); entityManager.refresh(appointment);
        return mapper.toResponse(appointment);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long organizationId, Long appointmentId) {
        tenants.requireTenant(organizationId);
        return mapper.toResponse(requireAppointment(organizationId, appointmentId));
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> list(Long organizationId, OffsetDateTime from, OffsetDateTime to,
            Long branchId, Long specialistId, Long customerId, AppointmentStatus status, Pageable pageable) {
        tenants.requireTenant(organizationId); validateRange(from, to); validateSort(pageable);
        Page<Appointment> page = appointments.findAll(specification(organizationId, from, to, branchId,
                specialistId, customerId, status), pageable);
        return PageResponse.from(page, mapper::toResponse);
    }

    @Transactional
    public AppointmentResponse reschedule(Long organizationId, Long appointmentId, AppointmentRescheduleRequest request) {
        tenants.requireTenant(organizationId);
        Appointment appointment = appointments.findForUpdate(organizationId, appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(organizationId, appointmentId));
        statusPolicy.requireReschedulable(appointment.getStatus());
        Long targetSpecialistId = request.specialistId() == null ? appointment.getSpecialist().getId() : request.specialistId();
        lockSpecialist(organizationId, targetSpecialistId);
        BookingSlot slot = requireBookingSlot(organizationId, appointment.getBranch().getId(),
                appointment.getService().getId(), targetSpecialistId, request.startsAt(), appointment.getId());
        AppUser actor = currentActor();
        AppointmentStatus unchangedStatus = appointment.getStatus();
        appointment.reschedule(slot.specialist(), slot.startsAt(), slot.endsAt(), actor);
        history.save(new AppointmentStatusHistory(appointment, unchangedStatus, unchangedStatus, actor,
                "RESCHEDULED" + reasonSuffix(request.reason())));
        notificationOutbox.enqueue(appointment, AppointmentNotificationEventType.APPOINTMENT_RESCHEDULED, now());
        entityManager.flush(); entityManager.refresh(appointment);
        return mapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse cancel(Long organizationId, Long appointmentId, AppointmentCancelRequest request) {
        tenants.requireTenant(organizationId);
        Appointment appointment = appointments.findForUpdate(organizationId, appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(organizationId, appointmentId));
        statusPolicy.requireCancellable(appointment.getStatus());
        lockSpecialist(organizationId, appointment.getSpecialist().getId());
        AppointmentStatus previous = appointment.getStatus();
        AppUser actor = currentActor();
        String reason = optional(request.reason());
        appointment.cancel(reason, now(), actor);
        history.save(new AppointmentStatusHistory(appointment, previous, AppointmentStatus.CANCELLED, actor, reason));
        notificationOutbox.enqueue(appointment, AppointmentNotificationEventType.APPOINTMENT_CANCELLED, now());
        entityManager.flush(); entityManager.refresh(appointment);
        return mapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse confirm(Long organizationId, Long appointmentId) {
        return transition(organizationId, appointmentId, AppointmentStatus.CONFIRMED);
    }

    @Transactional
    public AppointmentResponse checkIn(Long organizationId, Long appointmentId) {
        return transition(organizationId, appointmentId, AppointmentStatus.CHECKED_IN);
    }

    @Transactional
    public AppointmentResponse start(Long organizationId, Long appointmentId) {
        return transition(organizationId, appointmentId, AppointmentStatus.IN_PROGRESS);
    }

    @Transactional
    public AppointmentResponse complete(Long organizationId, Long appointmentId) {
        return transition(organizationId, appointmentId, AppointmentStatus.COMPLETED);
    }

    @Transactional
    public AppointmentResponse markNoShow(Long organizationId, Long appointmentId) {
        return transition(organizationId, appointmentId, AppointmentStatus.NO_SHOW);
    }

    @Transactional(readOnly = true)
    public List<AppointmentStatusHistoryResponse> history(Long organizationId, Long appointmentId) {
        tenants.requireTenant(organizationId); requireAppointment(organizationId, appointmentId);
        return history.findAllByAppointment_IdAndAppointment_Organization_IdOrderByChangedAtAsc(appointmentId, organizationId)
                .stream().map(mapper::toResponse).toList();
    }

    private BookingSlot requireBookingSlot(Long organizationId, Long branchId, Long serviceId,
            Long specialistId, OffsetDateTime start, Long excludedId) {
        try { return slots.requireAvailable(organizationId, branchId, serviceId, specialistId, start, excludedId); }
        catch (SlotNotAvailableException exception) { throw new AppointmentConflictException(); }
    }
    private AppointmentResponse transition(Long organizationId, Long appointmentId, AppointmentStatus target) {
        tenants.requireTenant(organizationId);
        Appointment appointment = appointments.findForUpdate(organizationId, appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(organizationId, appointmentId));
        OffsetDateTime occurredAt = now();
        AppointmentStatus previous = appointment.getStatus();
        AppUser actor = currentActor();
        switch (target) {
            case CONFIRMED -> { statusPolicy.requireConfirmable(previous); appointment.confirm(occurredAt, actor); }
            case CHECKED_IN -> { statusPolicy.requireCheckIn(previous); appointment.checkIn(occurredAt, actor); }
            case IN_PROGRESS -> { statusPolicy.requireStartable(previous); appointment.start(occurredAt, actor); }
            case COMPLETED -> { statusPolicy.requireCompletable(previous); appointment.complete(occurredAt, actor); }
            case NO_SHOW -> {
                statusPolicy.requireNoShow(previous, appointment.getStartsAt(), occurredAt);
                appointment.markNoShow(actor);
            }
            default -> throw new IllegalArgumentException("Unsupported lifecycle target " + target);
        }
        history.save(new AppointmentStatusHistory(appointment, previous, target, actor, target.name()));
        entityManager.flush(); entityManager.refresh(appointment);
        return mapper.toResponse(appointment);
    }
    private Specialist lockSpecialist(Long organizationId, Long specialistId) {
        return specialists.findForBooking(organizationId, specialistId)
                .orElseThrow(() -> new SpecialistNotFoundException(organizationId, specialistId));
    }
    private Appointment requireAppointment(Long organizationId, Long id) {
        return appointments.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new AppointmentNotFoundException(organizationId, id));
    }
    private AppUser currentActor() {
        Long userId = tenants.current().userId();
        return userId == null ? null : users.findByIdAndDeletedAtIsNull(userId).orElse(null);
    }
    private Specification<Appointment> specification(Long organizationId, OffsetDateTime from, OffsetDateTime to,
            Long branchId, Long specialistId, Long customerId, AppointmentStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("startsAt"), from));
            if (to != null) predicates.add(cb.lessThan(root.get("startsAt"), to));
            if (branchId != null) predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            if (specialistId != null) predicates.add(cb.equal(root.get("specialist").get("id"), specialistId));
            if (customerId != null) predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
    private void validateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && !to.isAfter(from)) throw new InvalidRequestException("to must be after from");
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(order -> !SORTS.contains(order.getProperty())))
            throw new InvalidRequestException("Unsupported appointment sort property");
    }
    private String optional(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String reasonSuffix(String reason) { String value = optional(reason); return value == null ? "" : ": " + value; }
    private OffsetDateTime now() { return OffsetDateTime.now(clock); }
}
