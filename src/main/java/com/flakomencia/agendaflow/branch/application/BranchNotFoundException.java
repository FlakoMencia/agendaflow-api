package com.flakomencia.agendaflow.branch.application;

import org.springframework.http.HttpStatus;

import com.flakomencia.agendaflow.common.exception.ApiException;

public class BranchNotFoundException extends ApiException {

    public BranchNotFoundException(Long organizationId, Long branchId) {
        super(HttpStatus.NOT_FOUND,
                "BRANCH_NOT_FOUND",
                "Branch %d was not found in organization %d".formatted(branchId, organizationId));
    }
}
