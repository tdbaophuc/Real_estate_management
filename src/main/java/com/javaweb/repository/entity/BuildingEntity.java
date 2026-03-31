package com.javaweb.repository.entity;

import java.util.ArrayList;
import java.util.List;

public class BuildingEntity {
    private Integer id;
    private Integer districtId;
    private String code;
    private String name;
    private String ward;
    private String street;
    private String structure;
    private Integer numberOfBasement;
    private Integer floorArea;
    private String direction;
    private String level; // Map từ cột 'class' trong DB
    private Integer price;
    private String serviceFee;
    private String carFee;
    private String priceDescription;
    private String motorbikeFee;
    private String overtimeFee;
    private String electricity;
    private String deposit;
    private String payment;
    private String rentTime;
    private String decorationTime;
    private String brokerageFee;
    private String note;
    private String manageName;
    private String managePhone;
    private Integer status;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getDistrictId() { return districtId; }
    public void setDistrictId(Integer districtId) { this.districtId = districtId; }

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