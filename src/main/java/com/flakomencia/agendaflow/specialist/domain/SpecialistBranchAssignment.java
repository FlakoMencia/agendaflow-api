package com.flakomencia.agendaflow.specialist.domain;

import java.time.OffsetDateTime;

import org.hibernate.annotations.DynamicInsert;

import com.flakomencia.agendaflow.branch.domain.Branch;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@DynamicInsert
@Table(name = "specialist_branches", schema = "agendaflow")
public class SpecialistBranchAssignment {

    @EmbeddedId
    private SpecialistBranchId id;

    @MapsId("specialistId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialist_id", nullable = false, updatable = false)
    private Specialist specialist;

    @MapsId("branchId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false, updatable = false)
    private Branch branch;

    @Column(name = "is_primary", nullable = false)
    private Boolean primary;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected SpecialistBranchAssignment() {
    }

    public SpecialistBranchAssignment(Specialist specialist, Branch branch) {
        this.id = new SpecialistBranchId(specialist.getId(), branch.getId());
        this.specialist = specialist;
        this.branch = branch;
    }

    public SpecialistBranchId getId() { return id; }
    public Specialist getSpecialist() { return specialist; }
    public Branch getBranch() { return branch; }
    public Boolean getPrimary() { return primary; }
    public void setPrimary(Boolean primary) { this.primary = primary; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
