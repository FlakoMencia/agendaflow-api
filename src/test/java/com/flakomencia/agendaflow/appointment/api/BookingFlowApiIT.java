package com.flakomencia.agendaflow.appointment.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.flakomencia.agendaflow.notification.application.NotificationOutboxClaimService;
import com.flakomencia.agendaflow.notification.application.NotificationOutboxStateService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
class BookingFlowApiIT {
    private static final AtomicInteger USERS = new AtomicInteger();
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 17);

    @Container
    static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:18.4")
            .withDatabaseName("agendaflow_booking_test").withUsername("agendaflow_user");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
        registry.add("agendaflow.scheduling.slot-interval-minutes", () -> 15);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper json;
    @Autowired NotificationOutboxClaimService outboxClaims;
    @Autowired NotificationOutboxStateService outboxStates;
    private String platformToken;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM agendaflow.notification_outbox");
        jdbc.update("DELETE FROM agendaflow.appointment_status_history");
        jdbc.update("DELETE FROM agendaflow.waitlist_requests");
        jdbc.update("DELETE FROM agendaflow.appointments");
        jdbc.update("DELETE FROM agendaflow.customers");
        jdbc.update("DELETE FROM agendaflow.schedule_blocks");
        jdbc.update("DELETE FROM agendaflow.availability_schedules");
        jdbc.update("DELETE FROM agendaflow.specialist_services");
        jdbc.update("DELETE FROM agendaflow.specialist_branches");
        jdbc.update("DELETE FROM agendaflow.specialists");
        jdbc.update("DELETE FROM agendaflow.branch_services");
        jdbc.update("DELETE FROM agendaflow.services");
        jdbc.update("DELETE FROM agendaflow.service_categories");
        jdbc.update("DELETE FROM agendaflow.membership_roles");
        jdbc.update("DELETE FROM agendaflow.organization_memberships");
        jdbc.update("DELETE FROM agendaflow.app_users");
        jdbc.update("DELETE FROM agendaflow.branches");
        jdbc.update("DELETE FROM agendaflow.organizations");
        platformToken = createSession(createOrganization("Platform"), "PLATFORM_ADMIN");
    }

    @Test
    void executesLifecycleHistoryRbacAndTenantIsolation() throws Exception {
        Fixture f = fixture(1, 30, 0, 0, null);
        long appointmentId = createAppointment(f, "2026-08-17T09:00:00-06:00", platformToken, 201);
        lifecycle(f, appointmentId, "confirm", "CONFIRMED");
        lifecycle(f, appointmentId, "check-in", "CHECKED_IN");
        lifecycle(f, appointmentId, "start", "IN_PROGRESS");
        lifecycle(f, appointmentId, "complete", "COMPLETED");
        mvc.perform(get("/api/v1/organizations/{organizationId}/appointments/{id}/history", f.organizationId(), appointmentId)
                        .header("Authorization", bearer(platformToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[1].previousStatus").value("PENDING"))
                .andExpect(jsonPath("$[4].newStatus").value("COMPLETED"));
        assertThat(jdbc.queryForObject("SELECT confirmed_at IS NOT NULL AND checked_in_at IS NOT NULL "
                + "AND started_service_at IS NOT NULL AND completed_at IS NOT NULL FROM agendaflow.appointments WHERE id=?",
                Boolean.class, appointmentId)).isTrue();

        long second = createAppointment(f, "2026-08-17T10:00:00-06:00", platformToken, 201);
        OffsetDateTime future = OffsetDateTime.now().plusDays(1);
        jdbc.update("UPDATE agendaflow.appointments SET starts_at=?, ends_at=? WHERE id=?",
                future, future.plusMinutes(30), second);
        mvc.perform(post("/api/v1/organizations/{organizationId}/appointments/{id}/no-show", f.organizationId(), second)
                        .header("Authorization", bearer(platformToken)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_DUE"));
        OffsetDateTime past = OffsetDateTime.now().minusDays(1);
        jdbc.update("UPDATE agendaflow.appointments SET starts_at=?, ends_at=? WHERE id=?",
                past, past.plusMinutes(30), second);
        lifecycle(f, second, "no-show", "NO_SHOW");

        long third = createAppointment(f, "2026-08-17T11:00:00-06:00", platformToken, 201);
        String auditor = createSession(f.organizationId(), "AUDITOR");
        mvc.perform(post("/api/v1/organizations/{organizationId}/appointments/{id}/complete", f.organizationId(), third)
                        .header("Authorization", bearer(auditor)))
                .andExpect(status().isForbidden());
        Long foreign = createOrganization("Foreign lifecycle");
        mvc.perform(post("/api/v1/organizations/{organizationId}/appointments/{id}/confirm", foreign, third)
                        .header("Authorization", bearer(platformToken)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_FOUND"));
    }

    @Test
    void writesEligibleAppointmentEventsWithStablePayloadAndNoLifecycleNoise() throws Exception {
        Fixture f = fixture(1, 30, 0, 0, null);
        jdbc.update("UPDATE agendaflow.customers SET email='Grace@Example.COM', email_consent=TRUE, preferred_language='es-SV' WHERE id=?",
                f.customerId());
        long appointmentId = createAppointment(f, "2026-08-17T09:00:00-06:00", platformToken, 201);
        mvc.perform(post("/api/v1/organizations/{organizationId}/appointments/{id}/reschedule", f.organizationId(), appointmentId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startsAt\":\"2026-08-17T10:00:00-06:00\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/organizations/{organizationId}/appointments/{id}/cancel", f.organizationId(), appointmentId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        List<String> types = jdbc.queryForList("SELECT event_type FROM agendaflow.notification_outbox ORDER BY id", String.class);
        assertThat(types).containsExactly("APPOINTMENT_CREATED", "APPOINTMENT_RESCHEDULED", "APPOINTMENT_CANCELLED");
        String payload = jdbc.queryForObject("SELECT payload::text FROM agendaflow.notification_outbox ORDER BY id LIMIT 1", String.class);
        JsonNode event = json.readTree(payload);
        assertThat(event.get("eventId").asLong()).isPositive();
        assertThat(event.get("organizationId").asLong()).isEqualTo(f.organizationId());
        assertThat(event.get("appointmentId").asLong()).isEqualTo(appointmentId);
        assertThat(event.get("type").asText()).isEqualTo("CREATED");
        assertThat(event.get("recipient").asText()).isEqualTo("grace@example.com");
        assertThat(event.get("locale").asText()).isEqualTo("es-SV");
        assertThat(event.get("variables").get("serviceName").asText()).isEqualTo("Consultation");
        assertThat(event.get("variables").get("specialistName").asText()).isEqualTo("Taylor");

        Fixture ineligible = fixture(1, 30, 0, 0, null);
        createAppointment(ineligible, "2026-08-17T09:00:00-06:00", platformToken, 201);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agendaflow.notification_outbox WHERE organization_id=?",
                Long.class, ineligible.organizationId())).isZero();
        jdbc.update("UPDATE agendaflow.customers SET email='no-consent@example.com', email_consent=FALSE WHERE id=?",
                ineligible.customerId());
        createAppointment(ineligible, "2026-08-17T10:00:00-06:00", platformToken, 201);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agendaflow.notification_outbox WHERE organization_id=?",
                Long.class, ineligible.organizationId())).isZero();
    }

    @Test
    void rollsBackAppointmentWhenDurableOutboxInsertFails() throws Exception {
        Fixture f = fixture(1, 30, 0, 0, null);
        jdbc.update("UPDATE agendaflow.customers SET email='atomic@example.com', email_consent=TRUE WHERE id=?", f.customerId());
        jdbc.execute("CREATE FUNCTION agendaflow.reject_outbox_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'test outbox rejection'; END $$");
        jdbc.execute("CREATE TRIGGER reject_outbox_test BEFORE INSERT ON agendaflow.notification_outbox FOR EACH ROW EXECUTE FUNCTION agendaflow.reject_outbox_test()");
        try {
            createAppointment(f, "2026-08-17T09:00:00-06:00", platformToken, 500);
        } finally {
            jdbc.execute("DROP TRIGGER reject_outbox_test ON agendaflow.notification_outbox");
            jdbc.execute("DROP FUNCTION agendaflow.reject_outbox_test()");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agendaflow.appointments WHERE organization_id=?", Long.class,
                f.organizationId())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agendaflow.notification_outbox WHERE organization_id=?", Long.class,
                f.organizationId())).isZero();
    }

    @Test
    void claimsOnceAcrossConcurrentWorkersRetriesAndRecoversStaleRows() throws Exception {
        Fixture f = fixture(1, 30, 0, 0, null);
        jdbc.update("UPDATE agendaflow.customers SET email='worker@example.com', email_consent=TRUE WHERE id=?", f.customerId());
        createAppointment(f, "2026-08-17T09:00:00-06:00", platformToken, 201);
        OffsetDateTime now = OffsetDateTime.now();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> claim = () -> { start.await(10, TimeUnit.SECONDS); return outboxClaims.claim(1, now).size(); };
        Future<Integer> first = executor.submit(claim); Future<Integer> second = executor.submit(claim); start.countDown();
        assertThat(first.get(20, TimeUnit.SECONDS) + second.get(20, TimeUnit.SECONDS)).isEqualTo(1);
        executor.shutdown(); assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        Long id = jdbc.queryForObject("SELECT id FROM agendaflow.notification_outbox", Long.class);
        outboxStates.markFailed(id, "HTTP 503", now, 2, java.time.Duration.ofSeconds(10));
        assertThat(jdbc.queryForObject("SELECT status FROM agendaflow.notification_outbox WHERE id=?", String.class, id))
                .isEqualTo("PENDING");
        jdbc.update("UPDATE agendaflow.notification_outbox SET status='PROCESSING', processing_started_at=? WHERE id=?",
                now.minusMinutes(10), id);
        assertThat(outboxClaims.recoverStale(now.minusMinutes(5), now)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM agendaflow.notification_outbox WHERE id=?", String.class, id))
                .isEqualTo("PENDING");
        jdbc.update("UPDATE agendaflow.notification_outbox SET next_attempt_at=? WHERE id=?", now.minusSeconds(1), id);
        assertThat(outboxClaims.claim(1, now)).hasSize(1);
        outboxStates.markFailed(id, "HTTP 401", now, 2, java.time.Duration.ofSeconds(10));
        assertThat(jdbc.queryForObject("SELECT status FROM agendaflow.notification_outbox WHERE id=?", String.class, id))
                .isEqualTo("EXHAUSTED");

        mvc.perform(post("/api/v1/organizations/{organizationId}/appointments/{id}/reschedule", f.organizationId(),
                        jdbc.queryForObject("SELECT aggregate_id FROM agendaflow.notification_outbox WHERE id=?", Long.class, id))
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startsAt\":\"2026-08-17T10:00:00-06:00\"}"))
                .andExpect(status().isOk());
        Long publishable = jdbc.queryForObject("SELECT max(id) FROM agendaflow.notification_outbox", Long.class);
        jdbc.update("UPDATE agendaflow.notification_outbox SET next_attempt_at=? WHERE id=?", now.minusSeconds(1), publishable);
        assertThat(outboxClaims.claim(1, now)).hasSize(1);
        outboxStates.markPublished(publishable, now);
        assertThat(jdbc.queryForObject("SELECT status FROM agendaflow.notification_outbox WHERE id=?", String.class, publishable))
                .isEqualTo("PUBLISHED");
    }

    @Test
    void managesCustomersWithTenantScopeAndPermissions() throws Exception {
        Long organizationId = createOrganization("Customer Org");
        Long otherOrganizationId = createOrganization("Other Customer Org");
        mvc.perform(post("/api/v1/organizations/{organizationId}/customers", organizationId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"phone\":\"555-0100\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").isEmpty())
                .andExpect(jsonPath("$.firstName").value("Ada"));
        Long customerId = jdbc.queryForObject("SELECT id FROM agendaflow.customers WHERE organization_id=?", Long.class, organizationId);
        mvc.perform(get("/api/v1/organizations/{organizationId}/customers", organizationId)
                        .header("Authorization", bearer(platformToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(put("/api/v1/organizations/{organizationId}/customers/{customerId}", organizationId, customerId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"email\":\"ADA@EXAMPLE.COM\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("ada@example.com"));
        mvc.perform(get("/api/v1/organizations/{organizationId}/customers/{customerId}", otherOrganizationId, customerId)
                        .header("Authorization", bearer(platformToken)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
        String auditor = createSession(organizationId, "AUDITOR");
        mvc.perform(post("/api/v1/organizations/{organizationId}/customers", organizationId)
                        .header("Authorization", bearer(auditor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Denied\",\"lastName\":\"Customer\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/organizations/{organizationId}/customers", organizationId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void completesBookingRescheduleCancellationAndHistoryLifecycle() throws Exception {
        Fixture f = fixture(1, 30, 0, 0, null);
        mvc.perform(get("/api/v1/organizations/{organizationId}/availability/slots", f.organizationId())
                        .header("Authorization", bearer(platformToken)).param("branchId", f.branchId().toString())
                        .param("serviceId", f.serviceId().toString()).param("date", MONDAY.toString())
                        .param("specialistId", f.specialistId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timezone").value("America/El_Salvador"))
                .andExpect(jsonPath("$.slots[0].start").value("2026-08-17T09:00:00-06:00"));

        long appointmentId = createAppointment(f, "2026-08-17T09:00:00-06:00", platformToken, 201);
        mvc.perform(get("/api/v1/organizations/{organizationId}/appointments/{id}", f.organizationId(), appointmentId)
                        .header("Authorization", bearer(platformToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
        mvc.perform(get("/api/v1/organizations/{organizationId}/appointments", f.organizationId())
                        .header("Authorization", bearer(platformToken)).param("customerId", f.customerId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        assertSlot(f, "09:00:00", false);

        mvc.perform(post("/api/v1/organizations/{organizationId}/appointments/{id}/reschedule", f.organizationId(), appointmentId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startsAt\":\"2026-08-17T10:00:00-06:00\",\"reason\":\"Customer request\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.startsAt").value("2026-08-17T10:00:00-06:00"));
        assertSlot(f, "09:00:00", true); assertSlot(f, "10:00:00", false);

        jdbc.update("""
                INSERT INTO agendaflow.schedule_blocks
                    (organization_id,specialist_id,branch_id,block_type,starts_at,ends_at)
                VALUES (?,?,?,'MEETING','2026-08-17T11:00:00-06:00','2026-08-17T12:00:00-06:00')
                """, f.organizationId(), f.specialistId(), f.branchId());
        assertSlot(f, "11:00:00", false);

        mvc.perform(post("/api/v1/organizations/{organizationId}/appointments/{id}/cancel", f.organizationId(), appointmentId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Cancelled by customer\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertSlot(f, "10:00:00", true);
        mvc.perform(get("/api/v1/organizations/{organizationId}/appointments/{id}/history", f.organizationId(), appointmentId)
                        .header("Authorization", bearer(platformToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].newStatus").value("PENDING"))
                .andExpect(jsonPath("$[2].newStatus").value("CANCELLED"));
        mvc.perform(post("/api/v1/organizations/{organizationId}/appointments/{id}/cancel", f.organizationId(), appointmentId)
                        .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("APPOINTMENT_ALREADY_CANCELLED"));
        Long other = createOrganization("Foreign");
        mvc.perform(get("/api/v1/organizations/{organizationId}/appointments/{id}", other, appointmentId)
                        .header("Authorization", bearer(platformToken)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_FOUND"));
        mvc.perform(get("/api/v1/organizations/{organizationId}/appointments", f.organizationId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pessimisticSpecialistLockAllowsExactlyOneBookingAtCapacityOne() throws Exception {
        Fixture fixture = fixture(1, 30, 0, 0, null);
        List<Integer> statuses = concurrentBookings(fixture, 2, "2026-08-17T09:00:00-06:00");
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(activeAppointments(fixture)).isEqualTo(1);
    }

    @Test
    void capacityTwoAllowsExactlyTwoConcurrentBookings() throws Exception {
        Fixture fixture = fixture(2, 30, 0, 0, null);
        List<Integer> statuses = concurrentBookings(fixture, 3, "2026-08-17T09:00:00-06:00");
        assertThat(statuses).containsExactlyInAnyOrder(201, 201, 409);
        assertThat(activeAppointments(fixture)).isEqualTo(2);
    }

    @Test
    void customDurationAndBuffersAreAppliedAndOpenApiDocumentsPhaseFive() throws Exception {
        Fixture f = fixture(1, 30, 5, 10, 45);
        mvc.perform(get("/api/v1/organizations/{organizationId}/availability/slots", f.organizationId())
                        .header("Authorization", bearer(platformToken)).param("branchId", f.branchId().toString())
                        .param("serviceId", f.serviceId().toString()).param("date", MONDAY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[0].start").value("2026-08-17T09:15:00-06:00"))
                .andExpect(jsonPath("$.slots[0].end").value("2026-08-17T10:00:00-06:00"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[?(@.name == 'Customers')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Appointments')]").exists())
                .andExpect(jsonPath("$.paths['/api/v1/organizations/{organizationId}/availability/slots'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/organizations/{organizationId}/appointments/{appointmentId}/confirm'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/organizations/{organizationId}/appointments/{appointmentId}/no-show'].post").exists());
    }

    private List<Integer> concurrentBookings(Fixture f, int count, String start) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count); CountDownLatch go = new CountDownLatch(1);
        List<Callable<Integer>> calls = new ArrayList<>();
        for (int i=0;i<count;i++) calls.add(() -> {
            ready.countDown(); if (!go.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
            return mvc.perform(post("/api/v1/organizations/{organizationId}/appointments", f.organizationId())
                            .header("Authorization", bearer(platformToken)).contentType(MediaType.APPLICATION_JSON)
                            .content(bookingJson(f,start))).andReturn().getResponse().getStatus();
        });
        List<Future<Integer>> futures = calls.stream().map(executor::submit).toList();
        assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); go.countDown();
        List<Integer> results = new ArrayList<>(); for (Future<Integer> future:futures) results.add(future.get(30,TimeUnit.SECONDS));
        executor.shutdown(); assertThat(executor.awaitTermination(10,TimeUnit.SECONDS)).isTrue(); return results;
    }

    private long createAppointment(Fixture f,String start,String token,int expected) throws Exception {
        var result=mvc.perform(post("/api/v1/organizations/{organizationId}/appointments",f.organizationId())
                        .header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON).content(bookingJson(f,start)))
                .andExpect(status().is(expected)).andReturn();
        return expected==201 ? json.readTree(result.getResponse().getContentAsString()).get("id").asLong() : -1;
    }
    private String bookingJson(Fixture f,String start) {
        return """
                {"customerId":%d,"branchId":%d,"serviceId":%d,"specialistId":%d,"startsAt":"%s"}
                """.formatted(f.customerId(),f.branchId(),f.serviceId(),f.specialistId(),start);
    }
    private void assertSlot(Fixture f,String time,boolean expected) throws Exception {
        String body=mvc.perform(get("/api/v1/organizations/{organizationId}/availability/slots",f.organizationId())
                        .header("Authorization",bearer(platformToken)).param("branchId",f.branchId().toString())
                        .param("serviceId",f.serviceId().toString()).param("date",MONDAY.toString())
                        .param("specialistId",f.specialistId().toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        boolean present=false; for(JsonNode slot:json.readTree(body).get("slots"))
            if(slot.get("start").asText().contains("T"+time)) present=true;
        assertThat(present).isEqualTo(expected);
    }
    private long activeAppointments(Fixture f) {
        return jdbc.queryForObject("SELECT count(*) FROM agendaflow.appointments WHERE organization_id=? AND status NOT IN ('CANCELLED','RESCHEDULED')",
                Long.class,f.organizationId());
    }
    private Fixture fixture(int capacity,int duration,int preparation,int cleanup,Integer customDuration) {
        Long organization=createOrganization("Booking "+USERS.incrementAndGet());
        Long branch=jdbc.queryForObject("INSERT INTO agendaflow.branches (organization_id,name,timezone) VALUES (?,'Central','America/El_Salvador') RETURNING id",Long.class,organization);
        Long service=jdbc.queryForObject("INSERT INTO agendaflow.services (organization_id,name,duration_minutes,preparation_minutes,cleanup_minutes) VALUES (?,'Consultation',?,?,?) RETURNING id",Long.class,organization,duration,preparation,cleanup);
        jdbc.update("INSERT INTO agendaflow.branch_services (branch_id,service_id) VALUES (?,?)",branch,service);
        Long specialist=jdbc.queryForObject("INSERT INTO agendaflow.specialists (organization_id,professional_name,simultaneous_capacity) VALUES (?,'Taylor',?) RETURNING id",Long.class,organization,capacity);
        jdbc.update("INSERT INTO agendaflow.specialist_branches (specialist_id,branch_id) VALUES (?,?)",specialist,branch);
        jdbc.update("INSERT INTO agendaflow.specialist_services (specialist_id,service_id,custom_duration_minutes) VALUES (?,?,?)",specialist,service,customDuration);
        jdbc.update("INSERT INTO agendaflow.availability_schedules (organization_id,specialist_id,branch_id,day_of_week,start_time,end_time) VALUES (?,?,?,1,'09:00','12:00')",organization,specialist,branch);
        Long customer=jdbc.queryForObject("INSERT INTO agendaflow.customers (organization_id,first_name,last_name) VALUES (?,'Grace','Hopper') RETURNING id",Long.class,organization);
        return new Fixture(organization,branch,service,specialist,customer);
    }
    private Long createOrganization(String name) {
        return jdbc.queryForObject("INSERT INTO agendaflow.organizations (legal_name,timezone) VALUES (?,'America/El_Salvador') RETURNING id",Long.class,name);
    }
    private String createSession(Long organizationId,String role) {
        String email="booking-"+USERS.incrementAndGet()+"@test.local"; String password="booking-test-password";
        Long user=jdbc.queryForObject("INSERT INTO agendaflow.app_users (email,password_hash,first_name,last_name) VALUES (?,?,'Booking','Tester') RETURNING id",Long.class,email,passwords.encode(password));
        Long membership=jdbc.queryForObject("INSERT INTO agendaflow.organization_memberships (organization_id,user_id) VALUES (?,?) RETURNING id",Long.class,organizationId,user);
        jdbc.update("INSERT INTO agendaflow.membership_roles (membership_id,role_id) SELECT ?,id FROM agendaflow.roles WHERE organization_id IS NULL AND name=?",membership,role);
        try {
            String body=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"%s\",\"password\":\"%s\",\"organizationId\":%d}".formatted(email,password,organizationId)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            return json.readTree(body).get("accessToken").asText();
        } catch(Exception exception) { throw new IllegalStateException(exception); }
    }
    private String bearer(String token) { return "Bearer "+token; }
    private void lifecycle(Fixture fixture, long appointmentId, String action, String expectedStatus) throws Exception {
        mvc.perform(post("/api/v1/organizations/{organizationId}/appointments/{id}/" + action,
                        fixture.organizationId(), appointmentId).header("Authorization", bearer(platformToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(expectedStatus));
    }
    private record Fixture(Long organizationId,Long branchId,Long serviceId,Long specialistId,Long customerId) {}
}
