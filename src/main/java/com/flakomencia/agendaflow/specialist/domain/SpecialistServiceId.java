package com.flakomencia.agendaflow.specialist.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record SpecialistServiceId(
        @Column(name = "specialist_id") Long specialistId,
        @Column(name = "service_id") Long serviceId) implements Serializable {
}
