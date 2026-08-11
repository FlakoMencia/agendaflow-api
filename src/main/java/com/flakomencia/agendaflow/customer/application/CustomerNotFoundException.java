package com.flakomencia.agendaflow.customer.application;

import org.springframework.http.HttpStatus;

import com.flakomencia.agendaflow.common.exception.ApiException;

public class CustomerNotFoundException extends ApiException {
    public CustomerNotFoundException(Long organizationId, Long customerId) {
        super(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND",
                "Customer %d was not found in organization %d".formatted(customerId, organizationId));
    }
}
