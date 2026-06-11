package com.javaweb.transaction.repository;

import com.javaweb.transaction.entity.Transaction;
import com.javaweb.transaction.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByCode(String code);

    Optional<Transaction> findByContractId(Long contractId);

    boolean existsByCode(String code);

    Page<Transaction> findAllByAgentId(Long agentId, Pageable pageable);

    Page<Transaction> findAllByCustomerId(Long customerId, Pageable pageable);

    Page<Transaction> findAllByStatus(TransactionStatus status, Pageable pageable);
}
