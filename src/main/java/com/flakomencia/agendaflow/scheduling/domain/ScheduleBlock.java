package com.flakomencia.agendaflow.scheduling.domain;

import java.time.OffsetDateTime;

import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicInsert;

import com.flakomencia.agendaflow.branch.domain.Branch;
import com.flakomencia.agendaflow.identity.domain.AppUser;
import com.flakomencia.agendaflow.organization.domain.Organization;
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
@DynamicInsert
@Table(name = "schedule_blocks", schema = "agendaflow")
public class ScheduleBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialist_id", updatable = false)
    private Specialist specialist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 30)
    private ScheduleBlockType blockType;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected ScheduleBlock() {
    }

    public ScheduleBlock(Organization organization, Specialist specialist, AppUser createdBy) {
        this.organization = organization;
        this.specialist = specialist;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public Specialist getSpecialist() { return specialist; }
    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }
    public ScheduleBlockType getBlockType() { return blockType; }
    public void setBlockType(ScheduleBlockType blockType) { this.blockType = blockType; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(OffsetDateTime startsAt) { this.startsAt = startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(OffsetDateTime endsAt) { this.endsAt = endsAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public AppUser getCreatedBy() { return createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false;
        ScheduleBlock that = (ScheduleBlock) other;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
