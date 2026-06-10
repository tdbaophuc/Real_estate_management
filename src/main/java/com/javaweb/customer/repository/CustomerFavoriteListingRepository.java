package com.javaweb.customer.repository;

import com.javaweb.customer.entity.CustomerFavoriteListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerFavoriteListingRepository
        extends JpaRepository<CustomerFavoriteListing, Long> {
    boolean existsByCustomerIdAndListingId(Long customerId, Long listingId);

    long deleteByCustomerIdAndListingId(Long customerId, Long listingId);

    Page<CustomerFavoriteListing> findAllByCustomerIdOrderByCreatedAtDesc(
            Long customerId,
            Pageable pageable
    );
}
