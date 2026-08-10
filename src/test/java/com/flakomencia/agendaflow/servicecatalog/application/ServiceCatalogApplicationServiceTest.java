package com.flakomencia.agendaflow.servicecatalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.flakomencia.agendaflow.branch.infrastructure.BranchRepository;
import com.flakomencia.agendaflow.common.exception.ConflictException;
import com.flakomencia.agendaflow.common.security.AuthenticatedOrganizationContext;
import com.flakomencia.agendaflow.common.security.TenantAccessGuard;
import com.flakomencia.agendaflow.organization.application.OrganizationNotFoundException;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.infrastructure.OrganizationRepository;
import com.flakomencia.agendaflow.servicecatalog.api.CatalogServiceCreateRequest;
import com.flakomencia.agendaflow.servicecatalog.api.CatalogServiceUpdateRequest;
import com.flakomencia.agendaflow.servicecatalog.domain.CatalogService;
import com.flakomencia.agendaflow.servicecatalog.infrastructure.BranchServiceAssignmentRepository;
import com.flakomencia.agendaflow.servicecatalog.infrastructure.CatalogServiceRepository;
import com.flakomencia.agendaflow.servicecatalog.infrastructure.ServiceCategoryRepository;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogApplicationServiceTest {

    @Mock ServiceCategoryRepository categories;
    @Mock CatalogServiceRepository services;
    @Mock BranchServiceAssignmentRepository branchServices;
    @Mock OrganizationRepository organizations;
    @Mock BranchRepository branches;
    @Mock TenantAccessGuard tenantAccess;
    @Mock EntityManager entityManager;

    private ServiceCatalogApplicationService subject;
    private Organization organization;

    @BeforeEach
    void setUp() {
        subject = new ServiceCatalogApplicationService(
                categories, services, branchServices, organizations, branches, tenantAccess, entityManager);
        organization = new Organization("Test organization");
        lenient().when(tenantAccess.current()).thenReturn(new AuthenticatedOrganizationContext(
                1L, 1L, 1L, Set.of("PLATFORM_ADMIN"), Set.of()));
        lenient().when(organizations.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(organization));
    }

    @Test
    void createsAndNormalizesAService() {
        var response = subject.createService(1L, new CatalogServiceCreateRequest(
                null, "  Consultation  ", null, 30, null, null, new BigDecimal("25.00"),
                "usd", null, null, null, null));

        assertThat(response.name()).isEqualTo("Consultation");
        assertThat(response.preparationMinutes()).isZero();
        assertThat(response.currencyCode()).isEqualTo("USD");
        verify(services).save(any(CatalogService.class));
    }

    @Test
    void updatesAService() {
        CatalogService existing = new CatalogService(organization, "Old", 30);
        existing.setPreparationMinutes(0);
        existing.setCleanupMinutes(0);
        existing.setCurrencyCode("USD");
        existing.setRequiresApproval(false);
        existing.setAllowsOnlineBooking(true);
        existing.setActive(true);
        when(services.findByIdAndOrganization_IdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(existing));

        var response = subject.updateService(1L, 10L, new CatalogServiceUpdateRequest(
                null, "Updated", null, 60, 5, 10, null, "USD", true, false, true, null));

        assertThat(response.name()).isEqualTo("Updated");
        assertThat(response.durationMinutes()).isEqualTo(60);
    }

    @Test
    void rejectsDuplicateServiceName() {
        when(services.existsByOrganization_IdAndNameAndDeletedAtIsNull(1L, "Duplicate")).thenReturn(true);
        assertThatThrownBy(() -> subject.createService(1L, new CatalogServiceCreateRequest(
                null, "Duplicate", null, 30, null, null, null, null, null, null, null, null)))
                .isInstanceOf(ConflictException.class).hasMessageContaining("already exists");
    }

    @Test
    void preservesTenantGuardAtApplicationBoundary() {
        doThrow(new OrganizationNotFoundException(2L)).when(tenantAccess).requireTenant(2L);
        assertThatThrownBy(() -> subject.createService(2L, new CatalogServiceCreateRequest(
                null, "Denied", null, 30, null, null, null, null, null, null, null, null)))
                .isInstanceOf(OrganizationNotFoundException.class);
    }
}
