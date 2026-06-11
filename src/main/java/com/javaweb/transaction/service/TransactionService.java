package com.javaweb.transaction.service;

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
import com.javaweb.commission.service.CommissionService;
import com.javaweb.contract.entity.Contract;
import com.javaweb.contract.enums.ContractStatus;
import com.javaweb.contract.enums.ContractType;
import com.javaweb.contract.repository.ContractRepository;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.enums.CustomerStatus;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.payment.entity.Invoice;
import com.javaweb.payment.entity.Payment;
import com.javaweb.payment.entity.Receipt;
import com.javaweb.payment.enums.InvoiceStatus;
import com.javaweb.payment.enums.PaymentStatus;
import com.javaweb.payment.repository.InvoiceRepository;
import com.javaweb.payment.repository.PaymentRepository;
import com.javaweb.payment.repository.ReceiptRepository;
import com.javaweb.property.entity.Property;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.repository.PropertyRepository;
import com.javaweb.transaction.dto.DepositCreateRequest;
import com.javaweb.transaction.dto.DepositResponse;
import com.javaweb.transaction.dto.InvoiceCreateRequest;
import com.javaweb.transaction.dto.InvoiceResponse;
import com.javaweb.transaction.dto.PaymentCreateRequest;
import com.javaweb.transaction.dto.PaymentResponse;
import com.javaweb.transaction.dto.PaymentScheduleCreateRequest;
import com.javaweb.transaction.dto.PaymentScheduleResponse;
import com.javaweb.transaction.dto.ReceiptCreateRequest;
import com.javaweb.transaction.dto.ReceiptResponse;
import com.javaweb.transaction.dto.TransactionCreateRequest;
import com.javaweb.transaction.dto.TransactionResponse;
import com.javaweb.transaction.dto.TransactionSearchRequest;
import com.javaweb.transaction.dto.TransactionStatusUpdateRequest;
import com.javaweb.transaction.entity.Deposit;
import com.javaweb.transaction.entity.PaymentSchedule;
import com.javaweb.transaction.entity.Transaction;
import com.javaweb.transaction.enums.DepositStatus;
import com.javaweb.transaction.enums.PaymentScheduleStatus;
import com.javaweb.transaction.enums.TransactionStatus;
import com.javaweb.transaction.mapper.TransactionMapper;
import com.javaweb.transaction.repository.DepositRepository;
import com.javaweb.transaction.repository.PaymentScheduleRepository;
import com.javaweb.transaction.repository.TransactionRepository;
import com.javaweb.transaction.repository.TransactionSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Service
public class TransactionService {
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "code", "code",
            "status", "status",
            "transactionType", "transactionType",
            "transactionDate", "transactionDate",
            "expectedCompletionDate", "expectedCompletionDate",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );
    private static final Set<TransactionStatus> TERMINAL_STATUSES = Set.of(
            TransactionStatus.COMPLETED,
            TransactionStatus.CANCELLED,
            TransactionStatus.REFUNDED
    );

    private final TransactionRepository transactionRepository;
    private final DepositRepository depositRepository;
    private final PaymentScheduleRepository scheduleRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReceiptRepository receiptRepository;
    private final ContractRepository contractRepository;
    private final PropertyRepository propertyRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final TransactionMapper mapper;
    private final CommissionService commissionService;
    private final AuditLogService auditLogService;

    public TransactionService(
            TransactionRepository transactionRepository,
            DepositRepository depositRepository,
            PaymentScheduleRepository scheduleRepository,
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            ReceiptRepository receiptRepository,
            ContractRepository contractRepository,
            PropertyRepository propertyRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            TransactionMapper mapper,
            CommissionService commissionService,
            AuditLogService auditLogService
    ) {
        this.transactionRepository = transactionRepository;
        this.depositRepository = depositRepository;
        this.scheduleRepository = scheduleRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.receiptRepository = receiptRepository;
        this.contractRepository = contractRepository;
        this.propertyRepository = propertyRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.commissionService = commissionService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public TransactionResponse create(
            TransactionCreateRequest request,
            AuthUserPrincipal actor
    ) {
        if (transactionRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Transaction code already exists");
        }
        User creator = requireUser(actor.id(), "Authenticated user not found");
        Property property = requireProperty(request.propertyId());
        Customer customer = requireCustomer(request.customerId());
        User agent = requireActiveAgent(request.agentId());
        requireCanCreate(property, customer, agent, actor);
        requirePurposeMatches(property, request.transactionType());
        Contract contract = resolveContract(
                request.contractId(),
                property,
                customer,
                agent,
                request.transactionType()
        );

        Transaction transaction = new Transaction(
                request.code(),
                request.transactionType(),
                request.agreedValue(),
                property,
                customer,
                property.getOwner(),
                agent,
                creator
        );
        transaction.setContract(contract);
        transaction.setCurrency(request.currency());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setExpectedCompletionDate(request.expectedCompletionDate());
        transaction.setNotes(request.notes());
        if (contract != null) {
            transaction.setStatus(TransactionStatus.CONTRACT_SIGNED);
        }
        return mapper.toResponse(transactionRepository.saveAndFlush(transaction));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> search(
            TransactionSearchRequest request,
            AuthUserPrincipal actor
    ) {
        String sortField = SORT_FIELDS.get(request.sortBy());
        if (sortField == null) {
            throw new BusinessException("Unsupported transaction sort field");
        }
        Long visibleAgentId = isManagerOrAdmin(actor) ? null : actor.id();
        Page<Transaction> page = transactionRepository.findAll(
                TransactionSpecifications.search(request, visibleAgentId),
                PageRequest.of(
                        request.page(),
                        request.size(),
                        Sort.by(request.direction(), sortField)
                                .and(Sort.by(Sort.Direction.ASC, "id"))
                )
        );
        return PageResponse.from(
                page,
                page.getContent().stream().map(mapper::toResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(Long transactionId, AuthUserPrincipal actor) {
        return mapper.toResponse(requireAccessible(transactionId, actor));
    }

    @Transactional
    public TransactionResponse updateStatus(
            Long transactionId,
            TransactionStatusUpdateRequest request,
            AuthUserPrincipal actor
    ) {
        Transaction transaction = requireAccessible(transactionId, actor);
        if (TERMINAL_STATUSES.contains(transaction.getStatus())) {
            throw new BusinessException("Terminal transactions cannot change status");
        }
        if (request.status() == transaction.getStatus()) {
            return mapper.toResponse(transaction);
        }
        TransactionStatus previousStatus = transaction.getStatus();
        if (TERMINAL_STATUSES.contains(request.status())) {
            requireManagement(actor);
        } else {
            requireAssignedAgentOrManagement(transaction, actor);
        }
        validateTransition(transaction, request.status());
        Instant now = Instant.now();
        switch (request.status()) {
            case COMPLETED -> transaction.setCompletedAt(now);
            case CANCELLED -> {
                transaction.setCancelledAt(now);
                transaction.setCancellationReason(request.reason());
            }
            case REFUNDED -> transaction.setRefundedAt(now);
            default -> {
            }
        }
        transaction.setStatus(request.status());
        Transaction saved = transactionRepository.saveAndFlush(transaction);
        if (request.status() == TransactionStatus.COMPLETED) {
            commissionService.calculateForCompletedTransaction(saved);
        }
        Map<String, Object> newValue = new java.util.LinkedHashMap<>();
        newValue.put("status", request.status().name());
        if (request.reason() != null) {
            newValue.put("reason", request.reason());
        }
        auditLogService.record(
                actor,
                AuditActions.TRANSACTION_STATUS_CHANGED,
                AuditActions.TRANSACTION,
                saved.getId(),
                Map.of("status", previousStatus.name()),
                newValue
        );
        return mapper.toResponse(saved);
    }

    @Transactional
    public DepositResponse addDeposit(
            Long transactionId,
            DepositCreateRequest request,
            AuthUserPrincipal actor
    ) {
        Transaction transaction = requireModifiable(transactionId, actor);
        requireCurrency(transaction, request.currency());
        Deposit existing = depositRepository.findByIdempotencyKey(request.idempotencyKey())
                .orElse(null);
        if (existing != null) {
            requireSameDeposit(existing, transaction, request);
            return mapper.toDepositResponse(existing);
        }
        requireConfirmedTotalWithinAgreedValue(transaction, request.amount());
        Instant confirmedAt = request.receivedAt() == null
                ? Instant.now()
                : request.receivedAt();
        Deposit deposit = new Deposit(
                requireUser(actor.id(), "Authenticated user not found"),
                request.amount(),
                request.paymentMethod(),
                request.idempotencyKey()
        );
        deposit.setCurrency(request.currency());
        deposit.setReferenceNumber(request.referenceNumber());
        deposit.setDueDate(request.dueDate());
        deposit.setReceivedAt(confirmedAt);
        deposit.setVerifiedAt(confirmedAt);
        deposit.setStatus(DepositStatus.VERIFIED);
        deposit.setNotes(request.notes());
        transaction.addDeposit(deposit);
        Deposit saved = depositRepository.saveAndFlush(deposit);
        if (transaction.getStatus() == TransactionStatus.PENDING) {
            transaction.setStatus(TransactionStatus.DEPOSITED);
        }
        return mapper.toDepositResponse(saved);
    }

    @Transactional
    public PaymentScheduleResponse addSchedule(
            Long transactionId,
            PaymentScheduleCreateRequest request,
            AuthUserPrincipal actor
    ) {
        Transaction transaction = requireModifiable(transactionId, actor);
        requireCurrency(transaction, request.currency());
        if (scheduleRepository.findByTransactionIdAndInstallmentNumber(
                transactionId,
                request.installmentNumber()
        ).isPresent()) {
            throw new DuplicateResourceException("Installment number already exists");
        }
        BigDecimal scheduled = transaction.getPaymentSchedules().stream()
                .map(PaymentSchedule::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (scheduled.add(request.amount()).compareTo(transaction.getAgreedValue()) > 0) {
            throw new BusinessException(
                    "Payment schedules cannot exceed the transaction agreed value"
            );
        }
        PaymentSchedule schedule = new PaymentSchedule(
                request.installmentNumber(),
                request.label(),
                request.dueDate(),
                request.amount()
        );
        schedule.setCurrency(request.currency());
        schedule.setNotes(request.notes());
        transaction.addPaymentSchedule(schedule);
        return mapper.toScheduleResponse(scheduleRepository.saveAndFlush(schedule));
    }

    @Transactional
    public PaymentResponse addPayment(
            Long transactionId,
            PaymentCreateRequest request,
            AuthUserPrincipal actor
    ) {
        Transaction transaction = requireModifiable(transactionId, actor);
        requireCurrency(transaction, request.currency());
        Payment existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey())
                .orElse(null);
        if (existing != null) {
            requireSamePayment(existing, transaction, request);
            return mapper.toPaymentResponse(existing);
        }
        requireConfirmedTotalWithinAgreedValue(transaction, request.amount());
        PaymentSchedule schedule = resolveSchedule(
                transactionId,
                request.paymentScheduleId()
        );
        if (schedule != null) {
            BigDecimal newPaidAmount = schedule.getPaidAmount().add(request.amount());
            if (newPaidAmount.compareTo(schedule.getAmount()) > 0) {
                throw new BusinessException("Payment exceeds the installment balance");
            }
            schedule.setPaidAmount(newPaidAmount);
            if (newPaidAmount.compareTo(schedule.getAmount()) == 0) {
                schedule.setStatus(PaymentScheduleStatus.PAID);
                schedule.setPaidAt(request.paidAt() == null
                        ? Instant.now()
                        : request.paidAt());
            } else {
                schedule.setStatus(PaymentScheduleStatus.PARTIALLY_PAID);
            }
        }
        Instant confirmedAt = request.paidAt() == null ? Instant.now() : request.paidAt();
        Payment payment = new Payment(
                requireUser(actor.id(), "Authenticated user not found"),
                request.amount(),
                request.paymentMethod(),
                request.idempotencyKey()
        );
        payment.setPaymentSchedule(schedule);
        payment.setCurrency(request.currency());
        payment.setReferenceNumber(request.referenceNumber());
        payment.setPaidAt(confirmedAt);
        payment.setConfirmedAt(confirmedAt);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setNotes(request.notes());
        transaction.addPayment(payment);
        Payment saved = paymentRepository.saveAndFlush(payment);
        if (transaction.getStatus() != TransactionStatus.PAYMENT_IN_PROGRESS) {
            transaction.setStatus(TransactionStatus.PAYMENT_IN_PROGRESS);
        }
        return mapper.toPaymentResponse(saved);
    }

    @Transactional
    public InvoiceResponse addInvoice(
            Long transactionId,
            InvoiceCreateRequest request,
            AuthUserPrincipal actor
    ) {
        Transaction transaction = requireModifiable(transactionId, actor);
        requireCurrency(transaction, request.currency());
        if (invoiceRepository.findByInvoiceNumber(request.invoiceNumber()).isPresent()) {
            throw new DuplicateResourceException("Invoice number already exists");
        }
        BigDecimal totalAmount = request.subtotal().add(request.taxAmount());
        Invoice invoice = new Invoice(
                requireUser(actor.id(), "Authenticated user not found"),
                request.invoiceNumber(),
                request.issueDate(),
                request.subtotal(),
                totalAmount,
                request.billedToName()
        );
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setDueDate(request.dueDate());
        invoice.setTaxAmount(request.taxAmount());
        invoice.setCurrency(request.currency());
        invoice.setBilledToEmail(request.billedToEmail());
        invoice.setBilledToAddress(request.billedToAddress());
        invoice.setNotes(request.notes());
        transaction.addInvoice(invoice);
        return mapper.toInvoiceResponse(invoiceRepository.saveAndFlush(invoice));
    }

    @Transactional
    public ReceiptResponse addReceipt(
            Long transactionId,
            Long paymentId,
            ReceiptCreateRequest request,
            AuthUserPrincipal actor
    ) {
        Transaction transaction = requireModifiable(transactionId, actor);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!payment.getTransaction().getId().equals(transaction.getId())) {
            throw new BusinessException("Payment must belong to the transaction");
        }
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException("Receipt requires a completed payment");
        }
        if (receiptRepository.findByPaymentId(paymentId).isPresent()) {
            throw new DuplicateResourceException("Payment already has a receipt");
        }
        if (receiptRepository.findByReceiptNumber(request.receiptNumber()).isPresent()) {
            throw new DuplicateResourceException("Receipt number already exists");
        }
        Receipt receipt = new Receipt(
                payment,
                requireUser(actor.id(), "Authenticated user not found"),
                request.receiptNumber(),
                request.issuedAt() == null ? Instant.now() : request.issuedAt(),
                payment.getAmount()
        );
        receipt.setCurrency(payment.getCurrency());
        receipt.setPayerName(request.payerName());
        receipt.setNotes(request.notes());
        return mapper.toReceiptResponse(receiptRepository.saveAndFlush(receipt));
    }

    private Transaction requireAccessible(Long transactionId, AuthUserPrincipal actor) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (isManagerOrAdmin(actor) || transaction.getAgent().getId().equals(actor.id())) {
            return transaction;
        }
        throw new AccessDeniedException(
                "You can only access transactions assigned to you"
        );
    }

    private Transaction requireModifiable(Long transactionId, AuthUserPrincipal actor) {
        Transaction transaction = requireAccessible(transactionId, actor);
        requireAssignedAgentOrManagement(transaction, actor);
        if (TERMINAL_STATUSES.contains(transaction.getStatus())) {
            throw new BusinessException("Terminal transactions cannot be modified");
        }
        return transaction;
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
        if (!hasAgentRole || agent.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Assigned agent must be active with AGENT role");
        }
        return agent;
    }

    private User requireUser(Long userId, String message) {
        return userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(message));
    }

    private Contract resolveContract(
            Long contractId,
            Property property,
            Customer customer,
            User agent,
            ContractType transactionType
    ) {
        if (contractId == null) {
            return null;
        }
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
        if (transactionRepository.findByContractId(contractId).isPresent()) {
            throw new DuplicateResourceException(
                    "Contract is already linked to a transaction"
            );
        }
        if (contract.getStatus() != ContractStatus.SIGNED
                && contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessException("Contract must be signed or active");
        }
        if (!contract.getProperty().getId().equals(property.getId())
                || !contract.getCustomer().getId().equals(customer.getId())
                || !contract.getOwner().getId().equals(property.getOwner().getId())
                || !contract.getAgent().getId().equals(agent.getId())
                || contract.getContractType() != transactionType) {
            throw new BusinessException(
                    "Contract parties and type must match the transaction"
            );
        }
        return contract;
    }

    private void requirePurposeMatches(Property property, ContractType transactionType) {
        boolean matches = transactionType == ContractType.SALE
                ? property.getPurpose() == PropertyPurpose.SALE
                : property.getPurpose() == PropertyPurpose.RENT;
        if (!matches) {
            throw new BusinessException(
                    "Transaction type must match the property purpose"
            );
        }
    }

    private void requireCanCreate(
            Property property,
            Customer customer,
            User agent,
            AuthUserPrincipal actor
    ) {
        if (isManagerOrAdmin(actor)) {
            return;
        }
        boolean customerAccessible = customer.getCreatedBy().getId().equals(actor.id())
                || customer.getAssignedAgent() != null
                && customer.getAssignedAgent().getId().equals(actor.id());
        boolean propertyAccessible = property.getCreatedBy().getId().equals(actor.id())
                || property.getAssignedAgent() != null
                && property.getAssignedAgent().getId().equals(actor.id());
        if (!agent.getId().equals(actor.id())
                || !customerAccessible
                || !propertyAccessible) {
            throw new AccessDeniedException(
                    "Agents can only create assigned transactions for accessible data"
            );
        }
    }

    private void validateTransition(Transaction transaction, TransactionStatus target) {
        if (!allowedTargets(transaction.getStatus()).contains(target)) {
            throw new BusinessException(
                    "Invalid transaction status transition from "
                            + transaction.getStatus() + " to " + target
            );
        }
        if (target == TransactionStatus.CONTRACT_SIGNED) {
            Contract contract = transaction.getContract();
            if (contract == null
                    || contract.getStatus() != ContractStatus.SIGNED
                    && contract.getStatus() != ContractStatus.ACTIVE) {
                throw new BusinessException(
                        "A signed or active contract is required"
                );
            }
        }
        if (target == TransactionStatus.COMPLETED
                && mapper.confirmedAmount(transaction)
                .compareTo(transaction.getAgreedValue()) < 0) {
            throw new BusinessException(
                    "Confirmed deposits and payments must cover the agreed value"
            );
        }
        if (target == TransactionStatus.REFUNDED
                && mapper.confirmedAmount(transaction).signum() == 0) {
            throw new BusinessException(
                    "A transaction without confirmed funds cannot be refunded"
            );
        }
    }

    private Set<TransactionStatus> allowedTargets(TransactionStatus current) {
        return switch (current) {
            case PENDING -> Set.of(
                    TransactionStatus.DEPOSITED,
                    TransactionStatus.CONTRACT_SIGNED,
                    TransactionStatus.PAYMENT_IN_PROGRESS,
                    TransactionStatus.CANCELLED
            );
            case DEPOSITED -> Set.of(
                    TransactionStatus.CONTRACT_SIGNED,
                    TransactionStatus.PAYMENT_IN_PROGRESS,
                    TransactionStatus.COMPLETED,
                    TransactionStatus.CANCELLED,
                    TransactionStatus.REFUNDED
            );
            case CONTRACT_SIGNED -> Set.of(
                    TransactionStatus.PAYMENT_IN_PROGRESS,
                    TransactionStatus.COMPLETED,
                    TransactionStatus.CANCELLED,
                    TransactionStatus.REFUNDED
            );
            case PAYMENT_IN_PROGRESS -> Set.of(
                    TransactionStatus.COMPLETED,
                    TransactionStatus.CANCELLED,
                    TransactionStatus.REFUNDED
            );
            default -> Set.of();
        };
    }

    private PaymentSchedule resolveSchedule(Long transactionId, Long scheduleId) {
        if (scheduleId == null) {
            return null;
        }
        PaymentSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment schedule not found"
                ));
        if (!schedule.getTransaction().getId().equals(transactionId)) {
            throw new BusinessException(
                    "Payment schedule must belong to the transaction"
            );
        }
        if (schedule.getStatus() == PaymentScheduleStatus.CANCELLED
                || schedule.getStatus() == PaymentScheduleStatus.PAID) {
            throw new BusinessException("Payment schedule cannot accept more payments");
        }
        return schedule;
    }

    private void requireCurrency(Transaction transaction, String currency) {
        if (!transaction.getCurrency().equals(currency)) {
            throw new BusinessException(
                    "Currency must match the transaction currency"
            );
        }
    }

    private void requireConfirmedTotalWithinAgreedValue(
            Transaction transaction,
            BigDecimal amount
    ) {
        if (mapper.confirmedAmount(transaction)
                .add(amount)
                .compareTo(transaction.getAgreedValue()) > 0) {
            throw new BusinessException(
                    "Confirmed deposits and payments cannot exceed the agreed value"
            );
        }
    }

    private void requireSameDeposit(
            Deposit existing,
            Transaction transaction,
            DepositCreateRequest request
    ) {
        if (!existing.getTransaction().getId().equals(transaction.getId())
                || existing.getAmount().compareTo(request.amount()) != 0
                || existing.getPaymentMethod() != request.paymentMethod()) {
            throw new DuplicateResourceException(
                    "Idempotency key is already used by another deposit"
            );
        }
    }

    private void requireSamePayment(
            Payment existing,
            Transaction transaction,
            PaymentCreateRequest request
    ) {
        Long existingScheduleId = existing.getPaymentSchedule() == null
                ? null
                : existing.getPaymentSchedule().getId();
        if (!existing.getTransaction().getId().equals(transaction.getId())
                || existing.getAmount().compareTo(request.amount()) != 0
                || existing.getPaymentMethod() != request.paymentMethod()
                || !java.util.Objects.equals(existingScheduleId, request.paymentScheduleId())) {
            throw new DuplicateResourceException(
                    "Idempotency key is already used by another payment"
            );
        }
    }

    private void requireAssignedAgentOrManagement(
            Transaction transaction,
            AuthUserPrincipal actor
    ) {
        if (isManagerOrAdmin(actor) || transaction.getAgent().getId().equals(actor.id())) {
            return;
        }
        throw new AccessDeniedException(
                "Only the assigned agent or management can modify this transaction"
        );
    }

    private void requireManagement(AuthUserPrincipal actor) {
        if (!isManagerOrAdmin(actor)) {
            throw new AccessDeniedException(
                    "Only managers or administrators can finalize transactions"
            );
        }
    }

    private boolean isManagerOrAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.MANAGER.name())
                || actor.roles().contains(RoleCode.ADMIN.name());
    }
}
