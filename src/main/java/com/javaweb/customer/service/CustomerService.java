package com.javaweb.customer.service;

import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.DuplicateResourceException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.common.response.PageResponse;
import com.javaweb.customer.dto.CustomerDetailResponse;
import com.javaweb.customer.dto.CustomerNoteRequest;
import com.javaweb.customer.dto.CustomerNoteResponse;
import com.javaweb.customer.dto.CustomerRequirementRequest;
import com.javaweb.customer.dto.CustomerRequirementResponse;
import com.javaweb.customer.dto.CustomerResponse;
import com.javaweb.customer.dto.CustomerSearchRequest;
import com.javaweb.customer.dto.CustomerTimelineItemResponse;
import com.javaweb.customer.dto.CustomerUpsertRequest;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.entity.CustomerNote;
import com.javaweb.customer.entity.CustomerRequirement;
import com.javaweb.customer.enums.CustomerStatus;
import com.javaweb.customer.mapper.CustomerMapper;
import com.javaweb.customer.repository.CustomerNoteRepository;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.customer.repository.CustomerRequirementRepository;
import com.javaweb.customer.repository.CustomerSpecifications;
import com.javaweb.property.entity.District;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.entity.Ward;
import com.javaweb.property.repository.DistrictRepository;
import com.javaweb.property.repository.PropertyTypeRepository;
import com.javaweb.property.repository.ProvinceRepository;
import com.javaweb.property.repository.WardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class CustomerService {
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "code", "code",
            "fullName", "fullName",
            "status", "status",
            "priority", "priority",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    private final CustomerRepository customerRepository;
    private final CustomerNoteRepository noteRepository;
    private final CustomerRequirementRepository requirementRepository;
    private final UserRepository userRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(
            CustomerRepository customerRepository,
            CustomerNoteRepository noteRepository,
            CustomerRequirementRepository requirementRepository,
            UserRepository userRepository,
            PropertyTypeRepository propertyTypeRepository,
            ProvinceRepository provinceRepository,
            DistrictRepository districtRepository,
            WardRepository wardRepository,
            CustomerMapper customerMapper
    ) {
        this.customerRepository = customerRepository;
        this.noteRepository = noteRepository;
        this.requirementRepository = requirementRepository;
        this.userRepository = userRepository;
        this.propertyTypeRepository = propertyTypeRepository;
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
        this.wardRepository = wardRepository;
        this.customerMapper = customerMapper;
    }

    @Transactional
    public CustomerResponse create(
            CustomerUpsertRequest request,
            AuthUserPrincipal actor
    ) {
        if (customerRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Customer code already exists");
        }
        User creator = requireUser(actor.id(), "Authenticated user not found");
        User linkedUser = resolveLinkedCustomer(request.userId(), actor, null);
        User assignedAgent = resolveAssignedAgent(request.assignedAgentId(), actor, null);

        Customer customer = customerMapper.toEntity(request, creator);
        customer.setUser(linkedUser);
        customer.setAssignedAgent(assignedAgent);
        return customerMapper.toResponse(customerRepository.saveAndFlush(customer));
    }

    @Transactional
    public CustomerResponse update(
            Long customerId,
            CustomerUpsertRequest request,
            AuthUserPrincipal actor
    ) {
        Customer customer = requireAccessibleCustomer(customerId, actor);
        if (customerRepository.existsByCodeAndIdNot(request.code(), customerId)) {
            throw new DuplicateResourceException("Customer code already exists");
        }

        customerMapper.updateEntity(customer, request);
        if (isManagerOrAdmin(actor)) {
            customer.setUser(resolveLinkedCustomer(request.userId(), actor, customerId));
            customer.setAssignedAgent(resolveAssignedAgent(
                    request.assignedAgentId(),
                    actor,
                    customer
            ));
        }
        return customerMapper.toResponse(customerRepository.saveAndFlush(customer));
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> search(
            CustomerSearchRequest request,
            AuthUserPrincipal actor
    ) {
        String sortField = SORT_FIELDS.get(request.sortBy());
        if (sortField == null) {
            throw new BusinessException("Unsupported customer sort field");
        }
        Long visibleUserId = isManagerOrAdmin(actor) ? null : actor.id();
        Page<Customer> page = customerRepository.findAll(
                CustomerSpecifications.search(request, visibleUserId),
                PageRequest.of(
                        request.page(),
                        request.size(),
                        Sort.by(request.direction(), sortField)
                                .and(Sort.by(Sort.Direction.DESC, "id"))
                )
        );
        return PageResponse.from(
                page,
                page.getContent().stream().map(customerMapper::toResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public CustomerDetailResponse get(Long customerId, AuthUserPrincipal actor) {
        Customer customer = requireAccessibleCustomer(customerId, actor);
        List<CustomerRequirementResponse> requirements = requirementRepository
                .findAllByCustomerIdAndActiveTrueOrderByCreatedAtDesc(customerId)
                .stream()
                .map(customerMapper::toRequirementResponse)
                .toList();
        List<CustomerNoteResponse> notes = noteRepository
                .findAllByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(customerMapper::toNoteResponse)
                .toList();
        return new CustomerDetailResponse(
                customerMapper.toResponse(customer),
                requirements,
                notes
        );
    }

    @Transactional
    public void delete(Long customerId, AuthUserPrincipal actor) {
        Customer customer = requireAccessibleCustomer(customerId, actor);
        customer.setStatus(CustomerStatus.ARCHIVED);
        customer.setDeletedAt(Instant.now());
        customerRepository.save(customer);
    }

    @Transactional
    public CustomerNoteResponse addNote(
            Long customerId,
            CustomerNoteRequest request,
            AuthUserPrincipal actor
    ) {
        Customer customer = requireAccessibleCustomer(customerId, actor);
        User author = requireUser(actor.id(), "Authenticated user not found");
        CustomerNote note = new CustomerNote(author, request.content());
        note.setPinned(request.pinned());
        customer.addNote(note);
        return customerMapper.toNoteResponse(noteRepository.saveAndFlush(note));
    }

    @Transactional
    public CustomerRequirementResponse addRequirement(
            Long customerId,
            CustomerRequirementRequest request,
            AuthUserPrincipal actor
    ) {
        Customer customer = requireAccessibleCustomer(customerId, actor);
        Location location = requireLocation(
                request.provinceId(),
                request.districtId(),
                request.wardId()
        );
        PropertyType propertyType = request.propertyTypeId() == null
                ? null
                : propertyTypeRepository.findByIdAndActiveTrue(request.propertyTypeId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Active property type not found"
                        ));

        CustomerRequirement requirement = new CustomerRequirement(request.purpose());
        requirement.setPropertyType(propertyType);
        requirement.setProvince(location.province());
        requirement.setDistrict(location.district());
        requirement.setWard(location.ward());
        requirement.setMinBudget(request.minBudget());
        requirement.setMaxBudget(request.maxBudget());
        requirement.setCurrency(request.currency());
        requirement.setMinArea(request.minArea());
        requirement.setMaxArea(request.maxArea());
        requirement.setMinBedrooms(request.minBedrooms());
        requirement.setMinBathrooms(request.minBathrooms());
        requirement.setDescription(request.description());
        customer.addRequirement(requirement);
        return customerMapper.toRequirementResponse(
                requirementRepository.saveAndFlush(requirement)
        );
    }

    @Transactional(readOnly = true)
    public List<CustomerTimelineItemResponse> getTimeline(
            Long customerId,
            AuthUserPrincipal actor
    ) {
        Customer customer = requireAccessibleCustomer(customerId, actor);
        List<CustomerTimelineItemResponse> items = new java.util.ArrayList<>();
        items.add(new CustomerTimelineItemResponse(
                "CUSTOMER_CREATED",
                customer.getId(),
                "Customer profile created",
                customer.getFullName(),
                customer.getCreatedBy().getId(),
                customer.getCreatedBy().getFullName(),
                customer.getCreatedAt()
        ));
        noteRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)
                .forEach(note -> items.add(new CustomerTimelineItemResponse(
                        "NOTE_ADDED",
                        note.getId(),
                        note.isPinned() ? "Pinned note added" : "Note added",
                        note.getContent(),
                        note.getAuthor().getId(),
                        note.getAuthor().getFullName(),
                        note.getCreatedAt()
                )));
        requirementRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)
                .forEach(requirement -> items.add(new CustomerTimelineItemResponse(
                        "REQUIREMENT_ADDED",
                        requirement.getId(),
                        "Customer requirement added",
                        requirement.getPurpose().name(),
                        null,
                        null,
                        requirement.getCreatedAt()
                )));
        return items.stream()
                .sorted(Comparator.comparing(
                        CustomerTimelineItemResponse::occurredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    private Customer requireAccessibleCustomer(
            Long customerId,
            AuthUserPrincipal actor
    ) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (customer.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Customer not found");
        }
        if (!isManagerOrAdmin(actor)) {
            boolean createdByActor = customer.getCreatedBy().getId().equals(actor.id());
            boolean assignedToActor = customer.getAssignedAgent() != null
                    && customer.getAssignedAgent().getId().equals(actor.id());
            if (!createdByActor && !assignedToActor) {
                throw new AccessDeniedException(
                        "Agents can only access customers they created or are assigned"
                );
            }
        }
        return customer;
    }

    private User resolveLinkedCustomer(
            Long userId,
            AuthUserPrincipal actor,
            Long customerId
    ) {
        if (!isManagerOrAdmin(actor)) {
            if (userId != null) {
                throw new AccessDeniedException(
                        "Only managers or administrators can link customer accounts"
                );
            }
            return null;
        }
        if (userId == null) {
            return null;
        }
        boolean alreadyLinked = customerId == null
                ? customerRepository.existsByUserId(userId)
                : customerRepository.existsByUserIdAndIdNot(userId, customerId);
        if (alreadyLinked) {
            throw new DuplicateResourceException("User is already linked to a customer");
        }
        return requireUserWithRole(userId, RoleCode.CUSTOMER, "Customer user");
    }

    private User resolveAssignedAgent(
            Long assignedAgentId,
            AuthUserPrincipal actor,
            Customer existing
    ) {
        if (!isManagerOrAdmin(actor)) {
            if (existing != null) {
                return existing.getAssignedAgent();
            }
            return requireUserWithRole(actor.id(), RoleCode.AGENT, "Assigned agent");
        }
        return assignedAgentId == null
                ? null
                : requireUserWithRole(assignedAgentId, RoleCode.AGENT, "Assigned agent");
    }

    private User requireUserWithRole(Long userId, RoleCode roleCode, String label) {
        User user = requireUser(userId, label + " not found");
        boolean hasRole = user.getRoles().stream()
                .map(Role::getCode)
                .anyMatch(roleCode::equals);
        if (!hasRole) {
            throw new BusinessException(label + " must have the " + roleCode + " role");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(label + " must be active");
        }
        return user;
    }

    private User requireUser(Long userId, String message) {
        return userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(message));
    }

    private Location requireLocation(Long provinceId, Long districtId, Long wardId) {
        if (provinceId == null && (districtId != null || wardId != null)) {
            throw new BusinessException("provinceId is required for district or ward");
        }
        if (districtId == null && wardId != null) {
            throw new BusinessException("districtId is required for ward");
        }
        Province province = provinceId == null
                ? null
                : provinceRepository.findByIdAndActiveTrue(provinceId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Active province not found"
                        ));
        District district = districtId == null
                ? null
                : districtRepository.findByIdAndActiveTrue(districtId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Active district not found"
                        ));
        Ward ward = wardId == null
                ? null
                : wardRepository.findByIdAndActiveTrue(wardId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Active ward not found"
                        ));
        if (district != null && !district.getProvince().getId().equals(province.getId())) {
            throw new BusinessException("District does not belong to province");
        }
        if (ward != null && !ward.getDistrict().getId().equals(district.getId())) {
            throw new BusinessException("Ward does not belong to district");
        }
        return new Location(province, district, ward);
    }

    private boolean isManagerOrAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.ADMIN.name())
                || actor.roles().contains(RoleCode.MANAGER.name());
    }

    private record Location(Province province, District district, Ward ward) {
    }
}
