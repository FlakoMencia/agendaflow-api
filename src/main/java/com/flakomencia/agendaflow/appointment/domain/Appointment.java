package com.flakomencia.agendaflow.appointment.domain;

import java.time.OffsetDateTime;

import org.hibernate.Hibernate;

import com.flakomencia.agendaflow.branch.domain.Branch;
import com.flakomencia.agendaflow.customer.domain.Customer;
import com.flakomencia.agendaflow.identity.domain.AppUser;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.servicecatalog.domain.CatalogService;
import com.flakomencia.agendaflow.specialist.domain.Specialist;

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
@Table(name = "appointments", schema = "agendaflow")
public class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private CatalogService service;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialist_id", nullable = false)
    private Specialist specialist;
    @Column(name = "resource_id", insertable = false, updatable = false)
    private Long resourceId;
    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;
    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private AppointmentStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private AppointmentOrigin origin;
    @Column(name = "customer_notes") private String customerNotes;
    @Column(name = "internal_notes") private String internalNotes;
    @Column(name = "cancellation_reason") private String cancellationReason;
    @Column(name = "cancelled_at") private OffsetDateTime cancelledAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cancelled_by") private AppUser cancelledBy;
    @Column(name = "confirmed_at") private OffsetDateTime confirmedAt;
    @Column(name = "checked_in_at") private OffsetDateTime checkedInAt;
    @Column(name = "started_service_at") private OffsetDateTime startedServiceAt;
    @Column(name = "completed_at") private OffsetDateTime completedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by", updatable = false) private AppUser createdBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by") private AppUser updatedBy;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false) private OffsetDateTime updatedAt;
    @Column(name = "deleted_at", insertable = false) private OffsetDateTime deletedAt;

    protected Appointment() {}
    public Appointment(Organization organization, Branch branch, Customer customer, CatalogService service,
            Specialist specialist, OffsetDateTime startsAt, OffsetDateTime endsAt, AppUser createdBy) {
        this.organization = organization; this.branch = branch; this.customer = customer; this.service = service;
        this.specialist = specialist; this.startsAt = startsAt; this.endsAt = endsAt; this.createdBy = createdBy;
        this.updatedBy = createdBy; this.status = AppointmentStatus.PENDING; this.origin = AppointmentOrigin.RECEPTION;
    }
    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public Branch getBranch() { return branch; }
    public Customer getCustomer() { return customer; }
    public CatalogService getService() { return service; }
    public Specialist getSpecialist() { return specialist; }
    public Long getResourceId() { return resourceId; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public AppointmentStatus getStatus() { return status; }
    public AppointmentOrigin getOrigin() { return origin; }
    public String getCustomerNotes() { return customerNotes; }
    public String getInternalNotes() { return internalNotes; }
    public String getCancellationReason() { return cancellationReason; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public AppUser getCancelledBy() { return cancelledBy; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public OffsetDateTime getCheckedInAt() { return checkedInAt; }
    public OffsetDateTime getStartedServiceAt() { return startedServiceAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public AppUser getCreatedBy() { return createdBy; }
    public AppUser getUpdatedBy() { return updatedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void reschedule(Specialist specialist, OffsetDateTime start, OffsetDateTime end, AppUser actor) {
        this.specialist = specialist; this.startsAt = start; this.endsAt = end; this.updatedBy = actor;
    }
    public void cancel(String reason, OffsetDateTime at, AppUser actor) {
        status = AppointmentStatus.CANCELLED; cancellationReason = reason; cancelledAt = at; cancelledBy = actor; updatedBy = actor;
    }
    public void confirm(OffsetDateTime at, AppUser actor) {
        status = AppointmentStatus.CONFIRMED; confirmedAt = at; updatedBy = actor;
    }
    public void checkIn(OffsetDateTime at, AppUser actor) {
        status = AppointmentStatus.CHECKED_IN; checkedInAt = at; updatedBy = actor;
    }
    public void start(OffsetDateTime at, AppUser actor) {
        status = AppointmentStatus.IN_PROGRESS; startedServiceAt = at; updatedBy = actor;
    }
    public void complete(OffsetDateTime at, AppUser actor) {
        status = AppointmentStatus.COMPLETED; completedAt = at; updatedBy = actor;
    }
    public void markNoShow(AppUser actor) {
        status = AppointmentStatus.NO_SHOW; updatedBy = actor;
    }
    public void setCustomerNotes(String value) { customerNotes = value; }
    public void setInternalNotes(String value) { internalNotes = value; }
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false;
        Appointment that = (Appointment) other; return id != null && id.equals(that.id);
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
