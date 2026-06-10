package com.javaweb.commission.service;

import com.javaweb.auth.entity.User;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.commission.dto.CommissionMarkPaidRequest;
import com.javaweb.commission.dto.CommissionResponse;
import com.javaweb.commission.dto.CommissionSearchRequest;
import com.javaweb.commission.entity.Commission;
import com.javaweb.commission.entity.CommissionRule;
import com.javaweb.commission.enums.CommissionCalculationType;
import com.javaweb.commission.enums.CommissionStatus;
import com.javaweb.commission.mapper.CommissionMapper;
import com.javaweb.commission.repository.CommissionRepository;
import com.javaweb.commission.repository.CommissionRuleRepository;
import com.javaweb.commission.repository.CommissionSpecifications;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.common.response.PageResponse;
import com.javaweb.transaction.entity.Transaction;
import com.javaweb.transaction.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@Service
public class CommissionService {
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "status", "status",
            "baseAmount", "baseAmount",
            "amount", "amount",
            "approvedAt", "approvedAt",
            "paidAt", "paidAt",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    private final CommissionRepository commissionRepository;
    private final CommissionRuleRepository ruleRepository;
    private final UserRepository userRepository;
    private final CommissionCalculator calculator;
    private final CommissionMapper mapper;

    public CommissionService(
            CommissionRepository commissionRepository,
            CommissionRuleRepository ruleRepository,
            UserRepository userRepository,
            CommissionCalculator calculator,
            CommissionMapper mapper
    ) {
        this.commissionRepository = commissionRepository;
        this.ruleRepository = ruleRepository;
        this.userRepository = userRepository;
        this.calculator = calculator;
        this.mapper = mapper;
    }

    @Transactional
    public void calculateForCompletedTransaction(Transaction transaction) {
        if (transaction.getStatus() != TransactionStatus.COMPLETED
                || commissionRepository
                .findByTransactionIdAndBeneficiaryUserId(
                        transaction.getId(),
                        transaction.getAgent().getId()
                )
                .isPresent()) {
            return;
        }
        LocalDate calculationDate = transaction.getCompletedAt() == null
                ? LocalDate.now()
                : transaction.getCompletedAt()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        CommissionRule rule = calculator.selectRule(
                        ruleRepository.findAllByActiveTrueOrderByPriorityDescIdAsc(),
                        transaction,
                        calculationDate
                )
                .orElse(null);
        if (rule == null) {
            return;
        }
        BigDecimal amount = calculator.calculate(rule, transaction.getAgreedValue());
        Commission commission = new Commission(
                transaction,
                transaction.getAgent(),
                transaction.getAgreedValue(),
                amount
        );
        commission.setCommissionRule(rule);
        commission.setCurrency(transaction.getCurrency());
        if (rule.getCalculationType() == CommissionCalculationType.PERCENTAGE) {
            commission.setRate(rule.getRate());
        }
        commissionRepository.saveAndFlush(commission);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommissionResponse> search(
            CommissionSearchRequest request
    ) {
        return search(request, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommissionResponse> searchMine(
            CommissionSearchRequest request,
            AuthUserPrincipal actor
    ) {
        return search(request, actor.id());
    }

    @Transactional
    public CommissionResponse markPaid(
            Long commissionId,
            CommissionMarkPaidRequest request,
            AuthUserPrincipal actor
    ) {
        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commission not found"
                ));
        if (commission.getStatus() == CommissionStatus.PAID) {
            throw new BusinessException("Commission is already paid");
        }
        if (commission.getStatus() == CommissionStatus.CANCELLED) {
            throw new BusinessException("Cancelled commission cannot be paid");
        }
        User payer = userRepository.findById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found"
                ));
        Instant paidAt = request.paidAt() == null ? Instant.now() : request.paidAt();
        commission.setStatus(CommissionStatus.PAID);
        commission.setApprovedBy(payer);
        commission.setApprovedAt(paidAt);
        commission.setPaidBy(payer);
        commission.setPaidAt(paidAt);
        commission.setPaymentReference(request.paymentReference());
        commission.setNotes(request.notes());
        return mapper.toResponse(commissionRepository.saveAndFlush(commission));
    }

    private PageResponse<CommissionResponse> search(
            CommissionSearchRequest request,
            Long visibleBeneficiaryId
    ) {
        String sortField = SORT_FIELDS.get(request.sortBy());
        if (sortField == null) {
            throw new BusinessException("Unsupported commission sort field");
        }
        Page<Commission> page = commissionRepository.findAll(
                CommissionSpecifications.search(request, visibleBeneficiaryId),
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
}
