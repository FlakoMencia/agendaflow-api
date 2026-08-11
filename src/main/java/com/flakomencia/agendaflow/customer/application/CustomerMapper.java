package com.flakomencia.agendaflow.customer.application;

import org.springframework.stereotype.Component;

import com.flakomencia.agendaflow.customer.api.CustomerRequest;
import com.flakomencia.agendaflow.customer.api.CustomerResponse;
import com.flakomencia.agendaflow.customer.domain.Customer;
import com.flakomencia.agendaflow.organization.domain.Organization;

@Component
public class CustomerMapper {
    public Customer toEntity(Organization organization, CustomerRequest request) {
        Customer customer = new Customer(organization, request.firstName().trim(), request.lastName().trim());
        update(customer, request);
        return customer;
    }

    public void update(Customer customer, CustomerRequest request) {
        customer.setCustomerNumber(optional(request.customerNumber()));
        customer.setFirstName(request.firstName().trim());
        customer.setMiddleName(optional(request.middleName()));
        customer.setLastName(request.lastName().trim());
        customer.setSecondLastName(optional(request.secondLastName()));
        customer.setEmail(optionalLower(request.email()));
        customer.setPhone(optional(request.phone()));
        customer.setAlternatePhone(optional(request.alternatePhone()));
        customer.setDateOfBirth(request.dateOfBirth());
        if (request.preferredLanguage() != null) customer.setPreferredLanguage(optional(request.preferredLanguage()));
        customer.setPreferredContactMethod(request.preferredContactMethod());
        customer.setAddressLine1(optional(request.addressLine1()));
        customer.setAddressLine2(optional(request.addressLine2()));
        customer.setCity(optional(request.city()));
        customer.setStateCode(optional(request.stateCode()));
        customer.setPostalCode(optional(request.postalCode()));
        if (request.countryCode() != null) customer.setCountryCode(optional(request.countryCode()));
        customer.setEmergencyContactName(optional(request.emergencyContactName()));
        customer.setEmergencyContactPhone(optional(request.emergencyContactPhone()));
        customer.setEmergencyContactRelationship(optional(request.emergencyContactRelationship()));
        if (request.emailConsent() != null) customer.setEmailConsent(request.emailConsent());
        if (request.smsConsent() != null) customer.setSmsConsent(request.smsConsent());
        if (request.marketingConsent() != null) customer.setMarketingConsent(request.marketingConsent());
        customer.setTermsAcceptedAt(request.termsAcceptedAt());
        customer.setPrivacyPolicyAcceptedAt(request.privacyPolicyAcceptedAt());
        customer.setInternalNotes(optional(request.internalNotes()));
        if (request.active() != null) customer.setActive(request.active());
    }

    public CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(c.getId(), c.getOrganization().getId(), c.getCustomerNumber(),
                c.getFirstName(), c.getMiddleName(), c.getLastName(), c.getSecondLastName(), c.getEmail(),
                c.getPhone(), c.getAlternatePhone(), c.getDateOfBirth(), c.getPreferredLanguage(),
                c.getPreferredContactMethod(), c.getAddressLine1(), c.getAddressLine2(), c.getCity(),
                c.getStateCode(), c.getPostalCode(), c.getCountryCode(), c.getEmergencyContactName(),
                c.getEmergencyContactPhone(), c.getEmergencyContactRelationship(), c.getEmailConsent(),
                c.getSmsConsent(), c.getMarketingConsent(), c.getTermsAcceptedAt(),
                c.getPrivacyPolicyAcceptedAt(), c.getInternalNotes(), c.getActive(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private String optional(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String optionalLower(String value) {
        String normalized = optional(value);
        return normalized == null ? null : normalized.toLowerCase(java.util.Locale.ROOT);
    }
}
