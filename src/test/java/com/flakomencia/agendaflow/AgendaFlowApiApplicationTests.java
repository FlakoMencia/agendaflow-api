package com.flakomencia.agendaflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.flakomencia.agendaflow.branch.application.BranchService;
import com.flakomencia.agendaflow.identity.application.AuthenticationService;
import com.flakomencia.agendaflow.organization.application.OrganizationService;
import com.flakomencia.agendaflow.scheduling.application.SchedulingApplicationService;
import com.flakomencia.agendaflow.servicecatalog.application.ServiceCatalogApplicationService;
import com.flakomencia.agendaflow.specialist.application.SpecialistApplicationService;

@SpringBootTest
@ActiveProfiles("test")
class AgendaFlowApiApplicationTests {

    @MockitoBean
    OrganizationService organizationService;

    @MockitoBean
    BranchService branchService;

    @MockitoBean
    AuthenticationService authenticationService;

    @MockitoBean
    ServiceCatalogApplicationService serviceCatalogApplicationService;

    @MockitoBean
    SpecialistApplicationService specialistApplicationService;

    @MockitoBean
    SchedulingApplicationService schedulingApplicationService;

    @Test
    void contextLoads() {
    }
}
