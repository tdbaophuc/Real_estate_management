package com.javaweb.contract.repository;

import com.javaweb.contract.entity.ContractParty;
import com.javaweb.contract.enums.ContractPartyRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractPartyRepository extends JpaRepository<ContractParty, Long> {
    List<ContractParty> findAllByContractIdOrderBySigningOrderAsc(Long contractId);

    List<ContractParty> findAllByContractIdAndPartyRole(
            Long contractId,
            ContractPartyRole partyRole
    );
}
