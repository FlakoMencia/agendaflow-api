package com.flakomencia.agendaflow.servicecatalog.application;

import java.util.List;
import java.util.Locale;
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
import com.flakomencia.agendaflow.organization.application.OrganizationNotFoundException;
import com.flakomencia.agendaflow.organization.domain.Organization;
import com.flakomencia.agendaflow.organization.domain.OrganizationStatus;
import com.flakomencia.agendaflow.organization.infrastructure.OrganizationRepository;
import com.flakomencia.agendaflow.servicecatalog.api.BranchServiceAssignmentRequest;
import com.flakomencia.agendaflow.servicecatalog.api.BranchServiceResponse;
import com.flakomencia.agendaflow.servicecatalog.api.CatalogServiceCreateRequest;
import com.flakomencia.agendaflow.servicecatalog.api.CatalogServiceResponse;
import com.flakomencia.agendaflow.servicecatalog.api.CatalogServiceUpdateRequest;
import com.flakomencia.agendaflow.servicecatalog.api.ServiceCategoryCreateRequest;
import com.flakomencia.agendaflow.servicecatalog.api.ServiceCategoryResponse;
import com.flakomencia.agendaflow.servicecatalog.api.ServiceCategoryUpdateRequest;
import com.flakomencia.agendaflow.servicecatalog.domain.BranchServiceAssignment;
import com.flakomencia.agendaflow.servicecatalog.domain.BranchServiceId;
import com.flakomencia.agendaflow.servicecatalog.domain.CatalogService;
import com.flakomencia.agendaflow.servicecatalog.domain.ServiceCategory;
import com.flakomencia.agendaflow.servicecatalog.infrastructure.BranchServiceAssignmentRepository;
import com.flakomencia.agendaflow.servicecatalog.infrastructure.CatalogServiceRepository;
import com.flakomencia.agendaflow.servicecatalog.infrastructure.ServiceCategoryRepository;

import jakarta.persistence.EntityManager;

@Service
public class ServiceCatalogApplicationService {

    private static final Set<String> SERVICE_SORTS = Set.of(
            "id", "name", "durationMinutes", "price", "active", "createdAt", "updatedAt");

    private final ServiceCategoryRepository categories;
    private final CatalogServiceRepository services;
    private final BranchServiceAssignmentRepository branchServices;
    private final OrganizationRepository organizations;
    private final BranchRepository branches;
    private final TenantAccessGuard tenantAccess;
    private final EntityManager entityManager;

    public ServiceCatalogApplicationService(
            ServiceCategoryRepository categories,
            CatalogServiceRepository services,
            BranchServiceAssignmentRepository branchServices,
            OrganizationRepository organizations,
            BranchRepository branches,
            TenantAccessGuard tenantAccess,
            EntityManager entityManager) {
        this.categories = categories;
        this.services = services;
        this.branchServices = branchServices;
        this.organizations = organizations;
        this.branches = branches;
        this.tenantAccess = tenantAccess;
        this.entityManager = entityManager;
    }

    @Transactional
    public ServiceCategoryResponse createCategory(Long organizationId, ServiceCategoryCreateRequest request) {
        Organization organization = requireOrganization(organizationId);
        String name = requiredText(request.name());
        validateCategoryName(organizationId, name, null);
        ServiceCategory category = new ServiceCategory(organization, name);
        category.setDescription(optionalText(request.description()));
        category.setActive(request.active() == null ? true : request.active());
        categories.save(category);
        refresh(category);
        return categoryResponse(category);
    }

    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> listCategories(Long organizationId) {
        requireOrganization(organizationId);
        return categories.findAllByOrganization_IdOrderByNameAsc(organizationId).stream()
                .map(this::categoryResponse).toList();
    }

    @Transactional(readOnly = true)
    public ServiceCategoryResponse getCategory(Long organizationId, Long categoryId) {
        requireOrganization(organizationId);
        return categoryResponse(requireCategory(organizationId, categoryId));
    }

