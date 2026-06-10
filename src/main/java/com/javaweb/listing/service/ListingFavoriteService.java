package com.javaweb.listing.service;

import com.javaweb.auth.entity.User;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.common.response.PageResponse;
import com.javaweb.listing.dto.PublicListingResponse;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.entity.ListingFavorite;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.enums.ListingVisibility;
import com.javaweb.listing.mapper.ListingMapper;
import com.javaweb.listing.repository.ListingFavoriteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListingFavoriteService {
    private final ListingFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final PublicListingService publicListingService;
    private final ListingMapper listingMapper;

    public ListingFavoriteService(
            ListingFavoriteRepository favoriteRepository,
            UserRepository userRepository,
            PublicListingService publicListingService,
            ListingMapper listingMapper
    ) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.publicListingService = publicListingService;
        this.listingMapper = listingMapper;
    }

    @Transactional
    public PublicListingResponse favorite(Long listingId, AuthUserPrincipal principal) {
        Listing listing = publicListingService.requirePublicListing(listingId);
        User customer = requireUser(principal);
        if (!favoriteRepository.existsByListingIdAndUserId(listingId, customer.getId())) {
            favoriteRepository.saveAndFlush(new ListingFavorite(listing, customer));
        }
        return listingMapper.toPublicResponse(listing);
    }

    @Transactional
    public void unfavorite(Long listingId, AuthUserPrincipal principal) {
        favoriteRepository.deleteByListingIdAndUserId(listingId, principal.id());
    }

    @Transactional(readOnly = true)
    public PageResponse<PublicListingResponse> getMyFavorites(
            AuthUserPrincipal principal,
            int page,
            int size
    ) {
        Page<ListingFavorite> favorites = favoriteRepository
                .findAllByUserIdAndListingStatusAndListingVisibilityAndListingDeletedAtIsNullAndListingPropertyDeletedAtIsNullOrderByCreatedAtDesc(
                        principal.id(),
                        ListingStatus.PUBLISHED,
                        ListingVisibility.PUBLIC,
                        PageRequest.of(page, size)
                );
        return PageResponse.from(
                favorites,
                favorites.getContent().stream()
                        .map(ListingFavorite::getListing)
                        .map(listingMapper::toPublicResponse)
                        .toList()
        );
    }

    private User requireUser(AuthUserPrincipal principal) {
        return userRepository.findById(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
