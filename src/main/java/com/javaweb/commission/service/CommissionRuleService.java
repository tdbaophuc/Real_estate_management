package com.javaweb.commission.service;

import com.javaweb.auth.entity.User;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.commission.dto.CommissionRuleRequest;
import com.javaweb.commission.dto.CommissionRuleResponse;
import com.javaweb.commission.dto.CommissionRuleSearchRequest;
import com.javaweb.commission.entity.CommissionRule;
import com.javaweb.commission.mapper.CommissionMapper;
import com.javaweb.commission.repository.CommissionRuleRepository;
import com.javaweb.commission.repository.CommissionRuleSpecifications;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.DuplicateResourceException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.common.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CommissionRuleService {
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "code", "code",
            "name", "name",
            "transactionType", "transactionType",
            "calculationType", "calculationType",
            "priority", "priority",
            "active", "active",
            "effectiveFrom", "effectiveFrom",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    private final CommissionRuleRepository ruleRepository;
    private final UserRepository userRepository;
    private final CommissionMapper mapper;

    public CommissionRuleService(
            CommissionRuleRepository ruleRepository,
            UserRepository userRepository,
            CommissionMapper mapper
    ) {
        this.ruleRepository = ruleRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional
    public CommissionRuleResponse create(
            CommissionRuleRequest request,
            AuthUserPrincipal actor
    ) {
        if (ruleRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Commission rule code already exists");
        }
        User creator = userRepository.findById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found"
                ));
        CommissionRule rule = new CommissionRule(
                request.code(),
                request.name(),
                request.calculationType(),
                creator
        );
        apply(rule, request);
        return mapper.toRuleResponse(ruleRepository.saveAndFlush(rule));
    }

    @Transactional(readOnly = true)
    public PageResponse<CommissionRuleResponse> search(
            CommissionRuleSearchRequest request
    ) {
        String sortField = SORT_FIELDS.get(request.sortBy());
        if (sortField == null) {
            throw new BusinessException("Unsupported commission rule sort field");
        }
        Page<CommissionRule> page = ruleRepository.findAll(
                CommissionRuleSpecifications.search(request),
                PageRequest.of(
                        request.page(),
                        request.size(),
                        Sort.by(request.direction(), sortField)
                                .and(Sort.by(Sort.Direction.ASC, "id"))
                )
        );
        return PageResponse.from(
                page,
                page.getContent().stream().map(mapper::toRuleResponse).toList()
        );
    }

    @Transactional
    public CommissionRuleResponse update(
            Long ruleId,
            CommissionRuleRequest request
    ) {
        CommissionRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commission rule not found"
                ));
        ruleRepository.findByCode(request.code())
                .filter(existing -> !existing.getId().equals(ruleId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Commission rule code already exists"
                    );
                });
        rule.setCode(request.code());
        apply(rule, request);
        return mapper.toRuleResponse(ruleRepository.saveAndFlush(rule));
    }

    private void apply(CommissionRule rule, CommissionRuleRequest request) {
        rule.setName(request.name());
        rule.setTransactionType(request.transactionType());
        rule.setCalculationType(request.calculationType());
        rule.setRate(request.rate());
        rule.setFixedAmount(request.fixedAmount());
        rule.setCurrency(request.currency());
        rule.setMinTransactionValue(request.minTransactionValue());
        rule.setMaxTransactionValue(request.maxTransactionValue());
        rule.setPriority(request.priority());
        rule.setActive(request.active());
        rule.setEffectiveFrom(request.effectiveFrom());
        rule.setEffectiveTo(request.effectiveTo());
        rule.setDescription(request.description());
    }
}
