package com.flakomencia.agendaflow.appointment.application;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.flakomencia.agendaflow.appointment.domain.AppointmentStatus;

@Component
public class AppointmentStatusPolicy {
    private static final Set<AppointmentStatus> RESCHEDULABLE =
            EnumSet.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
    private static final Set<AppointmentStatus> CANCELLABLE =
            EnumSet.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
    private static final Set<AppointmentStatus> NO_SHOW_ELIGIBLE =
            EnumSet.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN);

    public boolean canConfirm(AppointmentStatus status) { return status == AppointmentStatus.PENDING; }
    public boolean canCheckIn(AppointmentStatus status) { return status == AppointmentStatus.CONFIRMED; }
    public boolean canStart(AppointmentStatus status) { return status == AppointmentStatus.CHECKED_IN; }
    public boolean canComplete(AppointmentStatus status) { return status == AppointmentStatus.IN_PROGRESS; }
    public boolean canMarkNoShow(AppointmentStatus status) { return NO_SHOW_ELIGIBLE.contains(status); }
    public boolean canCancel(AppointmentStatus status) { return CANCELLABLE.contains(status); }
    public boolean canReschedule(AppointmentStatus status) { return RESCHEDULABLE.contains(status); }

    public void requireConfirmable(AppointmentStatus status) {
        require(canConfirm(status), status, "confirmed");
    }
    public void requireCheckIn(AppointmentStatus status) {
        require(canCheckIn(status), status, "checked in");
    }
    public void requireStartable(AppointmentStatus status) {
        require(canStart(status), status, "started");
    }
    public void requireCompletable(AppointmentStatus status) {
        require(canComplete(status), status, "completed");
    }
    public void requireNoShow(AppointmentStatus status, OffsetDateTime startsAt, OffsetDateTime now) {
        require(canMarkNoShow(status), status, "marked as no-show");
        if (now.isBefore(startsAt)) throw new AppointmentNotDueException(startsAt);
    }

    public void requireReschedulable(AppointmentStatus status) {
        require(canReschedule(status), status, "rescheduled");
    }
    public void requireCancellable(AppointmentStatus status) {
        if (status == AppointmentStatus.CANCELLED) throw new AppointmentAlreadyCancelledException();
        require(canCancel(status), status, "cancelled");
    }

    private void require(boolean allowed, AppointmentStatus status, String action) {
        if (!allowed) throw new InvalidAppointmentTransitionException(status, action);
    }
}
