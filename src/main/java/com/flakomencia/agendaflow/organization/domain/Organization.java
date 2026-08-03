package com.flakomencia.agendaflow.organization.domain;

import java.time.OffsetDateTime;

import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@DynamicInsert
@Table(name = "organizations", schema = "agendaflow")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legal_name", nullable = false, length = 180)
    private String legalName;

    @Column(name = "trade_name", length = 180)
    private String tradeName;

    @Column(name = "tax_identifier", length = 50)
    private String taxIdentifier;

    @Column(name = "organization_type", length = 50)
    private String organizationType;

    @Column(length = 254)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String website;

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

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(nullable = false, length = 60)
    private String timezone;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(name = "allows_public_booking", nullable = false)
    private Boolean allowsPublicBooking;

    @Column(name = "allows_guest_booking", nullable = false)
    private Boolean allowsGuestBooking;

    @Column(name = "requires_appointment_confirmation", nullable = false)
    private Boolean requiresAppointmentConfirmation;

    @Column(name = "minimum_booking_notice_minutes", nullable = false)
    private Integer minimumBookingNoticeMinutes;

    @Column(name = "maximum_booking_days_ahead", nullable = false)
    private Integer maximumBookingDaysAhead;

    @Column(name = "cancellation_notice_minutes", nullable = false)
    private Integer cancellationNoticeMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrganizationStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at", insertable = false)
    private OffsetDateTime deletedAt;

    protected Organization() {
    }

    public Organization(String legalName) {
        this.legalName = legalName;
    }

    public Long getId() {
        return id;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getTradeName() {
        return tradeName;
    }

    public void setTradeName(String tradeName) {
        this.tradeName = tradeName;
    }

    public String getTaxIdentifier() {
        return taxIdentifier;
    }

    public void setTaxIdentifier(String taxIdentifier) {
        this.taxIdentifier = taxIdentifier;
    }

    public String getOrganizationType() {
        return organizationType;
    }

    public void setOrganizationType(String organizationType) {
        this.organizationType = organizationType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStateCode() {
        return stateCode;
    }

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public Boolean getAllowsPublicBooking() {
        return allowsPublicBooking;
    }

    public void setAllowsPublicBooking(Boolean allowsPublicBooking) {
        this.allowsPublicBooking = allowsPublicBooking;
    }

    public Boolean getAllowsGuestBooking() {
        return allowsGuestBooking;
    }

    public void setAllowsGuestBooking(Boolean allowsGuestBooking) {
        this.allowsGuestBooking = allowsGuestBooking;
    }

    public Boolean getRequiresAppointmentConfirmation() {
        return requiresAppointmentConfirmation;
    }

    public void setRequiresAppointmentConfirmation(Boolean requiresAppointmentConfirmation) {
        this.requiresAppointmentConfirmation = requiresAppointmentConfirmation;
    }

    public Integer getMinimumBookingNoticeMinutes() {
        return minimumBookingNoticeMinutes;
    }

    public void setMinimumBookingNoticeMinutes(Integer minimumBookingNoticeMinutes) {
        this.minimumBookingNoticeMinutes = minimumBookingNoticeMinutes;
    }

    public Integer getMaximumBookingDaysAhead() {
        return maximumBookingDaysAhead;
    }

    public void setMaximumBookingDaysAhead(Integer maximumBookingDaysAhead) {
        this.maximumBookingDaysAhead = maximumBookingDaysAhead;
    }

    public Integer getCancellationNoticeMinutes() {
        return cancellationNoticeMinutes;
    }

    public void setCancellationNoticeMinutes(Integer cancellationNoticeMinutes) {
        this.cancellationNoticeMinutes = cancellationNoticeMinutes;
    }

    public OrganizationStatus getStatus() {
        return status;
    }

    public void setStatus(OrganizationStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        Organization that = (Organization) other;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
