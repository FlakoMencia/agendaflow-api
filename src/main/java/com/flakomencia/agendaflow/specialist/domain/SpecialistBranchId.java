package com.flakomencia.agendaflow.specialist.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record SpecialistBranchId(
        @Column(name = "specialist_id") Long specialistId,
        @Column(name = "branch_id") Long branchId) implements Serializable {
}
