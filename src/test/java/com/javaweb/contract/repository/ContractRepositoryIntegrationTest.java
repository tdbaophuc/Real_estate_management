package com.javaweb.contract.repository;

import com.javaweb.contract.entity.Contract;
import com.javaweb.contract.enums.ContractDocumentType;
import com.javaweb.contract.enums.ContractPartyRole;
import com.javaweb.contract.enums.ContractSignatureStatus;
import com.javaweb.contract.enums.ContractStatus;
import com.javaweb.contract.enums.ContractType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContractRepositoryIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractPartyRepository partyRepository;

    @Autowired
    private ContractDocumentRepository documentRepository;

    @Autowired
    private ContractTemplateRepository templateRepository;

    @Autowired
    private ContractSignatureRepository signatureRepository;

    @BeforeEach
    void setUpContractGraph() {
        jdbcTemplate.update(
                """
                INSERT INTO users (email, password_hash, full_name, status, email_verified)
                VALUES
                    ('contract-owner@example.com', 'hash', 'Contract Owner', 'ACTIVE', TRUE),
                    ('contract-agent@example.com', 'hash', 'Contract Agent', 'ACTIVE', TRUE),
                    ('contract-customer@example.com', 'hash', 'Contract Customer', 'ACTIVE', TRUE)
                """
        );
        jdbcTemplate.update(
                "INSERT INTO provinces (code, name) VALUES ('CONTRACT-PROVINCE', 'Contract Province')"
        );
        jdbcTemplate.update(
                """
                INSERT INTO addresses (province_id, street_address, full_address)
                SELECT id, '29 Contract Street', '29 Contract Street, Contract Province'
                FROM provinces
                WHERE code = 'CONTRACT-PROVINCE'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO properties (
                    code,
                    property_type_id,
                    address_id,
                    owner_id,
                    created_by,
                    assigned_agent_id,
                    name,
                    purpose,
                    status,
                    price
                )
                SELECT
                    'CONTRACT-PROPERTY-001',
                    property_type.id,
                    address.id,
                    owner.id,
                    owner.id,
                    agent.id,
                    'Contract test property',
                    'SALE',
                    'AVAILABLE',
                    3000000000
                FROM property_types property_type
                CROSS JOIN addresses address
                CROSS JOIN users owner
                CROSS JOIN users agent
                WHERE property_type.code = 'APARTMENT'
                  AND address.full_address = '29 Contract Street, Contract Province'
                  AND owner.email = 'contract-owner@example.com'
                  AND agent.email = 'contract-agent@example.com'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO customers (
                    code,
                    user_id,
                    assigned_agent_id,
                    created_by,
                    full_name,
                    email
                )
                SELECT
                    'CONTRACT-CUSTOMER-001',
                    customer.id,
                    agent.id,
                    agent.id,
                    customer.full_name,
                    customer.email
                FROM users customer
                CROSS JOIN users agent
                WHERE customer.email = 'contract-customer@example.com'
                  AND agent.email = 'contract-agent@example.com'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO contract_templates (
                    code,
                    name,
                    contract_type,
                    content_template,
                    created_by
                )
                SELECT
                    'SALE-STANDARD',
                    'Standard sale agreement',
                    'SALE',
                    'Agreement between {{seller}} and {{buyer}}',
                    id
                FROM users
                WHERE email = 'contract-agent@example.com'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO contracts (
                    code,
                    template_id,
                    property_id,
                    customer_id,
                    owner_id,
                    agent_id,
                    created_by,
                    contract_type,
                    title,
                    total_value,
                    effective_date,
                    expiration_date
                )
                SELECT
                    'CONTRACT-001',
                    template.id,
                    property.id,
                    customer.id,
                    owner.id,
                    agent.id,
                    agent.id,
                    'SALE',
                    'Apartment sale agreement',
                    3000000000,
                    DATE '2030-01-15',
                    DATE '2030-02-15'
                FROM contract_templates template
                CROSS JOIN properties property
                CROSS JOIN customers customer
                CROSS JOIN users owner
                CROSS JOIN users agent
                WHERE template.code = 'SALE-STANDARD'
                  AND property.code = 'CONTRACT-PROPERTY-001'
                  AND customer.code = 'CONTRACT-CUSTOMER-001'
                  AND owner.email = 'contract-owner@example.com'
                  AND agent.email = 'contract-agent@example.com'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO contract_parties (
                    contract_id,
                    customer_id,
                    party_role,
                    full_name,
                    email,
                    signing_order
                )
                SELECT
                    contract.id,
                    customer.id,
                    'BUYER',
                    customer.full_name,
                    customer.email,
                    1
                FROM contracts contract
                CROSS JOIN customers customer
                WHERE contract.code = 'CONTRACT-001'
                  AND customer.code = 'CONTRACT-CUSTOMER-001'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO file_resources (
                    uploaded_by,
                    original_file_name,
                    storage_key,
                    content_type,
                    file_size,
                    checksum_sha256,
                    storage_provider,
                    access_level
                )
                SELECT
                    id,
                    'contract-001.pdf',
                    'contracts/contract-001.pdf',
                    'application/pdf',
                    1024,
                    'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    'LOCAL',
                    'PRIVATE'
                FROM users
                WHERE email = 'contract-agent@example.com'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO contract_documents (
                    contract_id,
                    file_resource_id,
                    uploaded_by,
                    document_type,
                    display_name,
                    primary_document
                )
                SELECT
                    contract.id,
                    file.id,
                    agent.id,
                    'FINAL',
                    'Final sale agreement',
                    TRUE
                FROM contracts contract
                CROSS JOIN file_resources file
                CROSS JOIN users agent
                WHERE contract.code = 'CONTRACT-001'
                  AND file.storage_key = 'contracts/contract-001.pdf'
                  AND agent.email = 'contract-agent@example.com'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO contract_signatures (
                    contract_party_id,
                    contract_document_id,
                    signer_user_id,
                    signature_method,
                    status
                )
                SELECT
                    party.id,
                    document.id,
                    customer_user.id,
                    'ELECTRONIC',
                    'PENDING'
                FROM contract_parties party
                CROSS JOIN contract_documents document
                CROSS JOIN users customer_user
                WHERE party.contract_id = document.contract_id
                  AND customer_user.email = 'contract-customer@example.com'
                """
        );
    }

    @Test
    void shouldMapAndQueryCompleteContractGraph() {
        Contract contract = contractRepository.findByCode("CONTRACT-001").orElseThrow();

        assertThat(contract.getContractType()).isEqualTo(ContractType.SALE);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.DRAFT);
        assertThat(contract.getProperty().getCode()).isEqualTo("CONTRACT-PROPERTY-001");
        assertThat(contract.getCustomer().getCode()).isEqualTo("CONTRACT-CUSTOMER-001");
        assertThat(contract.getOwner().getEmail()).isEqualTo("contract-owner@example.com");
        assertThat(contract.getAgent().getEmail()).isEqualTo("contract-agent@example.com");
        assertThat(contract.getTemplate().getCode()).isEqualTo("SALE-STANDARD");

        assertThat(contractRepository.findAllByPropertyId(
                contract.getProperty().getId(),
                PageRequest.of(0, 10)
        )).hasSize(1);
        assertThat(partyRepository.findAllByContractIdAndPartyRole(
                contract.getId(),
                ContractPartyRole.BUYER
        )).singleElement().satisfies(party ->
                assertThat(party.getCustomer().getCode()).isEqualTo("CONTRACT-CUSTOMER-001"));
        assertThat(documentRepository.findAllByContractIdAndDocumentTypeOrderByVersionDesc(
                contract.getId(),
                ContractDocumentType.FINAL
        )).singleElement().satisfies(document ->
                assertThat(document.getFileResource().getStorageKey())
                        .isEqualTo("contracts/contract-001.pdf"));
        assertThat(templateRepository.findAllByContractTypeAndActiveTrueOrderByNameAscVersionDesc(
                ContractType.SALE
        )).extracting("code").contains("SALE-STANDARD");
        assertThat(signatureRepository.findAllByContractPartyContractIdAndStatus(
                contract.getId(),
                ContractSignatureStatus.PENDING
        )).singleElement().satisfies(signature ->
                assertThat(signature.getSignerUser().getEmail())
                        .isEqualTo("contract-customer@example.com"));
    }

    @Test
    void shouldEnforceContractLifecycleAndSignatureConstraints() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                UPDATE contracts
                SET status = 'SIGNED'
                WHERE code = 'CONTRACT-001'
                """
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                UPDATE contracts
                SET effective_date = DATE '2030-03-01',
                    expiration_date = DATE '2030-02-01'
                WHERE code = 'CONTRACT-001'
                """
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                UPDATE contract_signatures
                SET status = 'SIGNED'
                WHERE contract_party_id IN (
                    SELECT party.id
                    FROM contract_parties party
                    JOIN contracts contract ON contract.id = party.contract_id
                    WHERE contract.code = 'CONTRACT-001'
                )
                """
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
