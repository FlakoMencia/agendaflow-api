package com.flakomencia.agendaflow.servicecatalog.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicInsert;

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
@Table(name = "services", schema = "agendaflow")
public class CatalogService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ServiceCategory category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "preparation_minutes", nullable = false)
    private Integer preparationMinutes;

    @Column(name = "cleanup_minutes", nullable = false)
    private Integer cleanupMinutes;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval;

    @Column(name = "allows_online_booking", nullable = false)
    private Boolean allowsOnlineBooking;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "color_code", length = 20)
    private String colorCode;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at", insertable = false)
    private OffsetDateTime deletedAt;

    protected CatalogService() {
    }

    public CatalogService(Organization organization, String name, Integer durationMinutes) {
        this.organization = organization;
        this.name = name;
        this.durationMinutes = durationMinutes;
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public ServiceCategory getCategory() { return category; }
    public void setCategory(ServiceCategory category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public Integer getPreparationMinutes() { return preparationMinutes; }
    public void setPreparationMinutes(Integer preparationMinutes) { this.preparationMinutes = preparationMinutes; }
    public Integer getCleanupMinutes() { return cleanupMinutes; }
    public void setCleanupMinutes(Integer cleanupMinutes) { this.cleanupMinutes = cleanupMinutes; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public Boolean getRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(Boolean requiresApproval) { this.requiresApproval = requiresApproval; }
    public Boolean getAllowsOnlineBooking() { return allowsOnlineBooking; }
    public void setAllowsOnlineBooking(Boolean allowsOnlineBooking) { this.allowsOnlineBooking = allowsOnlineBooking; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false;
        CatalogService that = (CatalogService) other;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
