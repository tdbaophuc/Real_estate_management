package com.javaweb.storage.dto;

import com.javaweb.storage.enums.FileAccessLevel;
import jakarta.validation.constraints.NotNull;

public record FileAccessLevelUpdateRequest(
        @NotNull FileAccessLevel accessLevel
) {
}
