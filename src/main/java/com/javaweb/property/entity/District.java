package com.javaweb.property.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "districts")
public class District extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province_id", nullable = false)
    private Province province;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "administrative_type", length = 50)
    private String administrativeType;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "district", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Ward> wards = new ArrayList<>();

    protected District() {
    }

    public District(Province province, String code, String name) {
        this.province = province;
        this.code = code;
        this.name = name;
    }

    public void addWard(Ward ward) {
        wards.add(ward);
        ward.setDistrict(this);
    }

    public Long getId() {
        return id;
    }

    public Province getProvince() {
        return province;
    }

    void setProvince(Province province) {
        this.province = province;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdministrativeType() {
        return administrativeType;
    }

    public void setAdministrativeType(String administrativeType) {
        this.administrativeType = administrativeType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Ward> getWards() {
        return wards;
    }
}