    @Transactional
    public ServiceCategoryResponse updateCategory(
            Long organizationId, Long categoryId, ServiceCategoryUpdateRequest request) {
        requireOrganization(organizationId);
        ServiceCategory category = requireCategory(organizationId, categoryId);
        String name = requiredText(request.name());
        validateCategoryName(organizationId, name, categoryId);
        category.setName(name);
        category.setDescription(optionalText(request.description()));
        if (request.active() != null) category.setActive(request.active());
        entityManager.flush();
        entityManager.refresh(category);
        return categoryResponse(category);
    }

    @Transactional
    public CatalogServiceResponse createService(Long organizationId, CatalogServiceCreateRequest request) {
        Organization organization = requireOrganization(organizationId);
        String name = requiredText(request.name());
        validateServiceName(organizationId, name, null);
        CatalogService service = new CatalogService(organization, name, request.durationMinutes());
        applyServiceValues(service, organizationId, request.categoryId(), request.description(),
                request.preparationMinutes(), request.cleanupMinutes(), request.price(), request.currencyCode(),
                request.requiresApproval(), request.allowsOnlineBooking(), request.active(), request.colorCode(), true);
        services.save(service);
        refresh(service);
        return serviceResponse(service);
    }

    @Transactional(readOnly = true)
    public PageResponse<CatalogServiceResponse> listServices(Long organizationId, Pageable pageable) {
        requireOrganization(organizationId);
        validateSort(pageable);
        return PageResponse.from(
                services.findAllByOrganization_IdAndDeletedAtIsNull(organizationId, pageable),
                this::serviceResponse);
    }

    @Transactional(readOnly = true)
    public CatalogServiceResponse getService(Long organizationId, Long serviceId) {
        requireOrganization(organizationId);
        return serviceResponse(requireService(organizationId, serviceId));
    }

    @Transactional
    public CatalogServiceResponse updateService(
            Long organizationId, Long serviceId, CatalogServiceUpdateRequest request) {
        requireOrganization(organizationId);
        CatalogService service = requireService(organizationId, serviceId);
        String name = requiredText(request.name());
        validateServiceName(organizationId, name, serviceId);
        service.setName(name);
        service.setDurationMinutes(request.durationMinutes());
        applyServiceValues(service, organizationId, request.categoryId(), request.description(),
                request.preparationMinutes(), request.cleanupMinutes(), request.price(), request.currencyCode(),
                request.requiresApproval(), request.allowsOnlineBooking(), request.active(), request.colorCode(), false);
        entityManager.flush();
        entityManager.refresh(service);
        return serviceResponse(service);
    }

    @Transactional(readOnly = true)
    public List<BranchServiceResponse> listBranchServices(Long organizationId, Long branchId) {
        requireOrganization(organizationId);
        requireBranch(organizationId, branchId);
        return branchServices.findAllByBranch_IdAndBranch_Organization_IdOrderByService_NameAsc(branchId, organizationId)
                .stream().map(this::branchServiceResponse).toList();
    }

