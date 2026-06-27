package com.javaweb.storage.controller;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.storage.dto.FileAccessLevelUpdateRequest;
import com.javaweb.storage.dto.FileDownloadResult;
import com.javaweb.storage.dto.FileResourceResponse;
import com.javaweb.storage.enums.FileAccessLevel;
import com.javaweb.storage.service.FileResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
public class FileController {
    private final FileResourceService fileResourceService;

    public FileController(FileResourceService fileResourceService) {
        this.fileResourceService = fileResourceService;
    }

    @Operation(summary = "Get file metadata")
    @GetMapping("/{fileId}")
    public ApiResponse<FileResourceResponse> get(
            @PathVariable Long fileId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(fileResourceService.get(fileId, actor));
    }

    @Operation(summary = "Download a file or redirect to a safe direct/signed URL")
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long fileId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        FileDownloadResult download = fileResourceService.download(fileId, actor);
        if (download.redirect()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, download.redirectUrl())
                    .build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.file().getContentType()))
                .contentLength(download.file().getFileSize())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(download.file().getOriginalFileName())
                                .build()
                                .toString()
                )
                .body(download.resource());
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FileResourceResponse> upload(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "PRIVATE") FileAccessLevel accessLevel,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "File uploaded successfully",
                fileResourceService.upload(file, accessLevel, actor)
        );
    }

    @Operation(summary = "Delete an unlinked file")
    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long fileId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        fileResourceService.delete(fileId, actor);
    }

    @Operation(summary = "Change file access level")
    @PatchMapping("/{fileId}/access-level")
    public ApiResponse<FileResourceResponse> updateAccessLevel(
            @PathVariable Long fileId,
            @Valid @RequestBody FileAccessLevelUpdateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "File access level updated successfully",
                fileResourceService.updateAccessLevel(fileId, request.accessLevel(), actor)
        );
    }
}
