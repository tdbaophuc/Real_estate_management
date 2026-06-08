package com.javaweb.repository.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "building")
public class BuildingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "ward")
    private String ward;

    @Column(name = "street")
    private String street;

    @Column(name = "structure")
    private String structure;

    @Column(name = "numberOfBasement")
    private Integer numberOfBasement;

    @Column(name = "floorArea")
    private Integer floorArea;

    @Column(name = "direction")
    private String direction;

    @Column(name = "class")
    private String level; // Map từ cột 'class' trong DB

    @Column(name = "price")
    private Integer price;

    @Column(name = "serviceFee")
    private String serviceFee;

    @Column(name = "carFee")
    private String carFee;

    @Column(name = "priceDescription")
    private String priceDescription;

    @Column(name = "motorbikeFee")
    private String motorbikeFee;

    @Column(name = "overtimeFee")
    private String overtimeFee;

    @Column(name = "electricity")
    private String electricity;

    @Column(name = "deposit")
    private String deposit;

    @Column(name = "payment")
    private String payment;

    @Column(name = "rentTime")
    private String rentTime;

    @Column(name = "decorationTime")
    private String decorationTime;

    @Column(name = "brokerageFee")
    private String brokerageFee;

    @Column(name = "note")
    private String note;

    @Column(name = "manageName")
    private String manageName;

    @Column(name = "managePhone")
    private String managePhone;

    @Column(name = "status")
    private Integer status;

    @ManyToOne
    @JoinColumn(name = "district_id")
    private DistrictEntity district;

    @OneToMany(mappedBy = "building", fetch = FetchType.LAZY)
    private List<RentAreaEntity> rentArea = new ArrayList<>();

    public DistrictEntity getDistrict() {
        return district;
    }

    public void setDistrict(DistrictEntity district) {
        this.district = district;
    }

    public List<RentAreaEntity> getRentArea() {
        return rentArea;
    }

    public void setRentArea(List<RentAreaEntity> rentArea) {
        this.rentArea = rentArea;
    }


    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getStructure() { return structure; }
    public void setStructure(String structure) { this.structure = structure; }

    public Integer getNumberOfBasement() { return numberOfBasement; }
    public void setNumberOfBasement(Integer numberOfBasement) { this.numberOfBasement = numberOfBasement; }

    public Integer getFloorArea() { return floorArea; }
    public void setFloorArea(Integer floorArea) { this.floorArea = floorArea; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public String getServiceFee() { return serviceFee; }
    public void setServiceFee(String serviceFee) { this.serviceFee = serviceFee; }

    public String getCarFee() { return carFee; }
    public void setCarFee(String carFee) { this.carFee = carFee; }

    public String getPriceDescription() { return priceDescription; }
    public void setPriceDescription(String priceDescription) { this.priceDescription = priceDescription; }

    public String getMotorbikeFee() { return motorbikeFee; }
    public void setMotorbikeFee(String motorbikeFee) { this.motorbikeFee = motorbikeFee; }

    public String getOvertimeFee() { return overtimeFee; }
    public void setOvertimeFee(String overtimeFee) { this.overtimeFee = overtimeFee; }

    public String getElectricity() { return electricity; }
    public void setElectricity(String electricity) { this.electricity = electricity; }

    public String getDeposit() { return deposit; }
    public void setDeposit(String deposit) { this.deposit = deposit; }

    public String getPayment() { return payment; }
    public void setPayment(String payment) { this.payment = payment; }

    public String getRentTime() { return rentTime; }
    public void setRentTime(String rentTime) { this.rentTime = rentTime; }

    public String getDecorationTime() { return decorationTime; }
    public void setDecorationTime(String decorationTime) { this.decorationTime = decorationTime; }

    public String getBrokerageFee() { return brokerageFee; }
    public void setBrokerageFee(String brokerageFee) { this.brokerageFee = brokerageFee; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getManageName() {
        return manageName;
    }

    public void setManageName(String manageName) {
        this.manageName = manageName;
    }

    public String getManagePhone() {
        return managePhone;
    }

    public void setManagePhone(String managePhone) {
        this.managePhone = managePhone;
    }

}