package com.javaweb.listing.repository;

import com.javaweb.listing.entity.ListingPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ListingPackageRepository extends JpaRepository<ListingPackage, Long> {
    Optional<ListingPackage> findByCode(String code);

    boolean existsByCode(String code);

    List<ListingPackage> findAllByActiveTrueOrderByPriorityLevelDescNameAsc();
}
