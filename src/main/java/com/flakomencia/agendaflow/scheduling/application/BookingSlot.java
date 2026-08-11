package com.flakomencia.agendaflow.scheduling.application;

import java.time.OffsetDateTime;

import com.flakomencia.agendaflow.branch.domain.Branch;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.servicecatalog.domain.CatalogService;
import com.flakomencia.agendaflow.specialist.domain.Specialist;

public record BookingSlot(
        Organization organization, Branch branch, CatalogService service, Specialist specialist,
        OffsetDateTime startsAt, OffsetDateTime endsAt, int preparationMinutes, int cleanupMinutes) {
}
