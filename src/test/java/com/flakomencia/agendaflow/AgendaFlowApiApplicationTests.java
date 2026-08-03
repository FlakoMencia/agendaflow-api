package com.flakomencia.agendaflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.flakomencia.agendaflow.branch.application.BranchService;
import com.flakomencia.agendaflow.organization.application.OrganizationService;

@SpringBootTest
@ActiveProfiles("test")
class AgendaFlowApiApplicationTests {

    @MockitoBean
    OrganizationService organizationService;

    @MockitoBean
    BranchService branchService;

    @Test
    void contextLoads() {
    }
}
