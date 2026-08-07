package com.flakomencia.agendaflow.common.system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.flakomencia.agendaflow.branch.application.BranchService;
import com.flakomencia.agendaflow.identity.application.AuthenticationService;
import com.flakomencia.agendaflow.organization.application.OrganizationService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemInfoControllerTests {

    @MockitoBean
    OrganizationService organizationService;

    @MockitoBean
    BranchService branchService;

    @MockitoBean
    AuthenticationService authenticationService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsBootstrapInformation() throws Exception {
        mockMvc.perform(get("/api/v1/system/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("agendaflow-api"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.phase").value("bootstrap"));
    }

    @Test
    void deniesUnapprovedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/not-permitted"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }
}
