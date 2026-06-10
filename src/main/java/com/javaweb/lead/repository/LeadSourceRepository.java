package com.javaweb.lead.repository;

import com.javaweb.lead.entity.LeadSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeadSourceRepository extends JpaRepository<LeadSource, Long> {
    Optional<LeadSource> findByCodeAndActiveTrue(String code);

    List<LeadSource> findAllByActiveTrueOrderByName();
}
