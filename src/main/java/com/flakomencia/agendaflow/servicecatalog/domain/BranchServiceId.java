package com.flakomencia.agendaflow.servicecatalog.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record BranchServiceId(
        @Column(name = "branch_id") Long branchId,
        @Column(name = "service_id") Long serviceId) implements Serializable {

    public BranchServiceId {
    }
}
