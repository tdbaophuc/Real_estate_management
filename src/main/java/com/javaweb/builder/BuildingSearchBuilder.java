package com.javaweb.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuildingSearchBuilder {

    private String name;
    private String ward;
    private String street;
    private Integer numberOfBasement;
    private String direction;
    private Integer floorArea;
    private String level;
    private String manageName;
    private String managePhone;
    private Integer priceFrom;
    private Integer priceTo;
    private Integer districtId;
    private Integer staff_id;
    private String rentAreaFrom;
    private String rentAreaTo;
    private List<String> typeCode = new ArrayList<>();

    private BuildingSearchBuilder(Builder builder){
        this.name=builder.name;
        this.ward=builder.ward;
        this.street=builder.street;
        this.numberOfBasement=builder.numberOfBasement;
        this.direction=builder.direction;
        this.floorArea= builder.floorArea;
        this.level= builder.level;
        this.manageName= builder.manageName;
        this.managePhone= builder.managePhone;
        this.priceFrom= builder.priceFrom;
        this.priceTo= builder.priceTo;
        this.districtId=builder.districtId;
        this.staff_id= builder.staff_id;
        this.rentAreaFrom= builder.rentAreaFrom;
        this.rentAreaTo= builder.rentAreaTo;
        this.typeCode=builder.typeCode;
    };

    public String getName() {
        return name;
    }

    public String getWard() {
        return ward;
    }

    public String getStreet() {
        return street;
    }

    public Integer getNumberOfBasement() {
        return numberOfBasement;
    }

    public String getDirection() {
        return direction;
    }

    public Integer getFloorArea() {
        return floorArea;
    }

    public String getLevel() {
        return level;
    }

    public String getManageName() {
        return manageName;
    }

    public String getManagePhone() {
        return managePhone;
    }

    public Integer getPriceFrom() {
        return priceFrom;
    }

    public Integer getPriceTo() {
        return priceTo;
    }

    public Integer getDistrictId() {
        return districtId;
    }

    public Integer getStaff_id() {
        return staff_id;
    }

    public String getRentAreaFrom() {
        return rentAreaFrom;
    }

    public String getRentAreaTo() {
        return rentAreaTo;
    }

    public List<String> getTypeCode() {
        return typeCode;
    }

    public static class Builder{
        private String name;
        private String ward;
        private String street;
        private Integer numberOfBasement;
        private String direction;
        private Integer floorArea;
        private String level;
        private String manageName;
        private String managePhone;
        private Integer priceFrom;
        private Integer priceTo;
        private Integer districtId;
        private Integer staff_id;
        private String rentAreaFrom;
        private String rentAreaTo;
        private List<String> typeCode = new ArrayList<>();

        public Builder setPriceFrom(Integer priceFrom) {
            this.priceFrom = priceFrom;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setWard(String ward) {
            this.ward = ward;
            return this;
        }

        public Builder setStreet(String street) {
            this.street = street;
            return this;
        }

        public Builder setNumberOfBasement(Integer numberOfBasement) {
            this.numberOfBasement = numberOfBasement;
            return this;
        }

        public Builder setDirection(String direction) {
            this.direction = direction;
            return this;
        }

        public Builder setFloorArea(Integer floorArea) {
            this.floorArea = floorArea;
            return this;
        }

        public Builder setLevel(String level) {
            this.level = level;
            return this;
        }

        public Builder setManageName(String manageName) {
            this.manageName = manageName;
            return this;
        }

        public Builder setManagePhone(String managePhone) {
            this.managePhone = managePhone;
            return this;
        }

        public Builder setPriceTo(Integer priceTo) {
            this.priceTo = priceTo;
            return this;
        }

        public Builder setDistrictId(Integer districtId) {
            this.districtId = districtId;
            return this;
        }

        public Builder setStaff_id(Integer staff_id) {
            this.staff_id = staff_id;
            return this;
        }

        public Builder setRentAreaFrom(String rentAreaFrom) {
            this.rentAreaFrom = rentAreaFrom;
            return this;
        }

        public Builder setRentAreaTo(String rentAreaTo) {
            this.rentAreaTo = rentAreaTo;
            return this;
        }

        public Builder setTypeCode(List<String> typeCode) {
            this.typeCode = typeCode;
            return this;
        }

        public BuildingSearchBuilder build(){
            return new BuildingSearchBuilder(this);
        }
    };
}
