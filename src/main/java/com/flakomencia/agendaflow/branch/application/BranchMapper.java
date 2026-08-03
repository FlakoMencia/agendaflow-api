package com.flakomencia.agendaflow.branch.application;

import org.springframework.stereotype.Component;

import com.flakomencia.agendaflow.branch.api.BranchCreateRequest;
import com.flakomencia.agendaflow.branch.api.BranchResponse;
import com.flakomencia.agendaflow.branch.api.BranchUpdateRequest;
import com.flakomencia.agendaflow.branch.domain.Branch;
import com.flakomencia.agendaflow.organization.domain.Organization;

@Component
public class BranchMapper {

    public Branch toEntity(Organization organization, BranchCreateRequest request) {
        Branch branch = new Branch(organization, requiredText(request.name()));
        branch.setCode(optionalText(request.code()));
        branch.setEmail(optionalText(request.email()));
        branch.setPhone(optionalText(request.phone()));
        branch.setAddressLine1(optionalText(request.addressLine1()));
        branch.setAddressLine2(optionalText(request.addressLine2()));
        branch.setCity(optionalText(request.city()));
        branch.setStateCode(optionalText(request.stateCode()));
        branch.setPostalCode(optionalText(request.postalCode()));
        branch.setCountryCode(optionalText(request.countryCode()));
        branch.setTimezone(optionalText(request.timezone()));
        branch.setLatitude(request.latitude());
        branch.setLongitude(request.longitude());
        branch.setActive(request.active());
        return branch;
    }

    public void update(Branch branch, BranchUpdateRequest request) {
        branch.setName(requiredText(request.name()));
        branch.setCode(optionalText(request.code()));
        branch.setEmail(optionalText(request.email()));
        branch.setPhone(optionalText(request.phone()));
        branch.setAddressLine1(optionalText(request.addressLine1()));
        branch.setAddressLine2(optionalText(request.addressLine2()));
        branch.setCity(optionalText(request.city()));
        branch.setStateCode(optionalText(request.stateCode()));
        branch.setPostalCode(optionalText(request.postalCode()));
        if (request.countryCode() != null) {
            branch.setCountryCode(optionalText(request.countryCode()));
        }
        branch.setTimezone(optionalText(request.timezone()));
        branch.setLatitude(request.latitude());
        branch.setLongitude(request.longitude());
        if (request.active() != null) {
            branch.setActive(request.active());
        }
    }

    public BranchResponse toResponse(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getOrganization().getId(),
                branch.getName(),
                branch.getCode(),
                branch.getEmail(),
                branch.getPhone(),
                branch.getAddressLine1(),
                branch.getAddressLine2(),
                branch.getCity(),
                branch.getStateCode(),
                branch.getPostalCode(),
                branch.getCountryCode(),
                branch.getTimezone(),
                branch.getLatitude(),
                branch.getLongitude(),
                branch.getActive(),
                branch.getCreatedAt(),
                branch.getUpdatedAt());
    }

    private String requiredText(String value) {
        return value.trim();
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
