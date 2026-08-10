package com.flakomencia.agendaflow.specialist.application;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flakomencia.agendaflow.branch.application.BranchNotFoundException;
import com.flakomencia.agendaflow.branch.domain.Branch;
import com.flakomencia.agendaflow.branch.infrastructure.BranchRepository;
import com.flakomencia.agendaflow.common.api.PageResponse;
import com.flakomencia.agendaflow.common.exception.ConflictException;
import com.flakomencia.agendaflow.common.exception.InvalidRequestException;
import com.flakomencia.agendaflow.common.security.TenantAccessGuard;
import com.flakomencia.agendaflow.identity.domain.AppUser;
import com.flakomencia.agendaflow.identity.domain.MembershipStatus;
import com.flakomencia.agendaflow.identity.infrastructure.AppUserRepository;
import com.flakomencia.agendaflow.identity.infrastructure.OrganizationMembershipRepository;
import com.flakomencia.agendaflow.organization.application.OrganizationNotFoundException;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.domain.OrganizationStatus;
import com.flakomencia.agendaflow.organization.infrastructure.OrganizationRepository;
import com.flakomencia.agendaflow.servicecatalog.application.CatalogServiceNotFoundException;
import com.flakomencia.agendaflow.servicecatalog.domain.CatalogService;
import com.flakomencia.agendaflow.servicecatalog.infrastructure.CatalogServiceRepository;
import com.flakomencia.agendaflow.specialist.api.SpecialistBranchAssignmentRequest;
import com.flakomencia.agendaflow.specialist.api.SpecialistBranchResponse;
import com.flakomencia.agendaflow.specialist.api.SpecialistCreateRequest;
import com.flakomencia.agendaflow.specialist.api.SpecialistResponse;
import com.flakomencia.agendaflow.specialist.api.SpecialistServiceAssignmentRequest;
import com.flakomencia.agendaflow.specialist.api.SpecialistServiceResponse;
import com.flakomencia.agendaflow.specialist.api.SpecialistUpdateRequest;
import com.flakomencia.agendaflow.specialist.domain.Specialist;
import com.flakomencia.agendaflow.specialist.domain.SpecialistBranchAssignment;
import com.flakomencia.agendaflow.specialist.domain.SpecialistBranchId;
import com.flakomencia.agendaflow.specialist.domain.SpecialistServiceAssignment;
import com.flakomencia.agendaflow.specialist.domain.SpecialistServiceId;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistBranchAssignmentRepository;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistRepository;
import com.flakomencia.agendaflow.specialist.infrastructure.SpecialistServiceAssignmentRepository;

import jakarta.persistence.EntityManager;

@Service
public class SpecialistApplicationService {

    private static final Set<String> ALLOWED_SORTS = Set.of(
            "id", "professionalName", "specialtyName", "simultaneousCapacity", "active", "createdAt", "updatedAt");

    private final SpecialistRepository specialists;
    private final SpecialistBranchAssignmentRepository specialistBranches;
    private final SpecialistServiceAssignmentRepository specialistServices;
    private final OrganizationRepository organizations;
    private final BranchRepository branches;
    private final CatalogServiceRepository services;
    private final AppUserRepository users;
    private final OrganizationMembershipRepository memberships;
    private final TenantAccessGuard tenantAccess;
    private final EntityManager entityManager;

    public SpecialistApplicationService(
            SpecialistRepository specialists,
            SpecialistBranchAssignmentRepository specialistBranches,
            SpecialistServiceAssignmentRepository specialistServices,
            OrganizationRepository organizations,
            BranchRepository branches,
            CatalogServiceRepository services,
            AppUserRepository users,
            OrganizationMembershipRepository memberships,
            TenantAccessGuard tenantAccess,
            EntityManager entityManager) {
        this.specialists = specialists;
        this.specialistBranches = specialistBranches;
        this.specialistServices = specialistServices;
        this.organizations = organizations;
        this.branches = branches;
        this.services = services;
        this.users = users;
        this.memberships = memberships;
        this.tenantAccess = tenantAccess;
        this.entityManager = entityManager;
    }

    @Transactional
    public SpecialistResponse create(Long organizationId, SpecialistCreateRequest request) {
        Organization organization = requireOrganization(organizationId);
        validateUserDuplicate(organizationId, request.userId(), null);
        Specialist specialist = new Specialist(organization, requiredText(request.professionalName()));
        apply(specialist, organizationId, request.userId(), request.specialtyName(), request.biography(),
                request.licenseNumber(), request.photoUrl(), request.phone(), request.email(),
                request.simultaneousCapacity(), request.active(), true);
        specialists.save(specialist);
        refresh(specialist);
        return response(specialist);
    }

