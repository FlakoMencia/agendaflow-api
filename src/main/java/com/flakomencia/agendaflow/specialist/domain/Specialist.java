package com.flakomencia.agendaflow.specialist.domain;

import java.time.OffsetDateTime;

import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicInsert;

import com.flakomencia.agendaflow.identity.domain.AppUser;
import com.flakomencia.agendaflow.organization.domain.Organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@DynamicInsert
@Table(name = "specialists", schema = "agendaflow")
public class Specialist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "professional_name", nullable = false, length = 160)
    private String professionalName;

    @Column(name = "specialty_name", length = 150)
    private String specialtyName;

    @Column(columnDefinition = "text")
    private String biography;

    @Column(name = "license_number", length = 80)
    private String licenseNumber;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(length = 30)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(name = "simultaneous_capacity", nullable = false)
    private Integer simultaneousCapacity;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at", insertable = false)
    private OffsetDateTime deletedAt;

    protected Specialist() {
    }

    public Specialist(Organization organization, String professionalName) {
        this.organization = organization;
        this.professionalName = professionalName;
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public String getProfessionalName() { return professionalName; }
    public void setProfessionalName(String professionalName) { this.professionalName = professionalName; }
    public String getSpecialtyName() { return specialtyName; }
    public void setSpecialtyName(String specialtyName) { this.specialtyName = specialtyName; }
    public String getBiography() { return biography; }
    public void setBiography(String biography) { this.biography = biography; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getSimultaneousCapacity() { return simultaneousCapacity; }
    public void setSimultaneousCapacity(Integer simultaneousCapacity) { this.simultaneousCapacity = simultaneousCapacity; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false;
        Specialist that = (Specialist) other;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
