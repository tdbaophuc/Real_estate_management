package com.javaweb.storage.dto;

import com.javaweb.storage.entity.FileResource;
import org.springframework.core.io.Resource;

public record FileDownloadResult(
        FileResource file,
        Resource resource,
        String redirectUrl
) {
    public boolean redirect() {
        return redirectUrl != null && !redirectUrl.isBlank();
    }
}
