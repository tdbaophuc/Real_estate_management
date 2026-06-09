package com.javaweb.property.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "provinces")
public class Province extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "administrative_type", length = 50)
    private String administrativeType;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "province", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<District> districts = new ArrayList<>();

    protected Province() {
    }

    public Province(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public void addDistrict(District district) {
        districts.add(district);
        district.setProvince(this);
    }

    public Long getId() {
        return id;
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

    public List<District> getDistricts() {
        return districts;
    }
}
