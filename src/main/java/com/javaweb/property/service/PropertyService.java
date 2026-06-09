package com.javaweb.property.service;

import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.DuplicateResourceException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.common.response.PageResponse;
import com.javaweb.property.dto.PropertyAmenityRequest;
import com.javaweb.property.dto.PropertyResponse;
import com.javaweb.property.dto.PropertySearchRequest;
import com.javaweb.property.dto.PropertyUpsertRequest;
import com.javaweb.property.dto.UpdatePropertyStatusRequest;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Amenity;
import com.javaweb.property.entity.District;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyAmenity;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.entity.Ward;
import com.javaweb.property.enums.PropertyStatus;
import com.javaweb.property.mapper.PropertyMapper;
import com.javaweb.property.repository.AmenityRepository;
import com.javaweb.property.repository.DistrictRepository;
import com.javaweb.property.repository.PropertyRepository;
import com.javaweb.property.repository.PropertySpecifications;
import com.javaweb.property.repository.PropertyTypeRepository;
import com.javaweb.property.repository.ProvinceRepository;
import com.javaweb.property.repository.WardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PropertyService {
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "id",
                    "code",
                    "name",
                    "purpose",
                    "status",
                    "price",
                    "landArea",
                    "floorArea",
                    "bedrooms",
                    "bathrooms",
                    "createdAt",
                    "updatedAt"
            );
    private static final Map<PropertyStatus, Set<PropertyStatus>> ALLOWED_STATUS_TRANSITIONS =
            Map.of(
                    PropertyStatus.DRAFT, Set.of(PropertyStatus.AVAILABLE, PropertyStatus.INACTIVE),
                    PropertyStatus.AVAILABLE, Set.of(
                            PropertyStatus.RESERVED,
                            PropertyStatus.SOLD,
                            PropertyStatus.RENTED,
                            PropertyStatus.INACTIVE
                    ),
                    PropertyStatus.RESERVED, Set.of(
                            PropertyStatus.AVAILABLE,
                            PropertyStatus.SOLD,
                            PropertyStatus.RENTED,
                            PropertyStatus.INACTIVE
                    ),
                    PropertyStatus.SOLD, Set.of(PropertyStatus.INACTIVE),
                    PropertyStatus.RENTED, Set.of(PropertyStatus.AVAILABLE, PropertyStatus.INACTIVE),
                    PropertyStatus.INACTIVE, Set.of(PropertyStatus.DRAFT, PropertyStatus.AVAILABLE),
                    PropertyStatus.DELETED, Set.of()
            );

    private final PropertyRepository propertyRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final AmenityRepository amenityRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final UserRepository userRepository;
    private final PropertyMapper propertyMapper;

    public PropertyService(
            PropertyRepository propertyRepository,
            PropertyTypeRepository propertyTypeRepository,
            AmenityRepository amenityRepository,
            ProvinceRepository provinceRepository,
            DistrictRepository districtRepository,
            WardRepository wardRepository,
            UserRepository userRepository,
            PropertyMapper propertyMapper
    ) {
        this.propertyRepository = propertyRepository;
        this.propertyTypeRepository = propertyTypeRepository;
        this.amenityRepository = amenityRepository;
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
        this.wardRepository = wardRepository;
        this.userRepository = userRepository;
        this.propertyMapper = propertyMapper;
    }

    @Transactional
    public PropertyResponse create(PropertyUpsertRequest request, AuthUserPrincipal actor) {
        if (propertyRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Property code already exists");
        }

        User creator = requireUser(actor.id(), "Authenticated user not found");
        PropertyType propertyType = requirePropertyType(request.propertyTypeId());
        Location location = requireLocation(
                request.address().provinceId(),
                request.address().districtId(),
                request.address().wardId()
        );
        User owner = requireOptionalUserWithRole(request.ownerId(), RoleCode.OWNER, "Owner");
        User assignedAgent = resolveAssignedAgent(request.assignedAgentId(), actor);
        Map<Long, AmenitySelection> amenities = requireAmenities(request.amenities());

        Address address = propertyMapper.toAddress(
                request.address(),
                location.province(),
                location.district(),
                location.ward()
        );
        Property property = propertyMapper.toEntity(
                request,
                propertyType,
                address,
                creator
        );
        property.setOwner(owner);
        property.setAssignedAgent(assignedAgent);
        addAmenities(property, amenities);

        return propertyMapper.toResponse(propertyRepository.saveAndFlush(property));
    }

    @Transactional
    public PropertyResponse update(
            Long propertyId,
            PropertyUpsertRequest request,
            AuthUserPrincipal actor
    ) {
        Property property = propertyRepository.findWithUpdateDetailsById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        requireCanModify(property, actor);
        if (property.getDeletedAt() != null) {
            throw new BusinessException("Deleted properties cannot be updated");
        }
        if (propertyRepository.existsByCodeAndIdNot(request.code(), propertyId)) {
            throw new DuplicateResourceException("Property code already exists");
        }

        PropertyType propertyType = requirePropertyType(request.propertyTypeId());
        Location location = requireLocation(
                request.address().provinceId(),
                request.address().districtId(),
                request.address().wardId()
        );
        User owner = requireOptionalUserWithRole(request.ownerId(), RoleCode.OWNER, "Owner");
        User assignedAgent = resolveAssignedAgent(request.assignedAgentId(), actor);
        Map<Long, AmenitySelection> amenities = requireAmenities(request.amenities());

        propertyMapper.updateEntity(property, request, propertyType);
        propertyMapper.updateAddress(
                property.getAddress(),
                request.address(),
                location.province(),
                location.district(),
                location.ward()
        );
        property.setOwner(owner);
        property.setAssignedAgent(assignedAgent);
        reconcileAmenities(property, amenities);

        return propertyMapper.toResponse(propertyRepository.saveAndFlush(property));
    }

    @Transactional(readOnly = true)
    public PageResponse<PropertyResponse> search(
            PropertySearchRequest request,
            AuthUserPrincipal actor
    ) {
        String safeSortBy = requireAllowedSortField(request.sortBy());
        PageRequest pageable = PageRequest.of(
                request.page(),
                request.size(),
                Sort.by(request.direction(), safeSortBy)
        );
        Long visibleUserId = isManagerOrAdmin(actor) ? null : actor.id();
        Page<Property> propertyPage = propertyRepository.findAll(
                PropertySpecifications.search(request, visibleUserId),
                pageable
        );

        List<Long> ids = propertyPage.getContent().stream()
                .map(Property::getId)
                .toList();
        Map<Long, Property> propertiesById = ids.isEmpty()
                ? Map.of()
                : propertyRepository.findAllWithDetailsByIdIn(ids).stream()
                        .collect(Collectors.toMap(
                                Property::getId,
                                Function.identity(),
                                (left, right) -> left,
                                LinkedHashMap::new
                        ));
        List<PropertyResponse> content = ids.stream()
                .map(propertiesById::get)
                .map(propertyMapper::toResponse)
                .toList();

        return PageResponse.from(propertyPage, content);
    }

    @Transactional(readOnly = true)
    public PropertyResponse get(Long propertyId, AuthUserPrincipal actor) {
        Property property = propertyRepository.findActiveDetailsById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        requireCanRead(property, actor);
        return propertyMapper.toResponse(property);
    }

    @Transactional
    public void delete(Long propertyId, AuthUserPrincipal actor) {
        Property property = requireActivePropertyForModification(propertyId);
        requireCanModify(property, actor);
        property.setStatus(PropertyStatus.DELETED);
        property.setDeletedAt(Instant.now());
    }

    @Transactional
    public PropertyResponse updateStatus(
            Long propertyId,
            UpdatePropertyStatusRequest request,
            AuthUserPrincipal actor
    ) {
        Property property = requireActivePropertyForModification(propertyId);
        requireCanModify(property, actor);
        PropertyStatus requestedStatus = request.status();
        if (requestedStatus == PropertyStatus.DELETED) {
            throw new BusinessException("Use the delete endpoint to delete a property");
        }
        if (property.getStatus() != requestedStatus
                && !ALLOWED_STATUS_TRANSITIONS.get(property.getStatus()).contains(requestedStatus)) {
            throw new BusinessException(
                    "Property status cannot transition from "
                            + property.getStatus()
                            + " to "
                            + requestedStatus
            );
        }
        property.setStatus(requestedStatus);
        return propertyMapper.toResponse(property);
    }

    private PropertyType requirePropertyType(Long propertyTypeId) {
        return propertyTypeRepository.findByIdAndActiveTrue(propertyTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Active property type not found"));
    }

    private Location requireLocation(Long provinceId, Long districtId, Long wardId) {
        Province province = provinceRepository.findByIdAndActiveTrue(provinceId)
                .orElseThrow(() -> new ResourceNotFoundException("Active province not found"));
        District district = districtId == null
                ? null
                : districtRepository.findByIdAndActiveTrue(districtId)
                        .orElseThrow(() -> new ResourceNotFoundException("Active district not found"));
        Ward ward = wardId == null
                ? null
                : wardRepository.findByIdAndActiveTrue(wardId)
                        .orElseThrow(() -> new ResourceNotFoundException("Active ward not found"));

        if (ward != null && district == null) {
            throw new BusinessException("wardId requires districtId");
        }
        if (district != null && !district.getProvince().getId().equals(province.getId())) {
            throw new BusinessException("District does not belong to the selected province");
        }
        if (ward != null && !ward.getDistrict().getId().equals(district.getId())) {
            throw new BusinessException("Ward does not belong to the selected district");
        }
        return new Location(province, district, ward);
    }

    private User resolveAssignedAgent(
            Long requestedAgentId,
            AuthUserPrincipal actor
    ) {
        if (isAgentOnly(actor)) {
            if (requestedAgentId != null && !requestedAgentId.equals(actor.id())) {
                throw new AccessDeniedException("Agents cannot assign properties to another agent");
            }
            return requireUserWithRole(actor.id(), RoleCode.AGENT, "Agent");
        }
        return requireOptionalUserWithRole(requestedAgentId, RoleCode.AGENT, "Assigned agent");
    }

    private Map<Long, AmenitySelection> requireAmenities(List<PropertyAmenityRequest> requests) {
        Map<Long, PropertyAmenityRequest> requestsById = new LinkedHashMap<>();
        for (PropertyAmenityRequest request : requests) {
            if (requestsById.putIfAbsent(request.amenityId(), request) != null) {
                throw new BusinessException("Amenities must not contain duplicate amenityId values");
            }
        }
        if (requestsById.isEmpty()) {
            return Map.of();
        }

        Map<Long, Amenity> amenitiesById = amenityRepository
                .findAllByIdInAndActiveTrue(requestsById.keySet())
                .stream()
                .collect(Collectors.toMap(Amenity::getId, Function.identity()));
        if (amenitiesById.size() != requestsById.size()) {
            throw new ResourceNotFoundException("One or more active amenities were not found");
        }

        Map<Long, AmenitySelection> selections = new LinkedHashMap<>();
        requestsById.forEach((id, request) ->
                selections.put(id, new AmenitySelection(amenitiesById.get(id), request.details()))
        );
        return selections;
    }

    private User requireOptionalUserWithRole(Long userId, RoleCode role, String label) {
        return userId == null ? null : requireUserWithRole(userId, role, label);
    }

    private User requireUserWithRole(Long userId, RoleCode role, String label) {
        User user = requireUser(userId, label + " not found");
        boolean hasRole = user.getRoles().stream()
                .map(Role::getCode)
                .anyMatch(role::equals);
        if (!hasRole) {
            throw new BusinessException(label + " must have the " + role + " role");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(label + " must be active");
        }
        return user;
    }

    private User requireUser(Long userId, String message) {
        return userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(message));
    }

    private Property requireActivePropertyForModification(Long propertyId) {
        Property property = propertyRepository.findWithUpdateDetailsById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        if (property.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Property not found");
        }
        return property;
    }

    private String requireAllowedSortField(String sortBy) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BusinessException("Unsupported property sort field");
        }
        return sortBy;
    }

    private void requireCanRead(Property property, AuthUserPrincipal actor) {
        if (isManagerOrAdmin(actor)) {
            return;
        }
        boolean createdByActor = property.getCreatedBy().getId().equals(actor.id());
        boolean assignedToActor = property.getAssignedAgent() != null
                && property.getAssignedAgent().getId().equals(actor.id());
        if (!createdByActor && !assignedToActor) {
            throw new AccessDeniedException(
                    "Agents can only view properties they created or are assigned"
            );
        }
    }

    private void requireCanModify(Property property, AuthUserPrincipal actor) {
        if (isManagerOrAdmin(actor)) {
            return;
        }
        if (!property.getCreatedBy().getId().equals(actor.id())) {
            throw new AccessDeniedException("Agents can only modify properties they created");
        }
    }

    private boolean isManagerOrAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.ADMIN.name())
                || actor.roles().contains(RoleCode.MANAGER.name());
    }

    private boolean isAgentOnly(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.AGENT.name()) && !isManagerOrAdmin(actor);
    }

    private void addAmenities(Property property, Map<Long, AmenitySelection> selections) {
        selections.values().forEach(selection ->
                property.addAmenity(selection.amenity(), selection.details())
        );
    }

    private void reconcileAmenities(
            Property property,
            Map<Long, AmenitySelection> requestedAmenities
    ) {
        Map<Long, PropertyAmenity> currentByAmenityId = property.getAmenities().stream()
                .collect(Collectors.toMap(link -> link.getAmenity().getId(), Function.identity()));

        List<PropertyAmenity> removed = currentByAmenityId.entrySet().stream()
                .filter(entry -> !requestedAmenities.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        new ArrayList<>(removed).forEach(property::removeAmenity);

        requestedAmenities.forEach((amenityId, selection) -> {
            PropertyAmenity current = currentByAmenityId.get(amenityId);
            if (current == null) {
                property.addAmenity(selection.amenity(), selection.details());
            } else {
                current.setDetails(selection.details());
            }
        });
    }

    private record Location(Province province, District district, Ward ward) {
    }

    private record AmenitySelection(Amenity amenity, String details) {
    }
}
