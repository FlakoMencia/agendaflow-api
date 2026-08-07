package com.flakomencia.agendaflow.branch.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.flakomencia.agendaflow.branch.api.BranchCreateRequest;
import com.flakomencia.agendaflow.branch.domain.Branch;
import com.flakomencia.agendaflow.organization.domain.Organization;

class BranchMapperTest {

    private final BranchMapper mapper = new BranchMapper();

    @Test
    void mapsBranchWithoutChangingOrganization() {
        Organization organization = new Organization("AgendaFlow");
        BranchCreateRequest request = new BranchCreateRequest(
                "  Central  ", "  CENTRAL  ", null, null, null, null, null, null, null, null,
                null, null, null, null);

        Branch branch = mapper.toEntity(organization, request);

        assertThat(branch.getOrganization()).isSameAs(organization);
        assertThat(branch.getName()).isEqualTo("Central");
        assertThat(branch.getCode()).isEqualTo("CENTRAL");
    }
}
