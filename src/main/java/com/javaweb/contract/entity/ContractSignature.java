package com.javaweb.contract.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.contract.enums.ContractSignatureMethod;
import com.javaweb.contract.enums.ContractSignatureStatus;
import com.javaweb.property.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "contract_signatures",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_contract_signatures_party_document",
                columnNames = {"contract_party_id", "contract_document_id"}
        )
)
public class ContractSignature extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_party_id", nullable = false)
    private ContractParty contractParty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_document_id")
    private ContractDocument contractDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signer_user_id")
    private User signerUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_method", nullable = false, length = 30)
    private ContractSignatureMethod signatureMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContractSignatureStatus status = ContractSignatureStatus.PENDING;

    @Column(name = "provider_name", length = 100)
    private String providerName;

    @Column(name = "provider_signature_id", length = 255)
    private String providerSignatureId;

    @Column(name = "signature_data", length = 4000)
    private String signatureData;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "declined_at")
    private Instant declinedAt;

    @Column(name = "decline_reason", length = 1000)
    private String declineReason;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    protected ContractSignature() {
    }

    public ContractSignature(ContractSignatureMethod signatureMethod) {
        this.signatureMethod = signatureMethod;
    }

    void setContractParty(ContractParty contractParty) {
        this.contractParty = contractParty;
    }

    public Long getId() {
        return id;
    }

    public ContractParty getContractParty() {
        return contractParty;
    }

    public ContractDocument getContractDocument() {
        return contractDocument;
    }

    public void setContractDocument(ContractDocument contractDocument) {
        this.contractDocument = contractDocument;
    }

    public User getSignerUser() {
        return signerUser;
    }

    public void setSignerUser(User signerUser) {
        this.signerUser = signerUser;
    }

    public ContractSignatureMethod getSignatureMethod() {
        return signatureMethod;
    }

    public void setSignatureMethod(ContractSignatureMethod signatureMethod) {
        this.signatureMethod = signatureMethod;
    }

    public ContractSignatureStatus getStatus() {
        return status;
    }

    public void setStatus(ContractSignatureStatus status) {
        this.status = status;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderSignatureId() {
        return providerSignatureId;
    }

    public void setProviderSignatureId(String providerSignatureId) {
        this.providerSignatureId = providerSignatureId;
    }

    public String getSignatureData() {
        return signatureData;
    }

    public void setSignatureData(String signatureData) {
        this.signatureData = signatureData;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }

    public Instant getDeclinedAt() {
        return declinedAt;
    }

    public void setDeclinedAt(Instant declinedAt) {
        this.declinedAt = declinedAt;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
