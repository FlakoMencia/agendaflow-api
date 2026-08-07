package com.flakomencia.agendaflow.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.flakomencia.agendaflow.common.exception.ConflictException;
import com.flakomencia.agendaflow.common.security.AuthenticatedOrganizationContext;
import com.flakomencia.agendaflow.common.security.TenantAccessGuard;
import com.flakomencia.agendaflow.organization.api.OrganizationCreateRequest;
import com.flakomencia.agendaflow.organization.api.OrganizationResponse;
import com.flakomencia.agendaflow.organization.api.OrganizationUpdateRequest;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.domain.OrganizationStatus;
import com.flakomencia.agendaflow.organization.infrastructure.OrganizationRepository;

import jakarta.persistence.EntityManager;

class OrganizationServiceTest {

    private OrganizationRepository repository;
    private OrganizationMapper mapper;
    private EntityManager entityManager;
    private TenantAccessGuard tenantAccess;
    private OrganizationService service;

    @BeforeEach
    void setUp() {
        repository = mock(OrganizationRepository.class);
        mapper = mock(OrganizationMapper.class);
        entityManager = mock(EntityManager.class);
        tenantAccess = mock(TenantAccessGuard.class);
        when(tenantAccess.current()).thenReturn(
                new AuthenticatedOrganizationContext(1L, 1L, 1L, Set.of("PLATFORM_ADMIN"), Set.of()));
        service = new OrganizationService(repository, mapper, entityManager, tenantAccess);
    }

    @Test
    void createsOrganization() {
        OrganizationCreateRequest request = createRequest("AgendaFlow LLC", "TAX-100");
        Organization organization = new Organization("AgendaFlow LLC");
        OrganizationResponse response = mock(OrganizationResponse.class);
        when(repository.existsByTaxIdentifierAndDeletedAtIsNull("TAX-100")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(organization);
        when(repository.save(organization)).thenReturn(organization);
        when(mapper.toResponse(organization)).thenReturn(response);

        assertThat(service.create(request)).isSameAs(response);

        verify(entityManager).flush();
        verify(entityManager).refresh(organization);
    }

    @Test
    void updatesOrganization() {
        Organization organization = new Organization("Before");
        OrganizationUpdateRequest request = updateRequest("After", "TAX-101");
        OrganizationResponse response = mock(OrganizationResponse.class);
        when(repository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(organization));
        when(repository.existsByTaxIdentifierAndDeletedAtIsNullAndIdNot("TAX-101", 10L)).thenReturn(false);
        when(mapper.toResponse(organization)).thenReturn(response);

        assertThat(service.update(10L, request)).isSameAs(response);

        verify(mapper).update(organization, request);
        verify(entityManager).flush();
        verify(entityManager).refresh(organization);
    }

    @Test
    void rejectsMissingOrganization() {
        when(repository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(OrganizationNotFoundException.class)
                .hasMessage("Organization 99 was not found");
    }

    @Test
    void rejectsDuplicateTaxIdentifier() {
        OrganizationCreateRequest request = createRequest("Duplicate", "TAX-100");
        when(repository.existsByTaxIdentifierAndDeletedAtIsNull("TAX-100")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("TAX-100");

        verify(mapper, never()).toEntity(request);
    }

    private OrganizationCreateRequest createRequest(String legalName, String taxIdentifier) {
        return new OrganizationCreateRequest(
                legalName, null, taxIdentifier, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, OrganizationStatus.ACTIVE);
    }

    private OrganizationUpdateRequest updateRequest(String legalName, String taxIdentifier) {
        return new OrganizationUpdateRequest(
                legalName, null, taxIdentifier, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, OrganizationStatus.ACTIVE);
    }
}
