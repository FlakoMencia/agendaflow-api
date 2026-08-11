package com.flakomencia.agendaflow.customer.infrastructure;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.flakomencia.agendaflow.customer.domain.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Page<Customer> findAllByOrganization_IdAndDeletedAtIsNull(Long organizationId, Pageable pageable);
    Optional<Customer> findByIdAndOrganization_IdAndDeletedAtIsNull(Long id, Long organizationId);
    boolean existsByOrganization_IdAndCustomerNumberAndDeletedAtIsNull(Long organizationId, String customerNumber);
    boolean existsByOrganization_IdAndCustomerNumberAndDeletedAtIsNullAndIdNot(
            Long organizationId, String customerNumber, Long id);
}
