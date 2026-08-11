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
import com.flakomencia.agendaflow.appointment.application.AppointmentApplicationService;
import com.flakomencia.agendaflow.customer.application.CustomerApplicationService;
import com.flakomencia.agendaflow.identity.application.AuthenticationService;
import com.flakomencia.agendaflow.organization.application.OrganizationService;
import com.flakomencia.agendaflow.notification.application.AppointmentNotificationOutboxService;
import com.flakomencia.agendaflow.notification.application.NotificationOutboxClaimService;
import com.flakomencia.agendaflow.notification.application.NotificationOutboxStateService;
import com.flakomencia.agendaflow.scheduling.application.SchedulingApplicationService;
import com.flakomencia.agendaflow.scheduling.application.AvailableSlotService;
import com.flakomencia.agendaflow.servicecatalog.application.ServiceCatalogApplicationService;
import com.flakomencia.agendaflow.specialist.application.SpecialistApplicationService;

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

    @MockitoBean
    ServiceCatalogApplicationService serviceCatalogApplicationService;

    @MockitoBean
    SpecialistApplicationService specialistApplicationService;

    @MockitoBean
    SchedulingApplicationService schedulingApplicationService;

    @MockitoBean
    CustomerApplicationService customerApplicationService;

    @MockitoBean
    AppointmentApplicationService appointmentApplicationService;

    @MockitoBean
    AvailableSlotService availableSlotService;

    @MockitoBean AppointmentNotificationOutboxService appointmentNotificationOutboxService;
    @MockitoBean NotificationOutboxClaimService notificationOutboxClaimService;
    @MockitoBean NotificationOutboxStateService notificationOutboxStateService;

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
