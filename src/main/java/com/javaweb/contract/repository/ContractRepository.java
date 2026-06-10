package com.javaweb.contract.repository;

import com.javaweb.contract.entity.Contract;
import com.javaweb.contract.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ContractRepository
        extends JpaRepository<Contract, Long>, JpaSpecificationExecutor<Contract> {
    Optional<Contract> findByCode(String code);

    boolean existsByCode(String code);

    Page<Contract> findAllByPropertyId(Long propertyId, Pageable pageable);

    Page<Contract> findAllByCustomerId(Long customerId, Pageable pageable);

    Page<Contract> findAllByOwnerId(Long ownerId, Pageable pageable);

    Page<Contract> findAllByAgentId(Long agentId, Pageable pageable);

    Page<Contract> findAllByStatus(ContractStatus status, Pageable pageable);
}
