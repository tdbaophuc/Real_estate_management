package com.javaweb.contract.repository;

import com.javaweb.contract.entity.ContractTemplate;
import com.javaweb.contract.enums.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {
    Optional<ContractTemplate> findByCodeAndVersion(String code, int version);

    List<ContractTemplate> findAllByContractTypeAndActiveTrueOrderByNameAscVersionDesc(
            ContractType contractType
    );
}
