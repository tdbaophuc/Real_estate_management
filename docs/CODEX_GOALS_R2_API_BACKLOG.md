# Codex Goals - R2 Storage and API Missing Backlog

Ngay lap: 2026-06-27

File nay dung de copy tung goal vao Codex khi trien khai backlog trong
`docs/API_MISSING_BACKLOG.md`. Nen chay theo thu tu ben duoi, moi goal phai
ket thuc bang test va cap nhat tai lieu API.

## 0. Thong tin R2 can chuan bi

Khong dua secret truc tiep vao prompt, commit, README, hay file `.yml`. Hay dua
vao `.env` local hoac environment variables khi chay app.

Can cac gia tri:

```env
R2_ACCOUNT_ID=<cloudflare-account-id>
R2_API_TOKEN=<cloudflare-api-token-for-admin-or-bucket-ops-if-needed>
R2_ACCESS_KEY_ID=<r2-s3-access-key-id>
R2_SECRET_ACCESS_KEY=<r2-s3-secret-access-key>
R2_BUCKET=<bucket-name>
R2_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
R2_PUBLIC_BASE_URL=<optional-public-domain-or-r2-dev-url>
R2_PRESIGNED_URL_TTL=PT10M
STORAGE_PROVIDER=r2
```

Ghi chu:

- Upload/download object nen dung S3-compatible API cua R2 voi access key id va
  secret access key.
- `R2_API_TOKEN` chi can neu code can goi Cloudflare API quan tri bucket. Upload
  file binh thuong khong nen can token nay.
- Neu file PRIVATE, API backend phai check quyen roi tra stream hoac signed URL
  ngan han, khong tra public URL truc tiep.
- Neu file PUBLIC, co the tra `publicUrl` dua tren `R2_PUBLIC_BASE_URL`.

## 1. Goal R2 storage provider

Copy goal nay truoc tien.

```text
/goal Implement Cloudflare R2 storage as a first-class storage provider for this Spring Boot project.

Context:
- Read docs/API_MISSING_BACKLOG.md sections 3.3, 6.1, 6.2, 12.3, and 18.
- Current storage is local-only under src/main/java/com/javaweb/storage.
- Keep local storage working for tests and local development.
- Use Cloudflare R2 through its S3-compatible API. Do not hardcode credentials.

Requirements:
1. Add the minimal AWS SDK for S3 dependency needed by the project.
2. Extend app.storage configuration to support provider=local|r2, bucket, endpoint, region, access key, secret key, public base URL, and signed URL TTL.
3. Implement an R2/S3 FileStorageService that supports store, load/download, delete, SHA-256 checksum, public URL for PUBLIC files, and private object handling.
4. Make StorageConfig select LocalFileStorageService or R2FileStorageService based on app.storage.provider.
5. Preserve current FileResource data model where possible: storageProvider, storageKey, checksum, publicUrl, accessLevel.
6. Add safe validation for file type, size, empty file, and suspicious filenames.
7. Update application.yml, application-dev.yml if needed, .env.example, and docs/API_FRONTEND_REFERENCE.md.
8. Add tests that keep local storage as default and verify R2 configuration/service behavior without contacting Cloudflare.

Verification:
- Run mvn -Dtest=FileUploadIntegrationTest test.
- Run mvn -Dtest=LocalDevelopmentConfigurationTest test.
- Run mvn -DskipTests compile.
```

## 2. Goal file detail/download/delete APIs

```text
/goal Implement missing file resource APIs backed by the storage abstraction.

Context:
- Read docs/API_MISSING_BACKLOG.md section 18.
- R2 provider must already exist or be implemented in this goal if missing.

Endpoints:
- GET /api/v1/files/{fileId}
- GET /api/v1/files/{fileId}/download
- DELETE /api/v1/files/{fileId}
- PATCH /api/v1/files/{fileId}/access-level

Requirements:
1. Return metadata for authorized users.
2. Download PUBLIC files using direct URL when safe; PRIVATE files must enforce authorization and return stream or short-lived signed URL.
3. Delete object from storage and mark/delete FileResource according to existing repository style.
4. Do not allow deleting files linked to signed contracts or verified legal documents unless code already has a safe admin-force pattern.
5. Add audit actions for file download/delete/access-level changes.
6. Add OpenAPI annotations and update docs/API_FRONTEND_REFERENCE.md.
7. Add integration tests for upload, metadata, download, forbidden, not found, delete, and access-level update.

Verification:
- Run mvn -Dtest=FileUploadIntegrationTest test.
- Run mvn -DskipTests compile.
```

## 3. Goal current account APIs with avatar

