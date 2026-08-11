package com.flakomencia.agendaflow.customer.application;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flakomencia.agendaflow.common.api.PageResponse;
import com.flakomencia.agendaflow.common.exception.ConflictException;
import com.flakomencia.agendaflow.common.exception.InvalidRequestException;
import com.flakomencia.agendaflow.common.security.TenantAccessGuard;
import com.flakomencia.agendaflow.customer.api.CustomerRequest;
import com.flakomencia.agendaflow.customer.api.CustomerResponse;
import com.flakomencia.agendaflow.customer.domain.Customer;
import com.flakomencia.agendaflow.customer.infrastructure.CustomerRepository;
import com.flakomencia.agendaflow.organization.application.OrganizationNotFoundException;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.domain.OrganizationStatus;
import com.flakomencia.agendaflow.organization.infrastructure.OrganizationRepository;

import jakarta.persistence.EntityManager;

@Service
public class CustomerApplicationService {
    private static final Set<String> SORTS = Set.of("id", "customerNumber", "firstName", "lastName", "email", "createdAt");
    private final CustomerRepository customers;
    private final OrganizationRepository organizations;
    private final CustomerMapper mapper;
    private final TenantAccessGuard tenants;
    private final EntityManager entityManager;

    public CustomerApplicationService(CustomerRepository customers, OrganizationRepository organizations,
            CustomerMapper mapper, TenantAccessGuard tenants, EntityManager entityManager) {
        this.customers = customers;
        this.organizations = organizations;
        this.mapper = mapper;
        this.tenants = tenants;
        this.entityManager = entityManager;
    }

    @Transactional
    public CustomerResponse create(Long organizationId, CustomerRequest request) {
        tenants.requireTenant(organizationId);
        Organization organization = requireOrganization(organizationId);
        validateNumber(organizationId, request.customerNumber(), null);
        Customer customer = customers.save(mapper.toEntity(organization, request));
        entityManager.flush(); entityManager.refresh(customer);
        return mapper.toResponse(customer);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(Long organizationId, Pageable pageable) {
        tenants.requireTenant(organizationId); requireOrganization(organizationId); validateSort(pageable);
        return PageResponse.from(customers.findAllByOrganization_IdAndDeletedAtIsNull(organizationId, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long organizationId, Long customerId) {
        tenants.requireTenant(organizationId); requireOrganization(organizationId);
        return mapper.toResponse(requireCustomer(organizationId, customerId));
    }

    @Transactional
    public CustomerResponse update(Long organizationId, Long customerId, CustomerRequest request) {
        tenants.requireTenant(organizationId); requireOrganization(organizationId);
        Customer customer = requireCustomer(organizationId, customerId);
        validateNumber(organizationId, request.customerNumber(), customerId);
        mapper.update(customer, request); entityManager.flush(); entityManager.refresh(customer);
        return mapper.toResponse(customer);
    }

    private Organization requireOrganization(Long id) {
        if (!tenants.current().isPlatformAdministrator()) {
            return organizations.findByIdAndStatusAndDeletedAtIsNull(id, OrganizationStatus.ACTIVE)
                    .orElseThrow(() -> new OrganizationNotFoundException(id));
        }
        return organizations.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new OrganizationNotFoundException(id));
    }
    private Customer requireCustomer(Long organizationId, Long id) {
        return customers.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new CustomerNotFoundException(organizationId, id));
    }
    private void validateNumber(Long organizationId, String raw, Long currentId) {
        if (raw == null || raw.isBlank()) return;
        String value = raw.trim();
        boolean duplicate = currentId == null
                ? customers.existsByOrganization_IdAndCustomerNumberAndDeletedAtIsNull(organizationId, value)
                : customers.existsByOrganization_IdAndCustomerNumberAndDeletedAtIsNullAndIdNot(organizationId, value, currentId);
        if (duplicate) throw new ConflictException("CUSTOMER_NUMBER_EXISTS", "Customer number already exists in this organization");
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(order -> !SORTS.contains(order.getProperty())))
            throw new InvalidRequestException("Unsupported customer sort property");
    }
}
