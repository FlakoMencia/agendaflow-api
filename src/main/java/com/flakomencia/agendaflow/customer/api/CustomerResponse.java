package com.flakomencia.agendaflow.customer.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.flakomencia.agendaflow.customer.domain.PreferredContactMethod;

public record CustomerResponse(
        Long id, Long organizationId, String customerNumber,
        String firstName, String middleName, String lastName, String secondLastName,
        String email, String phone, String alternatePhone, LocalDate dateOfBirth,
        String preferredLanguage, PreferredContactMethod preferredContactMethod,
        String addressLine1, String addressLine2, String city, String stateCode,
        String postalCode, String countryCode, String emergencyContactName,
        String emergencyContactPhone, String emergencyContactRelationship,
        Boolean emailConsent, Boolean smsConsent, Boolean marketingConsent,
        OffsetDateTime termsAcceptedAt, OffsetDateTime privacyPolicyAcceptedAt,
        String internalNotes, Boolean active, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
