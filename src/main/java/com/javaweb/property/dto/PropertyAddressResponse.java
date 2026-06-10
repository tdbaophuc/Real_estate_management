package com.javaweb.property.dto;

import java.math.BigDecimal;

public record PropertyAddressResponse(
        Long id,
        Long provinceId,
        String provinceName,
        Long districtId,
        String districtName,
        Long wardId,
        String wardName,
        String streetAddress,
        String fullAddress,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
