package com.javaweb.contract.repository;

import com.javaweb.contract.entity.ContractSignature;
import com.javaweb.contract.enums.ContractSignatureStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractSignatureRepository extends JpaRepository<ContractSignature, Long> {
    List<ContractSignature> findAllByContractPartyContractId(Long contractId);

    List<ContractSignature> findAllByContractPartyContractIdAndStatus(
            Long contractId,
            ContractSignatureStatus status
    );
}
