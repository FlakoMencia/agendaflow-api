package com.flakomencia.agendaflow.specialist.application;

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

import com.flakomencia.agendaflow.branch.domain.Branch;
import com.flakomencia.agendaflow.branch.infrastructure.BranchRepository;
import com.flakomencia.agendaflow.common.security.AuthenticatedOrganizationContext;
import com.flakomencia.agendaflow.common.security.TenantAccessGuard;
import com.flakomencia.agendaflow.identity.infrastructure.AppUserRepository;
import com.flakomencia.agendaflow.identity.infrastructure.OrganizationMembershipRepository;
import com.flakomencia.agendaflow.organization.application.OrganizationNotFoundException;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.infrastructure.OrganizationRepository;
import com.flakomencia.agendaflow.servicecatalog.domain.CatalogService;
import com.flakomencia.agendaflow.servicecatalog.infrastructure.CatalogServiceRepository;
import com.flakomencia.agendaflow.specialist.api.SpecialistBranchAssignmentRequest;
import com.flakomencia.agendaflow.specialist.api.SpecialistCreateRequest;
import com.flakomencia.agendaflow.specialist.api.SpecialistServiceAssignmentRequest;
import com.flakomencia.agendaflow.specialist.api.SpecialistUpdateRequest;
import com.flakomencia.agendaflow.specialist.domain.Specialist;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistBranchAssignmentRepository;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistRepository;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistServiceAssignmentRepository;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class SpecialistApplicationServiceTest {

    @Mock SpecialistRepository specialists;
    @Mock SpecialistBranchAssignmentRepository specialistBranches;
    @Mock SpecialistServiceAssignmentRepository specialistServices;
    @Mock OrganizationRepository organizations;
    @Mock BranchRepository branches;
    @Mock CatalogServiceRepository services;
    @Mock AppUserRepository users;
    @Mock OrganizationMembershipRepository memberships;
    @Mock TenantAccessGuard tenantAccess;
    @Mock EntityManager entityManager;

    private SpecialistApplicationService subject;
    private Organization organization;

    @BeforeEach
    void setUp() {
        subject = new SpecialistApplicationService(specialists, specialistBranches, specialistServices,
                organizations, branches, services, users, memberships, tenantAccess, entityManager);
        organization = new Organization("Test organization");
        lenient().when(tenantAccess.current()).thenReturn(new AuthenticatedOrganizationContext(
                1L, 1L, 1L, Set.of("PLATFORM_ADMIN"), Set.of()));
        lenient().when(organizations.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(organization));
    }

    @Test
    void createsAndUpdatesSpecialist() {
        var created = subject.create(1L, new SpecialistCreateRequest(
                null, "  Dr. Rivera  ", "Therapy", null, null, null, null, null, null, null));
        assertThat(created.professionalName()).isEqualTo("Dr. Rivera");
        assertThat(created.simultaneousCapacity()).isEqualTo(1);
        verify(specialists).save(any(Specialist.class));

        Specialist existing = new Specialist(organization, "Old");
        existing.setSimultaneousCapacity(1);
        existing.setActive(true);
        when(specialists.findByIdAndOrganization_IdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(existing));
        var updated = subject.update(1L, 10L, new SpecialistUpdateRequest(
                null, "Updated", null, null, null, null, null, null, 2, false));
        assertThat(updated.professionalName()).isEqualTo("Updated");
        assertThat(updated.simultaneousCapacity()).isEqualTo(2);
    }

    @Test
    void assignsBranchAndServiceInsideTenant() {
        Specialist specialist = orgSpecialist();
        Branch branch = orgBranch();
        CatalogService catalogService = orgService();
        when(specialists.findByIdAndOrganization_IdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(specialist));
        when(branches.findByIdAndOrganization_IdAndDeletedAtIsNull(20L, 1L)).thenReturn(Optional.of(branch));
        when(services.findByIdAndOrganization_IdAndDeletedAtIsNull(30L, 1L)).thenReturn(Optional.of(catalogService));

        var branchResponse = subject.assignBranch(1L, 10L, 20L,
                new SpecialistBranchAssignmentRequest(true, true));
        var serviceResponse = subject.assignService(1L, 10L, 30L,
                new SpecialistServiceAssignmentRequest(45, new BigDecimal("50.00"), true));

        assertThat(branchResponse.primary()).isTrue();
        assertThat(serviceResponse.customDurationMinutes()).isEqualTo(45);
    }

    @Test
    void rejectsCrossTenantBeforeRepositoryMutation() {
        doThrow(new OrganizationNotFoundException(2L)).when(tenantAccess).requireTenant(2L);
        assertThatThrownBy(() -> subject.create(2L, new SpecialistCreateRequest(
                null, "Denied", null, null, null, null, null, null, 1, true)))
                .isInstanceOf(OrganizationNotFoundException.class);
    }

    private Specialist orgSpecialist() {
        Specialist value = org.mockito.Mockito.mock(Specialist.class);
        when(value.getId()).thenReturn(10L);
        return value;
    }

    private Branch orgBranch() {
        Branch value = org.mockito.Mockito.mock(Branch.class);
        when(value.getId()).thenReturn(20L);
        when(value.getName()).thenReturn("Central");
        return value;
    }

    private CatalogService orgService() {
        CatalogService value = org.mockito.Mockito.mock(CatalogService.class);
        when(value.getId()).thenReturn(30L);
        when(value.getName()).thenReturn("Consultation");
        return value;
    }
}
