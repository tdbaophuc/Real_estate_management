package com.javaweb.commission.repository;

import com.javaweb.commission.entity.CommissionRule;
import com.javaweb.contract.enums.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CommissionRuleRepository extends JpaRepository<CommissionRule, Long>,
        JpaSpecificationExecutor<CommissionRule> {
    Optional<CommissionRule> findByCode(String code);

    boolean existsByCode(String code);

    List<CommissionRule> findAllByActiveTrueOrderByPriorityDescIdAsc();

    List<CommissionRule> findAllByActiveTrueAndTransactionTypeOrderByPriorityDesc(
            ContractType transactionType
    );
}
