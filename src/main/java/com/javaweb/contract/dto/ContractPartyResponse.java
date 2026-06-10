package com.javaweb.contract.dto;

import com.javaweb.contract.enums.ContractPartyRole;

public record ContractPartyResponse(
        Long id,
        Long userId,
        Long customerId,
        ContractPartyRole partyRole,
        String fullName,
        String email,
        String phone,
        int signingOrder,
        boolean requiredSigner
) {
}
