package com.flakomencia.agendaflow.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.flakomencia.agendaflow.organization.api.OrganizationCreateRequest;
import com.flakomencia.agendaflow.organization.domain.Organization;

class OrganizationMapperTest {

    private final OrganizationMapper mapper = new OrganizationMapper();

    @Test
    void trimsRequiredAndOptionalValues() {
        OrganizationCreateRequest request = new OrganizationCreateRequest(
                "  AgendaFlow LLC  ", "  AgendaFlow  ", "  TAX-1  ", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null);

        Organization organization = mapper.toEntity(request);

        assertThat(organization.getLegalName()).isEqualTo("AgendaFlow LLC");
        assertThat(organization.getTradeName()).isEqualTo("AgendaFlow");
        assertThat(organization.getTaxIdentifier()).isEqualTo("TAX-1");
    }
}
