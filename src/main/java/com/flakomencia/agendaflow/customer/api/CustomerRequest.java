package com.flakomencia.agendaflow.customer.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.flakomencia.agendaflow.customer.domain.PreferredContactMethod;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @Size(max = 40) String customerNumber,
        @NotBlank @Size(max = 80) String firstName,
        @Size(max = 80) String middleName,
        @NotBlank @Size(max = 80) String lastName,
        @Size(max = 80) String secondLastName,
        @Email @Size(max = 254) String email,
        @Size(max = 30) String phone,
        @Size(max = 30) String alternatePhone,
        @Past LocalDate dateOfBirth,
        @Size(max = 10) String preferredLanguage,
        PreferredContactMethod preferredContactMethod,
        @Size(max = 150) String addressLine1,
        @Size(max = 150) String addressLine2,
        @Size(max = 100) String city,
        @Size(max = 10) String stateCode,
        @Size(max = 15) String postalCode,
        @Size(max = 2) String countryCode,
        @Size(max = 160) String emergencyContactName,
        @Size(max = 30) String emergencyContactPhone,
        @Size(max = 80) String emergencyContactRelationship,
        Boolean emailConsent,
        Boolean smsConsent,
        Boolean marketingConsent,
        OffsetDateTime termsAcceptedAt,
        OffsetDateTime privacyPolicyAcceptedAt,
        String internalNotes,
        Boolean active) {
}