    @Transactional(readOnly = true)
    public PageResponse<SpecialistResponse> list(Long organizationId, Pageable pageable) {
        requireOrganization(organizationId);
        validateSort(pageable);
        return PageResponse.from(specialists.findAllByOrganization_IdAndDeletedAtIsNull(organizationId, pageable),
                this::response);
    }

    @Transactional(readOnly = true)
    public SpecialistResponse get(Long organizationId, Long specialistId) {
        requireOrganization(organizationId);
        return response(requireSpecialist(organizationId, specialistId));
    }

    @Transactional
    public SpecialistResponse update(Long organizationId, Long specialistId, SpecialistUpdateRequest request) {
        requireOrganization(organizationId);
        Specialist specialist = requireSpecialist(organizationId, specialistId);
        validateUserDuplicate(organizationId, request.userId(), specialistId);
        specialist.setProfessionalName(requiredText(request.professionalName()));
        apply(specialist, organizationId, request.userId(), request.specialtyName(), request.biography(),
                request.licenseNumber(), request.photoUrl(), request.phone(), request.email(),
                request.simultaneousCapacity(), request.active(), false);
        refresh(specialist);
        return response(specialist);
    }

    @Transactional(readOnly = true)
    public List<SpecialistBranchResponse> listBranches(Long organizationId, Long specialistId) {
        requireOrganization(organizationId);
        requireSpecialist(organizationId, specialistId);
        return specialistBranches
                .findAllBySpecialist_IdAndSpecialist_Organization_IdOrderByBranch_NameAsc(specialistId, organizationId)
                .stream().map(this::branchResponse).toList();
    }

    @Transactional
    public SpecialistBranchResponse assignBranch(
            Long organizationId, Long specialistId, Long branchId, SpecialistBranchAssignmentRequest request) {
        requireOrganization(organizationId);
        Specialist specialist = requireSpecialist(organizationId, specialistId);
        Branch branch = requireBranch(organizationId, branchId);
        SpecialistBranchId id = new SpecialistBranchId(specialistId, branchId);
        var existing = specialistBranches.findByIdAndSpecialist_Organization_Id(id, organizationId);
        SpecialistBranchAssignment assignment = existing
                .orElseGet(() -> new SpecialistBranchAssignment(specialist, branch));
        if (request.primary() != null) assignment.setPrimary(request.primary());
        else if (assignment.getPrimary() == null) assignment.setPrimary(false);
        if (request.active() != null) assignment.setActive(request.active());
        else if (assignment.getActive() == null) assignment.setActive(true);
        if (existing.isEmpty()) entityManager.persist(assignment);
        refresh(assignment);
        return branchResponse(assignment);
    }

    @Transactional(readOnly = true)
    public List<SpecialistServiceResponse> listServices(Long organizationId, Long specialistId) {
        requireOrganization(organizationId);
        requireSpecialist(organizationId, specialistId);
        return specialistServices
                .findAllBySpecialist_IdAndSpecialist_Organization_IdOrderByService_NameAsc(specialistId, organizationId)
                .stream().map(this::serviceResponse).toList();
    }

    @Transactional
    public SpecialistServiceResponse assignService(
            Long organizationId, Long specialistId, Long serviceId, SpecialistServiceAssignmentRequest request) {
        requireOrganization(organizationId);
        Specialist specialist = requireSpecialist(organizationId, specialistId);
        CatalogService service = requireService(organizationId, serviceId);
        SpecialistServiceId id = new SpecialistServiceId(specialistId, serviceId);
        var existing = specialistServices.findByIdAndSpecialist_Organization_Id(id, organizationId);
        SpecialistServiceAssignment assignment = existing
                .orElseGet(() -> new SpecialistServiceAssignment(specialist, service));
        assignment.setCustomDurationMinutes(request.customDurationMinutes());
        assignment.setCustomPrice(request.customPrice());
        if (request.active() != null) assignment.setActive(request.active());
        else if (assignment.getActive() == null) assignment.setActive(true);
        if (existing.isEmpty()) entityManager.persist(assignment);
        refresh(assignment);
        return serviceResponse(assignment);
    }