```text
/goal Implement current account self-service APIs, including avatar upload stored through FileResource/R2.

Context:
- Read docs/API_MISSING_BACKLOG.md section 3.
- Current auth controller has GET /api/v1/auth/me.

Endpoints:
- PATCH /api/v1/auth/me/profile
- POST /api/v1/auth/me/change-password
- POST /api/v1/auth/me/avatar
- DELETE /api/v1/auth/me/avatar
- GET /api/v1/auth/me/sessions
- DELETE /api/v1/auth/me/sessions/{sessionId}
- DELETE /api/v1/auth/me/sessions

Requirements:
1. Do not allow self-update of email, roles, or status.
2. Validate phone uniqueness if the schema enforces it.
3. Avatar must use FileResourceService and the configured storage provider, including R2.
4. Change password must verify current password, validate strong password, and revoke refresh tokens according to existing token model.
5. Session APIs must only affect the current user's sessions.
6. Add audit actions and OpenAPI docs.
7. Update docs/API_FRONTEND_REFERENCE.md.
8. Add integration tests for happy path, validation, forbidden/not owner session, and token revocation behavior.

Verification:
- Run mvn -Dtest=AuthFlowIntegrationTest test.
- Run mvn -Dtest=SecurityHardeningIntegrationTest test.
- Run mvn -DskipTests compile.
```

## 4. Goal master data read APIs

```text
/goal Implement public/bearer master-data read APIs for frontend dropdowns.

Context:
- Read docs/API_MISSING_BACKLOG.md section 5.
- Existing repositories include ProvinceRepository, DistrictRepository, WardRepository, PropertyTypeRepository, AmenityRepository, ListingPackageRepository, and LeadSourceRepository.

Endpoints:
- GET /api/v1/master-data/provinces
- GET /api/v1/master-data/provinces/{provinceId}/districts
- GET /api/v1/master-data/districts/{districtId}/wards
- GET /api/v1/master-data/property-types
- GET /api/v1/master-data/amenities
- GET /api/v1/master-data/listing-packages
- GET /api/v1/master-data/lead-sources

Requirements:
1. Use DTOs, not entities.
2. Support active-only defaults where the schema has active flags.
3. Add simple filters where backlog requests them, for example amenity category.
4. Keep public endpoints safe and read-only.
5. Add OpenAPI docs and update docs/API_FRONTEND_REFERENCE.md.
6. Add integration tests for each endpoint.

Verification:
- Run mvn -Dtest=PropertyRepositoryIntegrationTest test.
- Run mvn -Dtest=LeadManagementIntegrationTest test.
- Run mvn -DskipTests compile.
```

## 5. Goal property images and legal documents

```text
/goal Implement missing property file/image APIs using FileResource and Cloudflare R2 compatible storage.

Context:
- Read docs/API_MISSING_BACKLOG.md sections 6.1 and 6.2.
- Existing property image upload/list/delete/set-cover APIs already exist.
- Existing PropertyLegalDocument entity and repository exist.

Endpoints:
- GET /api/v1/properties/{propertyId}/legal-documents
- POST /api/v1/properties/{propertyId}/legal-documents
- GET /api/v1/properties/{propertyId}/legal-documents/{documentId}
- PATCH /api/v1/properties/{propertyId}/legal-documents/{documentId}
- PATCH /api/v1/properties/{propertyId}/legal-documents/{documentId}/verify
- DELETE /api/v1/properties/{propertyId}/legal-documents/{documentId}
- PATCH /api/v1/properties/{propertyId}/images/{imageId}
- PUT /api/v1/properties/{propertyId}/images/reorder

Requirements:
1. Legal document upload must store the file via FileResourceService and configured storage provider.
2. PRIVATE should be default for legal documents.
3. Image upload remains PUBLIC unless existing behavior says otherwise.
4. Enforce roles: AGENT/MANAGER/ADMIN for upload/read/update, MANAGER/ADMIN for verify/delete.
5. Implement authorization checks against the property resource.
6. Add audit actions for document upload/update/verify/delete and image metadata/reorder.
7. Add OpenAPI docs and update docs/API_FRONTEND_REFERENCE.md.
8. Add integration tests for happy path, forbidden, not found, validation, and R2-neutral storage behavior.

Verification:
- Run mvn -Dtest=PropertyImageIntegrationTest test.
- Run mvn -Dtest=PropertyCreateUpdateIntegrationTest test.
- Run mvn -DskipTests compile.
```

## 6. Goal internal listing list/detail

