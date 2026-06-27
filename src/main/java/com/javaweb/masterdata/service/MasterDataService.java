package com.javaweb.masterdata.service;

import com.javaweb.lead.repository.LeadSourceRepository;
import com.javaweb.listing.repository.ListingPackageRepository;
import com.javaweb.masterdata.dto.AmenityOptionResponse;
import com.javaweb.masterdata.dto.LeadSourceOptionResponse;
import com.javaweb.masterdata.dto.ListingPackageOptionResponse;
import com.javaweb.masterdata.dto.LocationOptionResponse;
import com.javaweb.masterdata.dto.PropertyTypeOptionResponse;
import com.javaweb.property.enums.AmenityCategory;
import com.javaweb.property.repository.AmenityRepository;
import com.javaweb.property.repository.DistrictRepository;
import com.javaweb.property.repository.PropertyTypeRepository;
import com.javaweb.property.repository.ProvinceRepository;
import com.javaweb.property.repository.WardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MasterDataService {
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final AmenityRepository amenityRepository;
    private final ListingPackageRepository listingPackageRepository;
    private final LeadSourceRepository leadSourceRepository;

    public MasterDataService(
            ProvinceRepository provinceRepository,
            DistrictRepository districtRepository,
            WardRepository wardRepository,
            PropertyTypeRepository propertyTypeRepository,
            AmenityRepository amenityRepository,
            ListingPackageRepository listingPackageRepository,
            LeadSourceRepository leadSourceRepository
    ) {
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
        this.wardRepository = wardRepository;
        this.propertyTypeRepository = propertyTypeRepository;
        this.amenityRepository = amenityRepository;
        this.listingPackageRepository = listingPackageRepository;
        this.leadSourceRepository = leadSourceRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationOptionResponse> provinces() {
        return provinceRepository.findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(province -> new LocationOptionResponse(
                        province.getId(),
                        province.getCode(),
                        province.getName(),
                        province.getAdministrativeType(),
                        province.isActive()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LocationOptionResponse> districts(Long provinceId) {
        return districtRepository.findAllByProvinceIdAndActiveTrueOrderByNameAsc(provinceId)
                .stream()
                .map(district -> new LocationOptionResponse(
                        district.getId(),
                        district.getCode(),
                        district.getName(),
                        district.getAdministrativeType(),
                        district.isActive()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LocationOptionResponse> wards(Long districtId) {
        return wardRepository.findAllByDistrictIdAndActiveTrueOrderByNameAsc(districtId)
                .stream()
                .map(ward -> new LocationOptionResponse(
                        ward.getId(),
                        ward.getCode(),
                        ward.getName(),
                        ward.getAdministrativeType(),
                        ward.isActive()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PropertyTypeOptionResponse> propertyTypes() {
        return propertyTypeRepository.findAllByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(type -> new PropertyTypeOptionResponse(
                        type.getId(),
                        type.getCode(),
                        type.getName(),
                        type.getDescription(),
                        type.getDisplayOrder(),
                        type.isActive()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AmenityOptionResponse> amenities(AmenityCategory category) {
        return (category == null
                ? amenityRepository.findAllByActiveTrueOrderByDisplayOrderAsc()
                : amenityRepository.findAllByCategoryAndActiveTrueOrderByDisplayOrderAsc(category))
                .stream()
                .map(amenity -> new AmenityOptionResponse(
                        amenity.getId(),
                        amenity.getCode(),
                        amenity.getName(),
                        amenity.getCategory(),
                        amenity.getDescription(),
                        amenity.getDisplayOrder(),
                        amenity.isActive()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListingPackageOptionResponse> listingPackages() {
        return listingPackageRepository.findAllByActiveTrueOrderByPriorityLevelDescNameAsc()
                .stream()
                .map(item -> new ListingPackageOptionResponse(
                        item.getId(),
                        item.getCode(),
                        item.getName(),
                        item.getDescription(),
                        item.getPrice(),
                        item.getCurrency(),
                        item.getDurationDays(),
                        item.isFeatured(),
                        item.getPriorityLevel(),
                        item.isActive()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeadSourceOptionResponse> leadSources() {
        return leadSourceRepository.findAllByActiveTrueOrderByName()
                .stream()
                .map(source -> new LeadSourceOptionResponse(
                        source.getId(),
                        source.getCode(),
                        source.getName(),
                        source.getDescription(),
                        source.isActive()
                ))
                .toList();
    }
}