    private void apply(
            Specialist specialist, Long organizationId, Long userId, String specialtyName, String biography,
            String licenseNumber, String photoUrl, String phone, String email, Integer capacity,
            Boolean active, boolean creating) {
        specialist.setUser(userId == null ? null : requireOrganizationUser(organizationId, userId));
        specialist.setSpecialtyName(optionalText(specialtyName));
        specialist.setBiography(optionalText(biography));
        specialist.setLicenseNumber(optionalText(licenseNumber));
        specialist.setPhotoUrl(optionalText(photoUrl));
        specialist.setPhone(optionalText(phone));
        specialist.setEmail(optionalText(email));
        if (creating || capacity != null) specialist.setSimultaneousCapacity(capacity == null ? 1 : capacity);
        if (creating || active != null) specialist.setActive(active == null || active);
    }

    private Organization requireOrganization(Long organizationId) {
        tenantAccess.requireTenant(organizationId);
        if (!tenantAccess.current().isPlatformAdministrator()) {
            return organizations.findByIdAndStatusAndDeletedAtIsNull(organizationId, OrganizationStatus.ACTIVE)
                    .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        }
        return organizations.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    private Specialist requireSpecialist(Long organizationId, Long specialistId) {
        return specialists.findByIdAndOrganization_IdAndDeletedAtIsNull(specialistId, organizationId)
                .orElseThrow(() -> new SpecialistNotFoundException(organizationId, specialistId));
    }

    private Branch requireBranch(Long organizationId, Long branchId) {
        return branches.findByIdAndOrganization_IdAndDeletedAtIsNull(branchId, organizationId)
                .orElseThrow(() -> new BranchNotFoundException(organizationId, branchId));
    }

    private CatalogService requireService(Long organizationId, Long serviceId) {
        return services.findByIdAndOrganization_IdAndDeletedAtIsNull(serviceId, organizationId)
                .orElseThrow(() -> new CatalogServiceNotFoundException(organizationId, serviceId));
    }

    private AppUser requireOrganizationUser(Long organizationId, Long userId) {
        boolean member = memberships.existsByOrganization_IdAndUser_IdAndStatus(
                organizationId, userId, MembershipStatus.ACTIVE);
        if (!member) {
            throw new SpecialistUserNotFoundException();
        }
        return users.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(SpecialistUserNotFoundException::new);
    }

    private void validateUserDuplicate(Long organizationId, Long userId, Long currentId) {
        if (userId == null) return;
        boolean duplicate = currentId == null
                ? specialists.existsByOrganization_IdAndUser_IdAndDeletedAtIsNull(organizationId, userId)
                : specialists.existsByOrganization_IdAndUser_IdAndDeletedAtIsNullAndIdNot(
                        organizationId, userId, currentId);
        if (duplicate) throw new ConflictException("SPECIALIST_USER_EXISTS", "User is already linked to a specialist");
    }

    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(order -> !ALLOWED_SORTS.contains(order.getProperty()))) {
            throw new InvalidRequestException("Unsupported specialist sort property");
        }
    }

    private SpecialistResponse response(Specialist specialist) {
        return new SpecialistResponse(specialist.getId(), specialist.getOrganization().getId(),
                specialist.getUser() == null ? null : specialist.getUser().getId(), specialist.getProfessionalName(),
                specialist.getSpecialtyName(), specialist.getBiography(), specialist.getLicenseNumber(),
                specialist.getPhotoUrl(), specialist.getPhone(), specialist.getEmail(),
                specialist.getSimultaneousCapacity(), specialist.getActive(), specialist.getCreatedAt(),
                specialist.getUpdatedAt());
    }

    private SpecialistBranchResponse branchResponse(SpecialistBranchAssignment assignment) {
        return new SpecialistBranchResponse(assignment.getSpecialist().getId(), assignment.getBranch().getId(),
                assignment.getBranch().getName(), assignment.getPrimary(), assignment.getActive(), assignment.getCreatedAt());
    }

    private SpecialistServiceResponse serviceResponse(SpecialistServiceAssignment assignment) {
        return new SpecialistServiceResponse(assignment.getSpecialist().getId(), assignment.getService().getId(),
                assignment.getService().getName(), assignment.getCustomDurationMinutes(), assignment.getCustomPrice(),
                assignment.getActive(), assignment.getCreatedAt());
    }

    private void refresh(Object entity) { entityManager.flush(); entityManager.refresh(entity); }
    private String requiredText(String value) { return value.trim(); }
    private String optionalText(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
