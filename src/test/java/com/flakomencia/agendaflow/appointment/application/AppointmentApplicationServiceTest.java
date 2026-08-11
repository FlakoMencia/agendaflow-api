package com.flakomencia.agendaflow.appointment.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.flakomencia.agendaflow.appointment.api.AppointmentCancelRequest;
import com.flakomencia.agendaflow.appointment.api.AppointmentCreateRequest;
import com.flakomencia.agendaflow.appointment.api.AppointmentRescheduleRequest;
import com.flakomencia.agendaflow.appointment.domain.Appointment;
import com.flakomencia.agendaflow.appointment.domain.AppointmentStatus;
import com.flakomencia.agendaflow.appointment.domain.AppointmentStatusHistory;
import com.flakomencia.agendaflow.appointment.infrastructure.AppointmentRepository;
import com.flakomencia.agendaflow.appointment.infrastructure.AppointmentStatusHistoryRepository;
import com.flakomencia.agendaflow.branch.domain.Branch;
import com.flakomencia.agendaflow.common.security.AuthenticatedOrganizationContext;
import com.flakomencia.agendaflow.common.security.TenantAccessGuard;
import com.flakomencia.agendaflow.customer.domain.Customer;
import com.flakomencia.agendaflow.customer.infrastructure.CustomerRepository;
import com.flakomencia.agendaflow.identity.infrastructure.AppUserRepository;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.notification.application.AppointmentNotificationOutboxService;
import com.flakomencia.agendaflow.notification.domain.AppointmentNotificationEventType;
import com.flakomencia.agendaflow.scheduling.application.AvailableSlotService;
import com.flakomencia.agendaflow.scheduling.application.BookingSlot;
import com.flakomencia.agendaflow.scheduling.application.SlotNotAvailableException;
import com.flakomencia.agendaflow.servicecatalog.domain.CatalogService;
import com.flakomencia.agendaflow.specialist.domain.Specialist;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistRepository;

import jakarta.persistence.EntityManager;

class AppointmentApplicationServiceTest {
    private AppointmentRepository appointments; private AppointmentStatusHistoryRepository history;
    private CustomerRepository customers; private SpecialistRepository specialists; private AvailableSlotService slots;
    private AppointmentApplicationService service; private BookingSlot booking; private Appointment appointment;
    private AppointmentNotificationOutboxService notificationOutbox;

    @BeforeEach
    void setUp() {
        appointments=mock(AppointmentRepository.class); history=mock(AppointmentStatusHistoryRepository.class);
        customers=mock(CustomerRepository.class); specialists=mock(SpecialistRepository.class); slots=mock(AvailableSlotService.class);
        TenantAccessGuard tenants=mock(TenantAccessGuard.class);
        when(tenants.current()).thenReturn(new AuthenticatedOrganizationContext(null,null,1L,Set.of("PLATFORM_ADMIN"),Set.of()));
        Organization organization=mock(Organization.class); when(organization.getId()).thenReturn(1L); when(organization.getTimezone()).thenReturn("UTC");
        Branch branch=mock(Branch.class); when(branch.getId()).thenReturn(2L); when(branch.getTimezone()).thenReturn("America/El_Salvador");
        CatalogService catalog=mock(CatalogService.class); when(catalog.getId()).thenReturn(3L);
        Specialist specialist=mock(Specialist.class); when(specialist.getId()).thenReturn(4L);
        Customer customer=mock(Customer.class); when(customer.getId()).thenReturn(5L); when(customer.getActive()).thenReturn(true);
        OffsetDateTime start=OffsetDateTime.parse("2026-08-17T09:00:00-06:00");
        booking=new BookingSlot(organization,branch,catalog,specialist,start,start.plusMinutes(30),0,0);
        appointment=new Appointment(organization,branch,customer,catalog,specialist,start,start.plusMinutes(30),null);
        when(specialists.findForBooking(1L,4L)).thenReturn(Optional.of(specialist));
        when(customers.findByIdAndOrganization_IdAndDeletedAtIsNull(5L,1L)).thenReturn(Optional.of(customer));
        when(slots.requireAvailable(any(),any(),any(),any(),any(),any())).thenReturn(booking);
        notificationOutbox=mock(AppointmentNotificationOutboxService.class);
        service=new AppointmentApplicationService(appointments,history,customers,specialists,mock(AppUserRepository.class),
                slots,new AppointmentStatusPolicy(),new AppointmentMapper(),tenants,mock(EntityManager.class),
                Clock.fixed(Instant.parse("2026-08-18T15:00:00Z"), ZoneOffset.UTC), notificationOutbox);
    }

