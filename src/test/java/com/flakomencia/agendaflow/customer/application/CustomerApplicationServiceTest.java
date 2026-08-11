package com.flakomencia.agendaflow.customer.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.flakomencia.agendaflow.common.security.AuthenticatedOrganizationContext;
import com.flakomencia.agendaflow.common.security.TenantAccessGuard;
import com.flakomencia.agendaflow.customer.api.CustomerRequest;
import com.flakomencia.agendaflow.customer.domain.Customer;
import com.flakomencia.agendaflow.customer.infrastructure.CustomerRepository;
import com.flakomencia.agendaflow.organization.application.OrganizationNotFoundException;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.infrastructure.OrganizationRepository;

import jakarta.persistence.EntityManager;

class CustomerApplicationServiceTest {
    private CustomerRepository customers;
    private OrganizationRepository organizations;
    private TenantAccessGuard tenants;
    private CustomerApplicationService service;
    private Organization organization;

    @BeforeEach
    void setUp() {
        customers = mock(CustomerRepository.class); organizations = mock(OrganizationRepository.class);
        tenants = mock(TenantAccessGuard.class); organization = new Organization("AgendaFlow");
        when(tenants.current()).thenReturn(new AuthenticatedOrganizationContext(1L, 1L, 10L, Set.of("PLATFORM_ADMIN"), Set.of()));
        when(organizations.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(organization));
        service = new CustomerApplicationService(customers, organizations, new CustomerMapper(), tenants, mock(EntityManager.class));
    }

    @Test
    void createsCustomerWithoutRequiringEmail() {
        when(customers.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.create(10L, request("Ana", "Lopez", null));
        verify(customers).save(any(Customer.class));
    }

    @Test
    void updatesOrganizationScopedCustomer() {
        Customer customer = new Customer(organization, "Ana", "Lopez");
        when(customers.findByIdAndOrganization_IdAndDeletedAtIsNull(7L, 10L)).thenReturn(Optional.of(customer));
        service.update(10L, 7L, request("Ana Maria", "Lopez", "ana@example.com"));
        verify(customers).findByIdAndOrganization_IdAndDeletedAtIsNull(7L, 10L);
    }

    @Test
    void preservesTenantIsolation() {
        doThrow(new OrganizationNotFoundException(20L)).when(tenants).requireTenant(20L);
        assertThatThrownBy(() -> service.list(20L, PageRequest.of(0, 20)))
                .isInstanceOf(OrganizationNotFoundException.class);
    }

    private CustomerRequest request(String first, String last, String email) {
        return new CustomerRequest(null, first, null, last, null, email, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }
}
