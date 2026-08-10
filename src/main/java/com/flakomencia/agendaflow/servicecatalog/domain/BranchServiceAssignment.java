package com.flakomencia.agendaflow.servicecatalog.domain;

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
@Table(name = "branch_services", schema = "agendaflow")
public class BranchServiceAssignment {

    @EmbeddedId
    private BranchServiceId id;

    @MapsId("branchId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false, updatable = false)
    private Branch branch;

    @MapsId("serviceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false, updatable = false)
    private CatalogService service;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected BranchServiceAssignment() {
    }

    public BranchServiceAssignment(Branch branch, CatalogService service) {
        this.id = new BranchServiceId(branch.getId(), service.getId());
        this.branch = branch;
        this.service = service;
    }

    public BranchServiceId getId() { return id; }
    public Branch getBranch() { return branch; }
    public CatalogService getService() { return service; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
