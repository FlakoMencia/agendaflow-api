package com.flakomencia.agendaflow.organization.application;

import org.springframework.stereotype.Component;

import com.flakomencia.agendaflow.organization.api.OrganizationCreateRequest;
import com.flakomencia.agendaflow.organization.api.OrganizationResponse;
import com.flakomencia.agendaflow.organization.api.OrganizationUpdateRequest;
import com.flakomencia.agendaflow.organization.domain.Organization;

@Component
public class OrganizationMapper {

    public Organization toEntity(OrganizationCreateRequest request) {
        Organization organization = new Organization(requiredText(request.legalName()));
        organization.setTradeName(optionalText(request.tradeName()));
        organization.setTaxIdentifier(optionalText(request.taxIdentifier()));
        organization.setOrganizationType(optionalText(request.organizationType()));
        organization.setEmail(optionalText(request.email()));
        organization.setPhone(optionalText(request.phone()));
        organization.setWebsite(optionalText(request.website()));
        organization.setAddressLine1(optionalText(request.addressLine1()));
        organization.setAddressLine2(optionalText(request.addressLine2()));
        organization.setCity(optionalText(request.city()));
        organization.setStateCode(optionalText(request.stateCode()));
        organization.setPostalCode(optionalText(request.postalCode()));
        organization.setCountryCode(optionalText(request.countryCode()));
        organization.setLogoUrl(optionalText(request.logoUrl()));
        organization.setTimezone(optionalText(request.timezone()));
        organization.setCurrencyCode(optionalText(request.currencyCode()));
        organization.setLanguageCode(optionalText(request.languageCode()));
        organization.setAllowsPublicBooking(request.allowsPublicBooking());
        organization.setAllowsGuestBooking(request.allowsGuestBooking());
        organization.setRequiresAppointmentConfirmation(request.requiresAppointmentConfirmation());
        organization.setMinimumBookingNoticeMinutes(request.minimumBookingNoticeMinutes());
        organization.setMaximumBookingDaysAhead(request.maximumBookingDaysAhead());
        organization.setCancellationNoticeMinutes(request.cancellationNoticeMinutes());
        organization.setStatus(request.status());
        return organization;
    }

    public void update(Organization organization, OrganizationUpdateRequest request) {
        organization.setLegalName(requiredText(request.legalName()));
        organization.setTradeName(optionalText(request.tradeName()));
        organization.setTaxIdentifier(optionalText(request.taxIdentifier()));
        organization.setOrganizationType(optionalText(request.organizationType()));
        organization.setEmail(optionalText(request.email()));
        organization.setPhone(optionalText(request.phone()));
        organization.setWebsite(optionalText(request.website()));
        organization.setAddressLine1(optionalText(request.addressLine1()));
        organization.setAddressLine2(optionalText(request.addressLine2()));
        organization.setCity(optionalText(request.city()));
        organization.setStateCode(optionalText(request.stateCode()));
        organization.setPostalCode(optionalText(request.postalCode()));
        if (request.countryCode() != null) {
            organization.setCountryCode(optionalText(request.countryCode()));
        }
        organization.setLogoUrl(optionalText(request.logoUrl()));
        if (request.timezone() != null) {
            organization.setTimezone(optionalText(request.timezone()));
        }
        if (request.currencyCode() != null) {
            organization.setCurrencyCode(optionalText(request.currencyCode()));
        }
        if (request.languageCode() != null) {
            organization.setLanguageCode(optionalText(request.languageCode()));
        }
        if (request.allowsPublicBooking() != null) {
            organization.setAllowsPublicBooking(request.allowsPublicBooking());
        }
        if (request.allowsGuestBooking() != null) {
            organization.setAllowsGuestBooking(request.allowsGuestBooking());
        }
        if (request.requiresAppointmentConfirmation() != null) {
            organization.setRequiresAppointmentConfirmation(request.requiresAppointmentConfirmation());
        }
        if (request.minimumBookingNoticeMinutes() != null) {
            organization.setMinimumBookingNoticeMinutes(request.minimumBookingNoticeMinutes());
        }
        if (request.maximumBookingDaysAhead() != null) {
            organization.setMaximumBookingDaysAhead(request.maximumBookingDaysAhead());
        }
        if (request.cancellationNoticeMinutes() != null) {
            organization.setCancellationNoticeMinutes(request.cancellationNoticeMinutes());
        }
        if (request.status() != null) {
            organization.setStatus(request.status());
        }
    }

    public OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getLegalName(),
                organization.getTradeName(),
                organization.getTaxIdentifier(),
                organization.getOrganizationType(),
                organization.getEmail(),
                organization.getPhone(),
                organization.getWebsite(),
                organization.getAddressLine1(),
                organization.getAddressLine2(),
                organization.getCity(),
                organization.getStateCode(),
                organization.getPostalCode(),
                organization.getCountryCode(),
                organization.getLogoUrl(),
                organization.getTimezone(),
                organization.getCurrencyCode(),
                organization.getLanguageCode(),
                organization.getAllowsPublicBooking(),
                organization.getAllowsGuestBooking(),
                organization.getRequiresAppointmentConfirmation(),
                organization.getMinimumBookingNoticeMinutes(),
                organization.getMaximumBookingDaysAhead(),
                organization.getCancellationNoticeMinutes(),
                organization.getStatus(),
                organization.getCreatedAt(),
                organization.getUpdatedAt());
    }

    private String requiredText(String value) {
        return value.trim();
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
