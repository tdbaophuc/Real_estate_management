package com.javaweb.customer.mapper;

import com.javaweb.auth.entity.User;
import com.javaweb.customer.dto.CustomerNoteResponse;
import com.javaweb.customer.dto.CustomerRequirementResponse;
import com.javaweb.customer.dto.CustomerResponse;
import com.javaweb.customer.dto.CustomerUpsertRequest;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.entity.CustomerNote;
import com.javaweb.customer.entity.CustomerRequirement;
import com.javaweb.property.entity.District;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.entity.Ward;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toEntity(CustomerUpsertRequest request, User creator) {
        Customer customer = new Customer(request.code(), request.fullName(), creator);
        applyScalars(customer, request);
        return customer;
    }

    public void updateEntity(Customer customer, CustomerUpsertRequest request) {
        customer.setCode(request.code());
        customer.setFullName(request.fullName());
        applyScalars(customer, request);
    }

    public CustomerResponse toResponse(Customer customer) {
        User user = customer.getUser();
        User assignedAgent = customer.getAssignedAgent();
        User creator = customer.getCreatedBy();
        return new CustomerResponse(
                customer.getId(),
                customer.getCode(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getStatus(),
                customer.getSource(),
                customer.getPriority(),
                customer.getPreferredContactMethod(),
                customer.getNotes(),
                user == null ? null : user.getId(),
                user == null ? null : user.getFullName(),
                assignedAgent == null ? null : assignedAgent.getId(),
                assignedAgent == null ? null : assignedAgent.getFullName(),
                creator.getId(),
                creator.getFullName(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    public CustomerNoteResponse toNoteResponse(CustomerNote note) {
        return new CustomerNoteResponse(
                note.getId(),
                note.getCustomer().getId(),
                note.getAuthor().getId(),
                note.getAuthor().getFullName(),
                note.getContent(),
                note.isPinned(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }

    public CustomerRequirementResponse toRequirementResponse(
            CustomerRequirement requirement
    ) {
        PropertyType propertyType = requirement.getPropertyType();
        Province province = requirement.getProvince();
        District district = requirement.getDistrict();
        Ward ward = requirement.getWard();
        return new CustomerRequirementResponse(
                requirement.getId(),
                requirement.getCustomer().getId(),
                requirement.getPurpose(),
                propertyType == null ? null : propertyType.getId(),
                propertyType == null ? null : propertyType.getName(),
                province == null ? null : province.getId(),
                province == null ? null : province.getName(),
                district == null ? null : district.getId(),
                district == null ? null : district.getName(),
                ward == null ? null : ward.getId(),
                ward == null ? null : ward.getName(),
                requirement.getMinBudget(),
                requirement.getMaxBudget(),
                requirement.getCurrency(),
                requirement.getMinArea(),
                requirement.getMaxArea(),
                requirement.getMinBedrooms(),
                requirement.getMinBathrooms(),
                requirement.getDescription(),
                requirement.isActive(),
                requirement.getCreatedAt(),
                requirement.getUpdatedAt()
        );
    }

    private void applyScalars(Customer customer, CustomerUpsertRequest request) {
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setStatus(request.status());
        customer.setSource(request.source());
        customer.setPriority(request.priority());
        customer.setPreferredContactMethod(request.preferredContactMethod());
        customer.setNotes(request.notes());
    }
}
