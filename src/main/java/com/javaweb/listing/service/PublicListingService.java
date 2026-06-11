package com.javaweb.listing.service;

import com.javaweb.auth.entity.User;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.common.response.PageResponse;
import com.javaweb.listing.dto.ListingSearchRequest;
import com.javaweb.listing.dto.PublicListingResponse;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.entity.ListingView;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.enums.ListingVisibility;
import com.javaweb.listing.mapper.ListingMapper;
import com.javaweb.listing.repository.ListingRepository;
import com.javaweb.listing.repository.ListingSpecifications;
import com.javaweb.listing.repository.ListingViewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Service
public class PublicListingService {
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "price", "askingPrice",
            "askingPrice", "askingPrice",
            "publishedAt", "publishedAt",
            "createdAt", "createdAt",
            "viewCount", "viewCount"
    );

    private final ListingRepository listingRepository;
    private final ListingViewRepository listingViewRepository;
    private final UserRepository userRepository;
    private final ListingMapper listingMapper;

    public PublicListingService(
            ListingRepository listingRepository,
            ListingViewRepository listingViewRepository,
            UserRepository userRepository,
            ListingMapper listingMapper
    ) {
        this.listingRepository = listingRepository;
        this.listingViewRepository = listingViewRepository;
        this.userRepository = userRepository;
        this.listingMapper = listingMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<PublicListingResponse> search(ListingSearchRequest request) {
        String sortField = requireAllowedSortField(request.sortBy());
        Sort sort = Sort.by(request.direction(), sortField)
                .and(Sort.by(Sort.Direction.DESC, "id"));
        Page<Listing> page = listingRepository.findAll(
                ListingSpecifications.publicSearch(request),
                PageRequest.of(request.page(), request.size(), sort)
        );
        return PageResponse.from(
                page,
                page.getContent().stream()
                        .map(listingMapper::toPublicResponse)
                        .toList()
        );
    }

    @Transactional
    public PublicListingResponse getAndRecordView(
            String slug,
            AuthUserPrincipal principal,
            String sessionId,
            String remoteAddress,
            String userAgent,
            String referrer
    ) {
        Listing listing = requirePublicListingBySlug(slug);
        User viewer = principal == null
                ? null
                : userRepository.findById(principal.id()).orElse(null);
        ListingView view = listing.addView(viewer, truncate(sessionId, 100));
        view.setIpHash(hash(remoteAddress));
        view.setUserAgent(truncate(userAgent, 500));
        view.setReferrerUrl(truncate(referrer, 1000));
        listingViewRepository.save(view);
        listingRepository.incrementViewCount(listing.getId());

        Listing updated = requirePublicListingBySlug(slug);
        return listingMapper.toPublicResponse(updated);
    }

    @Transactional(readOnly = true)
    public Listing requirePublicListing(Long listingId) {
        Listing listing = listingRepository.findWithCoreDetailsById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Published listing not found"));
        if (!isPubliclyAvailable(listing)) {
            throw new ResourceNotFoundException("Published listing not found");
        }
        return listing;
    }

    private Listing requirePublicListingBySlug(String slug) {
        Listing listing = listingRepository
                .findBySlugAndStatusAndVisibilityAndDeletedAtIsNull(
                        slug,
                        ListingStatus.PUBLISHED,
                        ListingVisibility.PUBLIC
                )
                .orElseThrow(() -> new ResourceNotFoundException("Published listing not found"));
        if (listing.getProperty().getDeletedAt() != null) {
            throw new ResourceNotFoundException("Published listing not found");
        }
        return listing;
    }

    private boolean isPubliclyAvailable(Listing listing) {
        return listing.getStatus() == ListingStatus.PUBLISHED
                && listing.getVisibility() == ListingVisibility.PUBLIC
                && listing.getDeletedAt() == null
                && listing.getProperty().getDeletedAt() == null;
    }

    private String requireAllowedSortField(String sortBy) {
        String field = SORT_FIELDS.get(sortBy);
        if (field == null) {
            throw new BusinessException(
                    "sortBy must be one of: price, askingPrice, publishedAt, createdAt, viewCount"
            );
        }
        return field;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String hash(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
