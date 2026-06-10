package com.javaweb.listing.repository;

import com.javaweb.listing.entity.ListingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingStatusHistoryRepository extends JpaRepository<ListingStatusHistory, Long> {
    List<ListingStatusHistory> findAllByListingIdOrderByCreatedAtDesc(Long listingId);
}
