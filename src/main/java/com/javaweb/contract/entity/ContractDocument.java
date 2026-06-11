package com.javaweb.contract.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.contract.enums.ContractDocumentType;
import com.javaweb.property.entity.CreatedEntity;
import com.javaweb.storage.entity.FileResource;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "contract_documents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_contract_documents_file",
                        columnNames = "file_resource_id"
                ),
                @UniqueConstraint(
                        name = "uk_contract_documents_version",
                        columnNames = {"contract_id", "document_type", "version"}
                )
        }
)
public class ContractDocument extends CreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_resource_id", nullable = false)
    private FileResource fileResource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false, updatable = false)
    private User uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private ContractDocumentType documentType;

    @Column(nullable = false)
    private int version = 1;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(length = 1000)
    private String description;

    @Column(name = "primary_document", nullable = false)
    private boolean primaryDocument;

    @OneToMany(mappedBy = "contractDocument", fetch = FetchType.LAZY)
    private List<ContractSignature> signatures = new ArrayList<>();

    protected ContractDocument() {
    }

    public ContractDocument(
            FileResource fileResource,
            User uploadedBy,
            ContractDocumentType documentType,
            String displayName
    ) {
        this.fileResource = fileResource;
        this.uploadedBy = uploadedBy;
        this.documentType = documentType;
        this.displayName = displayName;
    }

    void setContract(Contract contract) {
        this.contract = contract;
    }

    public Long getId() {
        return id;
    }

    public Contract getContract() {
        return contract;
    }

    public FileResource getFileResource() {
        return fileResource;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public ContractDocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(ContractDocumentType documentType) {
        this.documentType = documentType;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPrimaryDocument() {
        return primaryDocument;
    }

    public void setPrimaryDocument(boolean primaryDocument) {
        this.primaryDocument = primaryDocument;
    }

    public List<ContractSignature> getSignatures() {
        return signatures;
    }
}
