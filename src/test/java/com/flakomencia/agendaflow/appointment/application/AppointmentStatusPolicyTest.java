package com.flakomencia.agendaflow.appointment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.flakomencia.agendaflow.appointment.domain.AppointmentStatus;

class AppointmentStatusPolicyTest {
    private final AppointmentStatusPolicy policy = new AppointmentStatusPolicy();

    @Test void exposesTheSupportedOperationalTransitions() {
        assertThat(policy.canConfirm(AppointmentStatus.PENDING)).isTrue();
        assertThat(policy.canCheckIn(AppointmentStatus.CONFIRMED)).isTrue();
        assertThat(policy.canStart(AppointmentStatus.CHECKED_IN)).isTrue();
        assertThat(policy.canComplete(AppointmentStatus.IN_PROGRESS)).isTrue();
        assertThat(policy.canMarkNoShow(AppointmentStatus.PENDING)).isTrue();
        assertThat(policy.canMarkNoShow(AppointmentStatus.CONFIRMED)).isTrue();
        assertThat(policy.canMarkNoShow(AppointmentStatus.CHECKED_IN)).isTrue();
    }

    @Test void rejectsInvalidConfirmation() {
        assertThatThrownBy(() -> policy.requireConfirmable(AppointmentStatus.CONFIRMED))
                .isInstanceOf(InvalidAppointmentTransitionException.class);
    }

    @Test void rejectsFutureNoShowEvenWhenStatusIsEligible() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-18T15:00:00Z");
        assertThatThrownBy(() -> policy.requireNoShow(AppointmentStatus.CONFIRMED, now.plusMinutes(1), now))
                .isInstanceOf(AppointmentNotDueException.class)
                .hasMessageContaining("cannot be marked as no-show");
    }

    @Test void acceptsNoShowAtTheScheduledInstant() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-18T15:00:00Z");
        policy.requireNoShow(AppointmentStatus.CONFIRMED, now, now);
    }
}
