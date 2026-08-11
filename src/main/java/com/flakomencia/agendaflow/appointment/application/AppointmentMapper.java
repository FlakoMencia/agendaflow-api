package com.flakomencia.agendaflow.appointment.application;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.flakomencia.agendaflow.appointment.api.AppointmentResponse;
import com.flakomencia.agendaflow.appointment.api.AppointmentStatusHistoryResponse;
import com.flakomencia.agendaflow.appointment.domain.Appointment;
import com.flakomencia.agendaflow.appointment.domain.AppointmentStatusHistory;

@Component
public class AppointmentMapper {
    public AppointmentResponse toResponse(Appointment a) {
        ZoneId zone = appointmentZone(a);
        return new AppointmentResponse(a.getId(), a.getOrganization().getId(), a.getBranch().getId(),
                a.getCustomer().getId(), a.getService().getId(), a.getSpecialist().getId(), inZone(a.getStartsAt(), zone),
                inZone(a.getEndsAt(), zone), a.getStatus(), a.getOrigin(), a.getCustomerNotes(), a.getInternalNotes(),
                a.getCancellationReason(), a.getCancelledAt(), id(a.getCancelledBy()), id(a.getCreatedBy()),
                id(a.getUpdatedBy()), a.getCreatedAt(), a.getUpdatedAt());
    }
    private ZoneId appointmentZone(Appointment appointment) {
        String branchZone = appointment.getBranch().getTimezone();
        return ZoneId.of(branchZone == null || branchZone.isBlank()
                ? appointment.getOrganization().getTimezone() : branchZone);
    }
    private OffsetDateTime inZone(OffsetDateTime value, ZoneId zone) {
        return value == null ? null : value.atZoneSameInstant(zone).toOffsetDateTime();
    }
    public AppointmentStatusHistoryResponse toResponse(AppointmentStatusHistory h) {
        return new AppointmentStatusHistoryResponse(h.getId(), h.getPreviousStatus(), h.getNewStatus(),
                id(h.getChangedBy()), h.getChangeReason(), h.getChangedAt());
    }
    private Long id(com.flakomencia.agendaflow.identity.domain.AppUser user) { return user == null ? null : user.getId(); }
}
