package com.javaweb.contract.repository;

import com.javaweb.contract.entity.ContractDocument;
import com.javaweb.contract.enums.ContractDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractDocumentRepository extends JpaRepository<ContractDocument, Long> {
    List<ContractDocument> findAllByContractIdOrderByCreatedAtDesc(Long contractId);

    List<ContractDocument> findAllByContractIdAndDocumentTypeOrderByVersionDesc(
            Long contractId,
            ContractDocumentType documentType
    );
}