    @Test void createsOnlyAfterAvailabilityValidationAndWritesHistory() {
        service.create(1L,new AppointmentCreateRequest(5L,2L,3L,4L,booking.startsAt(),null,null));
        verify(slots).requireAvailable(1L,2L,3L,4L,booking.startsAt(),null);
        verify(appointments).saveAndFlush(any(Appointment.class)); verify(history).save(any(AppointmentStatusHistory.class));
        verify(notificationOutbox).enqueue(any(Appointment.class),
                org.mockito.Mockito.eq(AppointmentNotificationEventType.APPOINTMENT_CREATED), any());
    }
    @Test void invalidSlotBecomesAppointmentConflict() {
        when(slots.requireAvailable(any(),any(),any(),any(),any(),any())).thenThrow(new SlotNotAvailableException());
        assertThatThrownBy(() -> service.create(1L,new AppointmentCreateRequest(5L,2L,3L,4L,booking.startsAt(),null,null)))
                .isInstanceOf(AppointmentConflictException.class);
    }
    @Test void invalidBranchServiceIsNeverInserted() { invalidRelationshipIsNeverInserted(); }
    @Test void invalidSpecialistServiceIsNeverInserted() { invalidRelationshipIsNeverInserted(); }
    @Test void reschedulesUsingSameAvailabilityAlgorithmAndWritesHistory() {
        when(appointments.findForUpdate(1L,8L)).thenReturn(Optional.of(appointment));
        service.reschedule(1L,8L,new AppointmentRescheduleRequest(booking.startsAt().plusHours(1),4L,"requested"));
        verify(history).save(any(AppointmentStatusHistory.class));
        verify(notificationOutbox).enqueue(appointment, AppointmentNotificationEventType.APPOINTMENT_RESCHEDULED,
                OffsetDateTime.parse("2026-08-18T15:00:00Z"));
    }
    @Test void cancelsAndWritesStatusHistory() {
        when(appointments.findForUpdate(1L,8L)).thenReturn(Optional.of(appointment));
        service.cancel(1L,8L,new AppointmentCancelRequest("customer request"));
        verify(history).save(any(AppointmentStatusHistory.class));
        verify(notificationOutbox).enqueue(appointment, AppointmentNotificationEventType.APPOINTMENT_CANCELLED,
                OffsetDateTime.parse("2026-08-18T15:00:00Z"));
    }
    @Test void rejectsInvalidStatusTransition() {
        appointment.cancel(null,OffsetDateTime.now(),null);
        when(appointments.findForUpdate(1L,8L)).thenReturn(Optional.of(appointment));
        assertThatThrownBy(() -> service.reschedule(1L,8L,new AppointmentRescheduleRequest(booking.startsAt(),null,null)))
                .isInstanceOf(InvalidAppointmentTransitionException.class);
    }
    @Test void returnsImmutableHistoryProjection() {
        when(appointments.findByIdAndOrganization_IdAndDeletedAtIsNull(8L,1L)).thenReturn(Optional.of(appointment));
        when(history.findAllByAppointment_IdAndAppointment_Organization_IdOrderByChangedAtAsc(8L,1L)).thenReturn(List.of());
        service.history(1L,8L); verify(history).findAllByAppointment_IdAndAppointment_Organization_IdOrderByChangedAtAsc(8L,1L);
    }
    @Test void executesCompleteOperationalLifecycleAndWritesEveryHistoryEntry() {
        when(appointments.findForUpdate(1L,8L)).thenReturn(Optional.of(appointment));
        service.confirm(1L,8L); service.checkIn(1L,8L); service.start(1L,8L); service.complete(1L,8L);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.getConfirmedAt()).isNotNull();
        assertThat(appointment.getCheckedInAt()).isNotNull();
        assertThat(appointment.getStartedServiceAt()).isNotNull();
        assertThat(appointment.getCompletedAt()).isNotNull();
        verify(history, org.mockito.Mockito.times(4)).save(any(AppointmentStatusHistory.class));
    }
    @Test void marksPastAppointmentAsNoShowAndWritesHistory() {
        when(appointments.findForUpdate(1L,8L)).thenReturn(Optional.of(appointment));
        service.markNoShow(1L,8L);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        verify(history).save(any(AppointmentStatusHistory.class));
    }
    @Test void operationalTransitionsDoNotProduceNotificationEvents() {
        when(appointments.findForUpdate(1L,8L)).thenReturn(Optional.of(appointment));
        service.confirm(1L,8L);
        org.mockito.Mockito.verifyNoInteractions(notificationOutbox);
    }
    private void invalidRelationshipIsNeverInserted() {
        when(slots.requireAvailable(any(),any(),any(),any(),any(),any())).thenThrow(new SlotNotAvailableException());
        assertThatThrownBy(() -> service.create(1L,new AppointmentCreateRequest(5L,2L,3L,4L,booking.startsAt(),null,null)))
                .isInstanceOf(AppointmentConflictException.class);
        org.mockito.Mockito.verifyNoInteractions(appointments);
    }
}
