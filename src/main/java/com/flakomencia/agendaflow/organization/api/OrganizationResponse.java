package com.flakomencia.agendaflow.organization.api;

import java.time.OffsetDateTime;

import com.flakomencia.agendaflow.organization.domain.OrganizationStatus;

public record OrganizationResponse(
        Long id,
        String legalName,
        String tradeName,
        String taxIdentifier,
        String organizationType,
        String email,
        String phone,
        String website,
        String addressLine1,
        String addressLine2,
        String city,
        String stateCode,
        String postalCode,
        String countryCode,
        String logoUrl,
        String timezone,
        String currencyCode,
        String languageCode,
        Boolean allowsPublicBooking,
        Boolean allowsGuestBooking,
        Boolean requiresAppointmentConfirmation,
        Integer minimumBookingNoticeMinutes,
        Integer maximumBookingDaysAhead,
        Integer cancellationNoticeMinutes,
        OrganizationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
