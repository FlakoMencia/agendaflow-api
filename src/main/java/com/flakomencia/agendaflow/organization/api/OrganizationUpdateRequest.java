package com.flakomencia.agendaflow.organization.api;

import com.flakomencia.agendaflow.organization.domain.OrganizationStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record OrganizationUpdateRequest(
        @NotBlank @Size(max = 180) String legalName,
        @Size(max = 180) String tradeName,
        @Size(max = 50) String taxIdentifier,
        @Size(max = 50) String organizationType,
        @Email @Size(max = 254) String email,
        @Size(max = 30) String phone,
        @Size(max = 255) String website,
        @Size(max = 150) String addressLine1,
        @Size(max = 150) String addressLine2,
        @Size(max = 100) String city,
        @Size(max = 10) String stateCode,
        @Size(max = 15) String postalCode,
        @Size(min = 2, max = 2) String countryCode,
        @Size(max = 500) String logoUrl,
        @Size(max = 60) String timezone,
        @Size(min = 3, max = 3) String currencyCode,
        @Size(max = 10) String languageCode,
        Boolean allowsPublicBooking,
        Boolean allowsGuestBooking,
        Boolean requiresAppointmentConfirmation,
        @PositiveOrZero Integer minimumBookingNoticeMinutes,
        @PositiveOrZero Integer maximumBookingDaysAhead,
        @PositiveOrZero Integer cancellationNoticeMinutes,
        OrganizationStatus status) {
}
