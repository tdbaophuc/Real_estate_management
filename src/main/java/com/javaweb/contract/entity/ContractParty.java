package com.javaweb.contract.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.contract.enums.ContractPartyRole;
import com.javaweb.customer.entity.Customer;
import com.javaweb.property.entity.AuditableEntity;
import jakarta.persistence.CascadeType;
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

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contract_parties")
public class ContractParty extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_role", nullable = false, length = 30)
    private ContractPartyRole partyRole;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "identity_number", length = 100)
    private String identityNumber;

    @Column(name = "tax_number", length = 100)
    private String taxNumber;

    @Column(length = 500)
    private String address;

    @Column(name = "signing_order", nullable = false)
    private int signingOrder = 1;

    @Column(name = "required_signer", nullable = false)
    private boolean requiredSigner = true;

    @OneToMany(mappedBy = "contractParty", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractSignature> signatures = new ArrayList<>();

    protected ContractParty() {
    }

    public ContractParty(ContractPartyRole partyRole, String fullName) {
        this.partyRole = partyRole;
        this.fullName = fullName;
    }

    public ContractSignature addSignature(ContractSignature signature) {
        signatures.add(signature);
        signature.setContractParty(this);
        return signature;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public ContractPartyRole getPartyRole() {
        return partyRole;
    }

    public void setPartyRole(ContractPartyRole partyRole) {
        this.partyRole = partyRole;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdentityNumber() {
        return identityNumber;
    }

    public void setIdentityNumber(String identityNumber) {
        this.identityNumber = identityNumber;
    }

    public String getTaxNumber() {
        return taxNumber;
    }

    public void setTaxNumber(String taxNumber) {
        this.taxNumber = taxNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getSigningOrder() {
        return signingOrder;
    }

    public void setSigningOrder(int signingOrder) {
        this.signingOrder = signingOrder;
    }

    public boolean isRequiredSigner() {
        return requiredSigner;
    }

    public void setRequiredSigner(boolean requiredSigner) {
        this.requiredSigner = requiredSigner;
    }

    public List<ContractSignature> getSignatures() {
        return signatures;
    }
}