```text
/goal Implement internal listing search/detail APIs for AGENT/MANAGER/ADMIN.

Context:
- Read docs/API_MISSING_BACKLOG.md section 7.1.
- Public listing APIs already exist, but internal draft/pending/moderation read APIs are missing.

Endpoints:
- GET /api/v1/listings
- GET /api/v1/listings/{listingId}

Requirements:
1. Support filters: status, purpose, createdBy, propertyId, keyword, page, size, sortBy, sortDirection.
2. Detail response should include listing fields, property summary, creator/reviewer, package, status history, view count, and favorite count when available.
3. Enforce role/resource visibility: agent sees own/assigned records unless existing project policy allows broader access.
4. Add OpenAPI docs and update docs/API_FRONTEND_REFERENCE.md.
5. Add integration tests.

Verification:
- Run mvn -Dtest=ListingCreateUpdateIntegrationTest test.
- Run mvn -Dtest=ListingReviewWorkflowIntegrationTest test.
- Run mvn -DskipTests compile.
```

## 7. Goal CRM notes/requirements/tags and follow-up tasks

```text
/goal Implement P0 CRM and follow-up task APIs from the missing backlog.

Context:
- Read docs/API_MISSING_BACKLOG.md sections 9.1, 9.2, and 10.1.

Endpoints:
- PUT /api/v1/customers/{customerId}/notes/{noteId}
- DELETE /api/v1/customers/{customerId}/notes/{noteId}
- PATCH /api/v1/customers/{customerId}/notes/{noteId}/pin
- PUT /api/v1/customers/{customerId}/requirements/{requirementId}
- DELETE /api/v1/customers/{customerId}/requirements/{requirementId}
- GET /api/v1/customers/{customerId}/tags
- POST /api/v1/customers/{customerId}/tags
- DELETE /api/v1/customers/{customerId}/tags/{tagId}
- GET /api/v1/follow-up-tasks
- GET /api/v1/follow-up-tasks/my
- GET /api/v1/follow-up-tasks/{taskId}
- PUT /api/v1/follow-up-tasks/{taskId}
- PATCH /api/v1/follow-up-tasks/{taskId}/status
- DELETE /api/v1/follow-up-tasks/{taskId}

Requirements:
1. Use DTOs and Bean Validation.
2. Enforce AGENT/MANAGER/ADMIN and resource ownership/assignment rules.
3. Add audit entries for note/requirement/tag/task changes.
4. Add OpenAPI docs and update docs/API_FRONTEND_REFERENCE.md.
5. Add integration tests.

Verification:
- Run mvn -Dtest=CustomerManagementIntegrationTest test.
- Run mvn -Dtest=LeadManagementIntegrationTest test.
- Run mvn -DskipTests compile.
```

## 8. Goal public inquiry and appointment request

```text
/goal Implement public listing inquiry and appointment request APIs that create leads/appointments.

Context:
- Read docs/API_MISSING_BACKLOG.md sections 7.5 and 22.1.

Endpoints:
- POST /api/v1/search/listings/{listingId}/inquiries
- POST /api/v1/search/listings/{listingId}/appointment-requests

Requirements:
1. Accept public or bearer requests.
2. Create or merge Customer by email/phone using existing CRM rules.
3. Create Lead with source LISTING_INQUIRY or equivalent seeded lead source.
4. Assign the listing/property agent when available.
5. Appointment request should create PENDING appointment and notify assigned agent.
6. Add validation, rate-limit awareness, OpenAPI docs, and API_FRONTEND_REFERENCE updates.
7. Add integration tests for guest and authenticated flows.

Verification:
- Run mvn -Dtest=PublicListingSearchFavoriteIntegrationTest test.
- Run mvn -Dtest=LeadManagementIntegrationTest test.
- Run mvn -Dtest=AppointmentManagementIntegrationTest test.
- Run mvn -DskipTests compile.
```

## 9. Final full regression goal

```text
/goal Run final regression for all implemented P0 backlog APIs and fix any failures.

Requirements:
1. Run the focused tests from each completed goal.
2. Run mvn clean test if environment supports Testcontainers/Postgres.
3. Check docs/API_FRONTEND_REFERENCE.md includes every new endpoint.
4. Check docs/API_MISSING_BACKLOG.md can be updated with implemented status notes without deleting the original backlog.
5. Verify no credentials were committed in .env, yml, docs, or tests.
6. Summarize implemented endpoints, changed files, and remaining backlog.
```

## Suggested Maven commands on Windows

Neu dung Maven bundled trong IntelliJ nhu repo hien tai:

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd' -DskipTests compile
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd' -Dtest=FileUploadIntegrationTest test
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd' clean test
```

Neu `mvn` da co trong PATH:

```powershell
mvn -DskipTests compile
mvn -Dtest=FileUploadIntegrationTest test
mvn clean test
```
