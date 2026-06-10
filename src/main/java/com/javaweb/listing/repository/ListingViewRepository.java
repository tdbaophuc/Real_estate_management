package com.javaweb.listing.repository;

import com.javaweb.listing.entity.ListingView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingViewRepository extends JpaRepository<ListingView, Long> {
    long countByListingId(Long listingId);

    List<ListingView> findAllByListingIdOrderByViewedAtDesc(Long listingId);
}
