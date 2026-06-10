package com.javaweb.commission.repository;

import com.javaweb.commission.entity.CommissionRule;
import com.javaweb.contract.enums.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommissionRuleRepository extends JpaRepository<CommissionRule, Long> {
    Optional<CommissionRule> findByCode(String code);

    List<CommissionRule> findAllByActiveTrueAndTransactionTypeOrderByPriorityDesc(
            ContractType transactionType
    );
}
