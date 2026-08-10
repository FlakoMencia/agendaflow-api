package com.flakomencia.agendaflow.specialist.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.hibernate.annotations.DynamicInsert;

import com.flakomencia.agendaflow.servicecatalog.domain.CatalogService;

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
@Table(name = "specialist_services", schema = "agendaflow")
public class SpecialistServiceAssignment {

    @EmbeddedId
    private SpecialistServiceId id;

    @MapsId("specialistId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialist_id", nullable = false, updatable = false)
    private Specialist specialist;

    @MapsId("serviceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false, updatable = false)
    private CatalogService service;

    @Column(name = "custom_duration_minutes")
    private Integer customDurationMinutes;

    @Column(name = "custom_price", precision = 12, scale = 2)
    private BigDecimal customPrice;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected SpecialistServiceAssignment() {
    }

    public SpecialistServiceAssignment(Specialist specialist, CatalogService service) {
        this.id = new SpecialistServiceId(specialist.getId(), service.getId());
        this.specialist = specialist;
        this.service = service;
    }

    public SpecialistServiceId getId() { return id; }
    public Specialist getSpecialist() { return specialist; }
    public CatalogService getService() { return service; }
    public Integer getCustomDurationMinutes() { return customDurationMinutes; }
    public void setCustomDurationMinutes(Integer value) { customDurationMinutes = value; }
    public BigDecimal getCustomPrice() { return customPrice; }
    public void setCustomPrice(BigDecimal value) { customPrice = value; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