    @Transactional
    public BranchServiceResponse assignBranchService(
            Long organizationId, Long branchId, Long serviceId, BranchServiceAssignmentRequest request) {
        requireOrganization(organizationId);
        Branch branch = requireBranch(organizationId, branchId);
        CatalogService service = requireService(organizationId, serviceId);
        BranchServiceId id = new BranchServiceId(branchId, serviceId);
        var existing = branchServices.findByIdAndBranch_Organization_Id(id, organizationId);
        BranchServiceAssignment assignment = existing.orElseGet(() -> new BranchServiceAssignment(branch, service));
        if (request.active() != null) assignment.setActive(request.active());
        else if (assignment.getActive() == null) assignment.setActive(true);
        if (existing.isEmpty()) entityManager.persist(assignment);
        refresh(assignment);
        return branchServiceResponse(assignment);
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

    private Branch requireBranch(Long organizationId, Long branchId) {
        return branches.findByIdAndOrganization_IdAndDeletedAtIsNull(branchId, organizationId)
                .orElseThrow(() -> new BranchNotFoundException(organizationId, branchId));
    }

    private ServiceCategory requireCategory(Long organizationId, Long categoryId) {
        return categories.findByIdAndOrganization_Id(categoryId, organizationId)
                .orElseThrow(() -> new ServiceCategoryNotFoundException(organizationId, categoryId));
    }

    private CatalogService requireService(Long organizationId, Long serviceId) {
        return services.findByIdAndOrganization_IdAndDeletedAtIsNull(serviceId, organizationId)
                .orElseThrow(() -> new CatalogServiceNotFoundException(organizationId, serviceId));
    }

    private void applyServiceValues(
            CatalogService service, Long organizationId, Long categoryId, String description,
            Integer preparationMinutes, Integer cleanupMinutes, java.math.BigDecimal price, String currencyCode,
            Boolean requiresApproval, Boolean allowsOnlineBooking, Boolean active, String colorCode, boolean creating) {
        service.setCategory(categoryId == null ? null : requireCategory(organizationId, categoryId));
        service.setDescription(optionalText(description));
        if (creating || preparationMinutes != null) service.setPreparationMinutes(valueOr(preparationMinutes, 0));
        if (creating || cleanupMinutes != null) service.setCleanupMinutes(valueOr(cleanupMinutes, 0));
        service.setPrice(price);
        if (creating || currencyCode != null) service.setCurrencyCode(currencyCode == null ? "USD" : currencyCode.trim().toUpperCase(Locale.ROOT));
        if (creating || requiresApproval != null) service.setRequiresApproval(Boolean.TRUE.equals(requiresApproval));
        if (creating || allowsOnlineBooking != null) service.setAllowsOnlineBooking(allowsOnlineBooking == null || allowsOnlineBooking);
        if (creating || active != null) service.setActive(active == null || active);
        service.setColorCode(optionalText(colorCode));
    }

    private int valueOr(Integer value, int fallback) { return value == null ? fallback : value; }

    private void validateCategoryName(Long organizationId, String name, Long currentId) {
        boolean duplicate = currentId == null
                ? categories.existsByOrganization_IdAndName(organizationId, name)
                : categories.existsByOrganization_IdAndNameAndIdNot(organizationId, name, currentId);
        if (duplicate) throw new ConflictException("SERVICE_CATEGORY_NAME_EXISTS", "Service category name already exists");
    }

    private void validateServiceName(Long organizationId, String name, Long currentId) {
        boolean duplicate = currentId == null
                ? services.existsByOrganization_IdAndNameAndDeletedAtIsNull(organizationId, name)
                : services.existsByOrganization_IdAndNameAndDeletedAtIsNullAndIdNot(organizationId, name, currentId);
        if (duplicate) throw new ConflictException("SERVICE_NAME_EXISTS", "Service name already exists");
    }

    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(order -> !SERVICE_SORTS.contains(order.getProperty()))) {
            throw new InvalidRequestException("Unsupported service sort property");
        }
    }

    private ServiceCategoryResponse categoryResponse(ServiceCategory category) {
        return new ServiceCategoryResponse(category.getId(), category.getOrganization().getId(), category.getName(),
                category.getDescription(), category.getActive(), category.getCreatedAt(), category.getUpdatedAt());
    }

    private CatalogServiceResponse serviceResponse(CatalogService service) {
        return new CatalogServiceResponse(service.getId(), service.getOrganization().getId(),
                service.getCategory() == null ? null : service.getCategory().getId(), service.getName(),
                service.getDescription(), service.getDurationMinutes(), service.getPreparationMinutes(),
                service.getCleanupMinutes(), service.getPrice(), service.getCurrencyCode(),
                service.getRequiresApproval(), service.getAllowsOnlineBooking(), service.getActive(),
                service.getColorCode(), service.getCreatedAt(), service.getUpdatedAt());
    }

    private BranchServiceResponse branchServiceResponse(BranchServiceAssignment assignment) {
        return new BranchServiceResponse(assignment.getBranch().getId(), assignment.getService().getId(),
                assignment.getService().getName(), assignment.getActive(), assignment.getCreatedAt());
    }

    private void refresh(Object entity) { entityManager.flush(); entityManager.refresh(entity); }
    private String requiredText(String value) { return value.trim(); }
    private String optionalText(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
