package com.javaweb.listing.repository;

import com.javaweb.listing.entity.ListingFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingFavoriteRepository extends JpaRepository<ListingFavorite, Long> {
    boolean existsByListingIdAndUserId(Long listingId, Long userId);

    long deleteByListingIdAndUserId(Long listingId, Long userId);

    Page<ListingFavorite> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
