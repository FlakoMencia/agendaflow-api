package com.flakomencia.agendaflow.customer.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicInsert;

import com.flakomencia.agendaflow.organization.domain.Organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@DynamicInsert
@Table(name = "customers", schema = "agendaflow")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @Column(name = "customer_number", length = 40)
    private String customerNumber;
    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;
    @Column(name = "middle_name", length = 80)
    private String middleName;
    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;
    @Column(name = "second_last_name", length = 80)
    private String secondLastName;
    @Column(length = 254)
    private String email;
    @Column(length = 30)
    private String phone;
    @Column(name = "alternate_phone", length = 30)
    private String alternatePhone;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage;
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_contact_method", length = 20)
    private PreferredContactMethod preferredContactMethod;
    @Column(name = "address_line_1", length = 150)
    private String addressLine1;
    @Column(name = "address_line_2", length = 150)
    private String addressLine2;
    @Column(length = 100)
    private String city;
    @Column(name = "state_code", length = 10)
    private String stateCode;
    @Column(name = "postal_code", length = 15)
    private String postalCode;
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;
    @Column(name = "emergency_contact_name", length = 160)
    private String emergencyContactName;
    @Column(name = "emergency_contact_phone", length = 30)
    private String emergencyContactPhone;
    @Column(name = "emergency_contact_relationship", length = 80)
    private String emergencyContactRelationship;
    @Column(name = "email_consent", nullable = false)
    private Boolean emailConsent;
    @Column(name = "sms_consent", nullable = false)
    private Boolean smsConsent;
    @Column(name = "marketing_consent", nullable = false)
    private Boolean marketingConsent;
    @Column(name = "terms_accepted_at")
    private OffsetDateTime termsAcceptedAt;
    @Column(name = "privacy_policy_accepted_at")
    private OffsetDateTime privacyPolicyAcceptedAt;
    @Column(name = "internal_notes")
    private String internalNotes;
    @Column(name = "is_active", nullable = false)
    private Boolean active;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
    @Column(name = "deleted_at", insertable = false)
    private OffsetDateTime deletedAt;

    protected Customer() {}

    public Customer(Organization organization, String firstName, String lastName) {
        this.organization = organization;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public String getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(String value) { customerNumber = value; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String value) { firstName = value; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String value) { middleName = value; }
    public String getLastName() { return lastName; }
    public void setLastName(String value) { lastName = value; }
    public String getSecondLastName() { return secondLastName; }
    public void setSecondLastName(String value) { secondLastName = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
    public String getAlternatePhone() { return alternatePhone; }
    public void setAlternatePhone(String value) { alternatePhone = value; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate value) { dateOfBirth = value; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String value) { preferredLanguage = value; }
    public PreferredContactMethod getPreferredContactMethod() { return preferredContactMethod; }
    public void setPreferredContactMethod(PreferredContactMethod value) { preferredContactMethod = value; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String value) { addressLine1 = value; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String value) { addressLine2 = value; }
    public String getCity() { return city; }
    public void setCity(String value) { city = value; }
    public String getStateCode() { return stateCode; }
    public void setStateCode(String value) { stateCode = value; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String value) { postalCode = value; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String value) { countryCode = value; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String value) { emergencyContactName = value; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String value) { emergencyContactPhone = value; }
    public String getEmergencyContactRelationship() { return emergencyContactRelationship; }
    public void setEmergencyContactRelationship(String value) { emergencyContactRelationship = value; }
    public Boolean getEmailConsent() { return emailConsent; }
    public void setEmailConsent(Boolean value) { emailConsent = value; }
    public Boolean getSmsConsent() { return smsConsent; }
    public void setSmsConsent(Boolean value) { smsConsent = value; }
    public Boolean getMarketingConsent() { return marketingConsent; }
    public void setMarketingConsent(Boolean value) { marketingConsent = value; }
    public OffsetDateTime getTermsAcceptedAt() { return termsAcceptedAt; }
    public void setTermsAcceptedAt(OffsetDateTime value) { termsAcceptedAt = value; }
    public OffsetDateTime getPrivacyPolicyAcceptedAt() { return privacyPolicyAcceptedAt; }
    public void setPrivacyPolicyAcceptedAt(OffsetDateTime value) { privacyPolicyAcceptedAt = value; }
    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String value) { internalNotes = value; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean value) { active = value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false;
        Customer that = (Customer) other;
        return id != null && id.equals(that.id);
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
