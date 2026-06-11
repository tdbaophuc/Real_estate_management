package com.javaweb.contract.service;

import com.javaweb.audit.AuditActions;
import com.javaweb.audit.service.AuditLogService;
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
import com.javaweb.contract.dto.ContractCancelRequest;
import com.javaweb.contract.dto.ContractCreateRequest;
import com.javaweb.contract.dto.ContractDocumentResponse;
import com.javaweb.contract.dto.ContractResponse;
import com.javaweb.contract.dto.ContractSearchRequest;
import com.javaweb.contract.dto.ContractUpdateRequest;
import com.javaweb.contract.entity.Contract;
import com.javaweb.contract.entity.ContractDocument;
import com.javaweb.contract.entity.ContractParty;
import com.javaweb.contract.entity.ContractTemplate;
import com.javaweb.contract.enums.ContractDocumentType;
import com.javaweb.contract.enums.ContractPartyRole;
import com.javaweb.contract.enums.ContractStatus;
import com.javaweb.contract.enums.ContractType;
import com.javaweb.contract.mapper.ContractMapper;
import com.javaweb.contract.repository.ContractDocumentRepository;
import com.javaweb.contract.repository.ContractRepository;
import com.javaweb.contract.repository.ContractSpecifications;
import com.javaweb.contract.repository.ContractTemplateRepository;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.enums.CustomerStatus;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.property.entity.Property;
import com.javaweb.property.repository.PropertyRepository;
import com.javaweb.storage.entity.FileResource;
import com.javaweb.storage.enums.FileAccessLevel;
import com.javaweb.storage.service.FileResourceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Service
public class ContractService {
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "code", "code",
            "status", "status",
            "contractType", "contractType",
            "effectiveDate", "effectiveDate",
            "expirationDate", "expirationDate",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );
    private static final Set<ContractStatus> CANCELLABLE_STATUSES = Set.of(
            ContractStatus.DRAFT,
            ContractStatus.PENDING_REVIEW,
            ContractStatus.PENDING_SIGNATURE
    );

    private final ContractRepository contractRepository;
    private final ContractDocumentRepository documentRepository;
    private final ContractTemplateRepository templateRepository;
    private final PropertyRepository propertyRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final FileResourceService fileResourceService;
    private final ContractMapper contractMapper;
    private final AuditLogService auditLogService;

    public ContractService(
            ContractRepository contractRepository,
            ContractDocumentRepository documentRepository,
            ContractTemplateRepository templateRepository,
            PropertyRepository propertyRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            FileResourceService fileResourceService,
            ContractMapper contractMapper,
            AuditLogService auditLogService
    ) {
        this.contractRepository = contractRepository;
        this.documentRepository = documentRepository;
        this.templateRepository = templateRepository;
        this.propertyRepository = propertyRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.fileResourceService = fileResourceService;
        this.contractMapper = contractMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ContractResponse create(
            ContractCreateRequest request,
            AuthUserPrincipal actor
    ) {
        if (contractRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Contract code already exists");
        }
        User creator = requireUser(actor.id(), "Authenticated user not found");
        Property property = requireProperty(request.propertyId());
        Customer customer = requireCustomer(request.customerId());
        User agent = requireActiveAgent(request.agentId());
        requireCanCreate(agent, customer, property, actor);
        ContractTemplate template = resolveTemplate(
                request.templateId(),
                request.contractType()
        );

        Contract contract = new Contract(
                request.code(),
                request.contractType(),
                request.title(),
                property,
                customer,
                property.getOwner(),
                agent,
                creator
        );
        contract.setTemplate(template);
        applyEditableFields(
                contract,
                request.title(),
                request.totalValue(),
                request.currency(),
                request.effectiveDate(),
                request.expirationDate(),
                request.terms(),
                request.notes()
        );
        addDefaultParties(contract);
        return contractMapper.toResponse(contractRepository.saveAndFlush(contract));
    }

    @Transactional(readOnly = true)
    public PageResponse<ContractResponse> search(
            ContractSearchRequest request,
            AuthUserPrincipal actor
    ) {
        String sortField = SORT_FIELDS.get(request.sortBy());
        if (sortField == null) {
            throw new BusinessException("Unsupported contract sort field");
        }
        Long visibleAgentId = isManagerOrAdmin(actor) ? null : actor.id();
        Page<Contract> page = contractRepository.findAll(
                ContractSpecifications.search(request, visibleAgentId),
                PageRequest.of(
                        request.page(),
                        request.size(),
                        Sort.by(request.direction(), sortField)
                                .and(Sort.by(Sort.Direction.ASC, "id"))
                )
        );
        return PageResponse.from(
                page,
                page.getContent().stream().map(contractMapper::toResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public ContractResponse get(Long contractId, AuthUserPrincipal actor) {
        return contractMapper.toResponse(requireAccessible(contractId, actor));
    }

    @Transactional
    public ContractResponse update(
            Long contractId,
            ContractUpdateRequest request,
            AuthUserPrincipal actor
    ) {
        Contract contract = requireAccessible(contractId, actor);
        requireAssignedAgentOrManagement(contract, actor);
        requireStatus(
                contract,
                ContractStatus.DRAFT,
                "Only draft contracts can be updated"
        );
        applyEditableFields(
                contract,
                request.title(),
                request.totalValue(),
                request.currency(),
                request.effectiveDate(),
                request.expirationDate(),
                request.terms(),
                request.notes()
        );
        return contractMapper.toResponse(contractRepository.saveAndFlush(contract));
    }

    @Transactional
    public ContractDocumentResponse uploadDocument(
            Long contractId,
            MultipartFile file,
            ContractDocumentType documentType,
            String displayName,
            String description,
            boolean primaryDocument,
            AuthUserPrincipal actor
    ) {
        Contract contract = requireAccessible(contractId, actor);
        requireAssignedAgentOrManagement(contract, actor);
        requireDocumentUploadAllowed(contract, documentType);
        String resolvedDisplayName = normalizeDisplayName(displayName, file);
        String resolvedDescription = normalizeDescription(description);
        int version = documentRepository
                .findAllByContractIdAndDocumentTypeOrderByVersionDesc(
                        contractId,
                        documentType
                )
                .stream()
                .findFirst()
                .map(document -> document.getVersion() + 1)
                .orElse(1);
        FileResource resource = fileResourceService.store(
                file,
                FileAccessLevel.PRIVATE,
                actor
        );
        try {
            ContractDocument document = new ContractDocument(
                    resource,
                    resource.getUploadedBy(),
                    documentType,
                    resolvedDisplayName
            );
            document.setVersion(version);
            document.setDescription(resolvedDescription);
            document.setPrimaryDocument(primaryDocument);
            contract.addDocument(document);
            return contractMapper.toDocumentResponse(
                    documentRepository.saveAndFlush(document)
            );
        } catch (RuntimeException exception) {
            fileResourceService.delete(resource);
            throw exception;
        }
    }

    @Transactional
    public ContractResponse submitReview(Long contractId, AuthUserPrincipal actor) {
        Contract contract = requireAccessible(contractId, actor);
        requireAssignedAgentOrManagement(contract, actor);
        requireStatus(
                contract,
                ContractStatus.DRAFT,
                "Only draft contracts can be submitted for review"
        );
        if (!documentRepository.existsByContractId(contractId)) {
            throw new BusinessException(
                    "At least one contract document is required before review"
            );
        }
        contract.setStatus(ContractStatus.PENDING_REVIEW);
        contract.setSubmittedAt(Instant.now());
        return saveStatusChange(
                contract,
                ContractStatus.DRAFT,
                actor,
                null
        );
    }

    @Transactional
    public ContractResponse approve(Long contractId, AuthUserPrincipal actor) {
        requireManagement(actor);
        Contract contract = requireAccessible(contractId, actor);
        requireStatus(
                contract,
                ContractStatus.PENDING_REVIEW,
                "Only contracts pending review can be approved"
        );
        ContractStatus previousStatus = contract.getStatus();
        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        contract.setApprovedAt(Instant.now());
        return saveStatusChange(contract, previousStatus, actor, null);
    }

    @Transactional
    public ContractResponse markSigned(Long contractId, AuthUserPrincipal actor) {
        Contract contract = requireAccessible(contractId, actor);
        requireAssignedAgentOrManagement(contract, actor);
        requireStatus(
                contract,
                ContractStatus.PENDING_SIGNATURE,
                "Only contracts pending signature can be marked signed"
        );
        if (!documentRepository.existsByContractIdAndDocumentType(
                contractId,
                ContractDocumentType.SIGNED
        )) {
            throw new BusinessException(
                    "A signed contract document is required before marking signed"
            );
        }
        ContractStatus previousStatus = contract.getStatus();
        contract.setStatus(ContractStatus.SIGNED);
        contract.setSignedAt(Instant.now());
        return saveStatusChange(contract, previousStatus, actor, null);
    }

    @Transactional
    public ContractResponse cancel(
            Long contractId,
            ContractCancelRequest request,
            AuthUserPrincipal actor
    ) {
        Contract contract = requireAccessible(contractId, actor);
        requireAssignedAgentOrManagement(contract, actor);
        if (!CANCELLABLE_STATUSES.contains(contract.getStatus())) {
            throw new BusinessException(
                    "Only draft, pending review or pending signature contracts can be cancelled"
            );
        }
        ContractStatus previousStatus = contract.getStatus();
        contract.setStatus(ContractStatus.CANCELLED);
        contract.setCancellationReason(request.reason());
        contract.setCancelledAt(Instant.now());
        return saveStatusChange(contract, previousStatus, actor, request.reason());
    }

    private ContractResponse saveStatusChange(
            Contract contract,
            ContractStatus previousStatus,
            AuthUserPrincipal actor,
            String reason
    ) {
        Contract saved = contractRepository.saveAndFlush(contract);
        Map<String, Object> newValue = new java.util.LinkedHashMap<>();
        newValue.put("status", saved.getStatus().name());
        if (reason != null) {
            newValue.put("reason", reason);
        }
        auditLogService.record(
                actor,
                AuditActions.CONTRACT_STATUS_CHANGED,
                AuditActions.CONTRACT,
                saved.getId(),
                Map.of("status", previousStatus.name()),
                newValue
        );
        return contractMapper.toResponse(saved);
    }

    private Contract requireAccessible(Long contractId, AuthUserPrincipal actor) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
        if (isManagerOrAdmin(actor)
                || contract.getAgent().getId().equals(actor.id())) {
            return contract;
        }
        throw new AccessDeniedException(
                "You can only access contracts assigned to you"
        );
    }

    private Property requireProperty(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        if (property.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Property not found");
        }
        if (property.getOwner() == null) {
            throw new BusinessException("Property must have an owner");
        }
        return property;
    }

    private Customer requireCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (customer.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Customer not found");
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessException("Customer must be active");
        }
        return customer;
    }

    private User requireActiveAgent(Long agentId) {
        User agent = requireUser(agentId, "Agent not found");
        boolean hasAgentRole = agent.getRoles().stream()
                .map(Role::getCode)
                .anyMatch(RoleCode.AGENT::equals);
        if (!hasAgentRole) {
            throw new BusinessException("Assigned user must have the AGENT role");
        }
        if (agent.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Assigned agent must be active");
        }
        return agent;
    }

    private User requireUser(Long userId, String message) {
        return userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(message));
    }

    private ContractTemplate resolveTemplate(
            Long templateId,
            ContractType contractType
    ) {
        if (templateId == null) {
            return null;
        }
        ContractTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contract template not found"
                ));
        if (!template.isActive()) {
            throw new BusinessException("Contract template must be active");
        }
        if (template.getContractType() != contractType) {
            throw new BusinessException(
                    "Contract template type must match contract type"
            );
        }
        return template;
    }

    private void requireCanCreate(
            User agent,
            Customer customer,
            Property property,
            AuthUserPrincipal actor
    ) {
        if (isManagerOrAdmin(actor)) {
            return;
        }
        if (!agent.getId().equals(actor.id())) {
            throw new AccessDeniedException(
                    "Agents cannot create contracts for another agent"
            );
        }
        boolean customerAccessible = customer.getCreatedBy().getId().equals(actor.id())
                || customer.getAssignedAgent() != null
                && customer.getAssignedAgent().getId().equals(actor.id());
        boolean propertyAccessible = property.getCreatedBy().getId().equals(actor.id())
                || property.getAssignedAgent() != null
                && property.getAssignedAgent().getId().equals(actor.id());
        if (!customerAccessible || !propertyAccessible) {
            throw new AccessDeniedException(
                    "Agents can only create contracts for accessible customers and properties"
            );
        }
    }

    private void addDefaultParties(Contract contract) {
        ContractParty customerParty = new ContractParty(
                contract.getContractType() == ContractType.SALE
                        ? ContractPartyRole.BUYER
                        : ContractPartyRole.TENANT,
                contract.getCustomer().getFullName()
        );
        customerParty.setCustomer(contract.getCustomer());
        customerParty.setUser(contract.getCustomer().getUser());
        customerParty.setEmail(contract.getCustomer().getEmail());
        customerParty.setPhone(contract.getCustomer().getPhone());
        customerParty.setSigningOrder(1);
        contract.addParty(customerParty);

        ContractParty ownerParty = new ContractParty(
                contract.getContractType() == ContractType.SALE
                        ? ContractPartyRole.SELLER
                        : ContractPartyRole.LANDLORD,
                contract.getOwner().getFullName()
        );
        ownerParty.setUser(contract.getOwner());
        ownerParty.setEmail(contract.getOwner().getEmail());
        ownerParty.setPhone(contract.getOwner().getPhone());
        ownerParty.setSigningOrder(2);
        contract.addParty(ownerParty);

        ContractParty agentParty = new ContractParty(
                ContractPartyRole.AGENT,
                contract.getAgent().getFullName()
        );
        agentParty.setUser(contract.getAgent());
        agentParty.setEmail(contract.getAgent().getEmail());
        agentParty.setPhone(contract.getAgent().getPhone());
        agentParty.setSigningOrder(3);
        agentParty.setRequiredSigner(false);
        contract.addParty(agentParty);
    }

    private void applyEditableFields(
            Contract contract,
            String title,
            java.math.BigDecimal totalValue,
            String currency,
            java.time.LocalDate effectiveDate,
            java.time.LocalDate expirationDate,
            String terms,
            String notes
    ) {
        contract.setTitle(title);
        contract.setTotalValue(totalValue);
        contract.setCurrency(currency);
        contract.setEffectiveDate(effectiveDate);
        contract.setExpirationDate(expirationDate);
        contract.setTerms(terms);
        contract.setNotes(notes);
    }

    private void requireDocumentUploadAllowed(
            Contract contract,
            ContractDocumentType documentType
    ) {
        if (documentType == ContractDocumentType.SIGNED) {
            requireStatus(
                    contract,
                    ContractStatus.PENDING_SIGNATURE,
                    "Signed documents can only be uploaded while pending signature"
            );
            return;
        }
        requireStatus(
                contract,
                ContractStatus.DRAFT,
                "Draft, final and attachment documents can only be uploaded to draft contracts"
        );
    }

    private void requireStatus(
            Contract contract,
            ContractStatus required,
            String message
    ) {
        if (contract.getStatus() != required) {
            throw new BusinessException(message);
        }
    }

    private void requireAssignedAgentOrManagement(
            Contract contract,
            AuthUserPrincipal actor
    ) {
        if (isManagerOrAdmin(actor)
                || contract.getAgent().getId().equals(actor.id())) {
            return;
        }
        throw new AccessDeniedException(
                "Only the assigned agent or management can modify this contract"
        );
    }

    private void requireManagement(AuthUserPrincipal actor) {
        if (!isManagerOrAdmin(actor)) {
            throw new AccessDeniedException(
                    "Only managers or administrators can approve contracts"
            );
        }
    }

    private boolean isManagerOrAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.MANAGER.name())
                || actor.roles().contains(RoleCode.ADMIN.name());
    }

    private String normalizeDisplayName(String displayName, MultipartFile file) {
        String value = displayName == null || displayName.isBlank()
                ? file.getOriginalFilename()
                : displayName.trim();
        if (value == null || value.isBlank()) {
            throw new BusinessException("displayName is required");
        }
        if (value.length() > 255) {
            throw new BusinessException("displayName must not exceed 255 characters");
        }
        return value;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String value = description.trim();
        if (value.length() > 1000) {
            throw new BusinessException("description must not exceed 1000 characters");
        }
        return value;
    }
}
