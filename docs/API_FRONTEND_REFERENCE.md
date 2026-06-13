# Tài liệu API phục vụ phát triển Frontend

> Cập nhật theo mã nguồn branch `breakthrough` ngày 11/06/2026.
> Phạm vi: các API hiện có đến Day 43, không bao gồm chức năng AI.

## 1. Thông tin chung

- Base URL local: `http://localhost:8081`
- API prefix chính: `/api/v1`
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- Content type mặc định: `application/json`
- Thời gian dạng thời điểm: ISO-8601 UTC, ví dụ `2026-06-10T08:30:00Z`
- Ngày không kèm giờ: `yyyy-MM-dd`, ví dụ `2026-06-10`
- Tiền tệ: mã 3 ký tự viết hoa, ví dụ `VND`, `USD`
- Phân trang bắt đầu từ `page=0`; `size` tối đa `100`
- Hướng sắp xếp: `ASC` hoặc `DESC`

### Chạy backend local với Docker Compose

Từ thư mục gốc repository:

```powershell
Copy-Item .env.example .env
docker compose up -d
Get-Content .env |
  Where-Object { $_ -match '^[^#][^=]*=' } |
  ForEach-Object {
    $name, $value = $_ -split '=', 2
    Set-Item -Path "Env:$name" -Value $value
  }
mvn spring-boot:run
```

Docker Compose cung cấp PostgreSQL (`5432`), Redis (`6379`), MinIO
(`9000`/`9001`) và MailHog (`1025`/`8025`). Backend đang dùng trực
tiếp PostgreSQL; upload vẫn lưu local và email dev vẫn ghi log. Nếu đổi
`DB_NAME` hoặc `DB_PORT` trong `.env`, cần cập nhật `DB_URL` tương ứng.

Các API cần đăng nhập phải gửi:

```http
Authorization: Bearer <accessToken>
```

## 2. Cấu trúc response

### 2.1 Thành công

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request processed successfully",
  "data": {},
  "timestamp": "2026-06-10T08:30:00Z"
}
```

Frontend luôn đọc dữ liệu nghiệp vụ từ `response.data`.

### 2.2 Dữ liệu phân trang

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

### 2.3 Lỗi

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "errors": [
    {
      "field": "email",
      "message": "email must be valid"
    }
  ],
  "path": "/api/v1/auth/register",
  "timestamp": "2026-06-10T08:30:00Z"
}
```

Frontend nên xử lý:

- `400`: body/query không hợp lệ hoặc vi phạm quy tắc nghiệp vụ.
- `401`: chưa đăng nhập, access token thiếu/hết hạn/không hợp lệ.
- `403`: đã đăng nhập nhưng không đúng role hoặc không có quyền với dữ liệu.
- `404`: tài nguyên không tồn tại.
- `409`: dữ liệu trùng hoặc xung đột trạng thái nếu backend trả conflict.
- `500`: lỗi ngoài dự kiến.

Khi nhận `401`, chỉ gọi refresh token một lần, cập nhật token và retry request gốc. Nếu refresh thất bại thì xóa phiên đăng nhập.

## 3. Role và phạm vi giao diện

| Role | Chức năng chính |
|---|---|
| `ADMIN` | Quản trị user, dashboard hệ thống, toàn bộ nghiệp vụ nội bộ |
| `MANAGER` | Quản trị user, duyệt listing/contract, commission, dashboard quản lý |
| `AGENT` | Property, listing, customer, lead, lịch hẹn, contract, transaction, commission cá nhân |
| `CUSTOMER` | Lịch hẹn cá nhân, yêu thích listing, thông báo |
| `OWNER` | Hiện chưa có controller nghiệp vụ riêng; có thể tham gia dữ liệu property/contract |

Lưu ý:

- Role ở controller chỉ là lớp kiểm tra đầu tiên.
- Service còn kiểm tra ownership/assignment. Ví dụ Agent thường chỉ thao tác dữ liệu được giao cho chính mình.
- Frontend phải ẩn action không phù hợp role, nhưng backend vẫn là nguồn xác thực cuối cùng.
- `/api/v1/admin/users` hiện cho cả `ADMIN` và `MANAGER`, dù URL chứa `admin`.

## 4. Authentication

### Endpoint

| Method | Endpoint | Auth | Request | Response data |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | `RegisterRequest` | `RegisterResponse` |
| `POST` | `/api/v1/auth/login` | Public | `LoginRequest` | `LoginResponse` |
| `POST` | `/api/v1/auth/refresh-token` | Public | `RefreshTokenRequest` | `LoginResponse` |
| `POST` | `/api/v1/auth/logout` | Public | `LogoutRequest` | `null` |
| `GET` | `/api/v1/auth/me` | Bearer | Không có | `AuthUserResponse` |

### Request

`RegisterRequest`

| Field | Bắt buộc | Quy tắc |
|---|---:|---|
| `email` | Có | Email hợp lệ, tối đa 255 |
| `password` | Có | 12-200 ký tự |
| `fullName` | Có | Tối đa 150 |
| `phone` | Không | 8-15 chữ số, có thể bắt đầu bằng `+` |

`LoginRequest`: `email`, `password`.

`RefreshTokenRequest` và `LogoutRequest`: `{ "refreshToken": "..." }`.

### Response chính

`LoginResponse`

```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "refreshToken": "...",
  "refreshExpiresIn": 2592000,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "fullName": "User",
    "status": "ACTIVE",
    "roles": ["AGENT"],
    "permissions": []
  }
}
```

`RegisterResponse`: `id`, `email`, `fullName`, `phone`, `status`, `roles`.

`AuthUserResponse`: `id`, `email`, `fullName`, `status`, `roles`, `permissions`.

## 5. Quản trị người dùng

Role: `ADMIN`, `MANAGER`.

| Method | Endpoint | Mục đích |
|---|---|---|
| `GET` | `/api/v1/admin/users` | Danh sách user |
| `GET` | `/api/v1/admin/users/{userId}` | Chi tiết user |
| `PATCH` | `/api/v1/admin/users/{userId}/status` | Đổi trạng thái |
| `PUT` | `/api/v1/admin/users/{userId}/roles` | Gán toàn bộ role |

Query danh sách:

- `page=0`
- `size=20`
- `sortBy=createdAt`
- `direction=DESC`

Request đổi trạng thái:

```json
{ "status": "ACTIVE" }
```

Request gán role:

```json
{ "roles": ["AGENT", "OWNER"] }
```

`UserManagementResponse`: `id`, `email`, `fullName`, `phone`, `status`,
`emailVerified`, `lockedUntil`, `lastLoginAt`, `createdAt`, `updatedAt`, `roles`.

## 6. Property

Role: `AGENT`, `MANAGER`, `ADMIN`.

### Endpoint

| Method | Endpoint | Request/Query | Response data |
|---|---|---|---|
| `GET` | `/api/v1/properties` | `PropertySearchRequest` | Page `PropertyResponse` |
| `GET` | `/api/v1/properties/{propertyId}` | - | `PropertyResponse` |
| `POST` | `/api/v1/properties` | `PropertyUpsertRequest` | `PropertyResponse` |
| `PUT` | `/api/v1/properties/{propertyId}` | `PropertyUpsertRequest` | `PropertyResponse` |
| `DELETE` | `/api/v1/properties/{propertyId}` | - | `null` |
| `PATCH` | `/api/v1/properties/{propertyId}/status` | `{status}` | `PropertyResponse` |
| `GET` | `/api/v1/properties/{propertyId}/images` | - | `PropertyImageResponse[]` |
| `POST` | `/api/v1/properties/{propertyId}/images` | Multipart | `PropertyImageResponse` |
| `DELETE` | `/api/v1/properties/{propertyId}/images/{imageId}` | - | `null` |
| `PATCH` | `/api/v1/properties/{propertyId}/cover-image/{imageId}` | - | `PropertyImageResponse` |

### Query tìm kiếm

`keyword`, `propertyTypeId`, `purpose`, `provinceId`, `districtId`, `wardId`,
`minPrice`, `maxPrice`, `minArea`, `maxArea`, `bedrooms`, `bathrooms`, `status`,
`page=0`, `size=20`, `sortBy=createdAt`, `direction=DESC`.

### Request tạo/cập nhật

```json
{
  "code": "PROP-001",
  "name": "Căn hộ trung tâm",
  "description": "Mô tả",
  "propertyTypeId": 1,
  "purpose": "SALE",
  "price": 3500000000,
  "currency": "VND",
  "landArea": 80,
  "floorArea": 75,
  "bedrooms": 2,
  "bathrooms": 2,
  "floors": 1,
  "direction": "SOUTHEAST",
  "legalStatus": "PINK_BOOK",
  "furnitureStatus": "FULLY_FURNISHED",
  "videoUrl": null,
  "virtualTourUrl": null,
  "availableFrom": "2026-07-01",
  "ownerId": 10,
  "assignedAgentId": 20,
  "address": {
    "provinceId": 1,
    "districtId": 2,
    "wardId": 3,
    "streetAddress": "123 Nguyễn Huệ",
    "fullAddress": "123 Nguyễn Huệ, ...",
    "latitude": 10.7769,
    "longitude": 106.7009
  },
  "amenities": [
    { "amenityId": 1, "details": "24/7" }
  ]
}
```

Bắt buộc: `code`, `name`, `propertyTypeId`, `purpose`, `currency`, `address`,
`address.provinceId`, `address.streetAddress`.

Upload image dùng `multipart/form-data`:

- `file`: bắt buộc
- `altText`: tùy chọn
- `displayOrder`: mặc định `0`

`PropertyResponse`: toàn bộ field request cùng `id`, tên/code của property type,
owner, creator, assigned agent, address đã mở rộng, amenities, `status`,
`createdAt`, `updatedAt`.

`PropertyImageResponse`: `id`, `imageUrl`, `fileName`, `mimeType`, `fileSize`,
`altText`, `coverImage`, `displayOrder`, uploader và `createdAt`.

## 7. Listing và tìm kiếm công khai

### 7.1 Quản lý listing

Role mặc định: `AGENT`, `MANAGER`, `ADMIN`.

| Method | Endpoint | Role bổ sung | Request |
|---|---|---|---|
| `POST` | `/api/v1/listings` | - | `ListingCreateRequest` |
| `PUT` | `/api/v1/listings/{listingId}` | - | `ListingUpdateRequest` |
| `PATCH` | `/api/v1/listings/{listingId}/submit` | - | Không body |
| `PATCH` | `/api/v1/listings/{listingId}/approve` | Manager/Admin | Không body |
| `PATCH` | `/api/v1/listings/{listingId}/reject` | Manager/Admin | `{reason}` |
| `PATCH` | `/api/v1/listings/{listingId}/publish` | - | Không body |
| `PATCH` | `/api/v1/listings/{listingId}/unpublish` | - | Không body |

Tạo listing:

```json
{
  "propertyId": 1,
  "code": "LIST-001",
  "title": "Căn hộ 2 phòng ngủ",
  "slug": "can-ho-2-phong-ngu",
  "description": "Mô tả chi tiết",
  "purpose": "SALE",
  "visibility": "PUBLIC",
  "askingPrice": 3500000000,
  "currency": "VND",
  "listingPackageId": null,
  "seoTitle": null,
  "seoDescription": null,
  "seoKeywords": null
}
```

Update không có `propertyId` và `code`; các field nội dung còn lại giống create.

Luồng chính:

`DRAFT/REJECTED -> PENDING_REVIEW -> APPROVED -> PUBLISHED -> UNPUBLISHED`

Backend quyết định chính xác transition hợp lệ. Frontend chỉ bật nút theo status hiện tại.

`ListingResponse`: `id`, `code`, property, creator, package, nội dung, `purpose`,
`status`, `visibility`, giá, SEO, reviewer, `rejectionReason`, các mốc thời gian,
`viewCount`.

Hiện chưa có API nội bộ `GET /api/v1/listings` hoặc `GET /api/v1/listings/{id}`.
Frontend quản trị listing chưa thể tải danh sách/chi tiết nội bộ đầy đủ chỉ bằng API hiện tại.

### 7.2 Tìm kiếm công khai

Không cần đăng nhập.

| Method | Endpoint | Mục đích |
|---|---|---|
| `GET` | `/api/v1/search/listings` | Tìm listing đã publish |
| `GET` | `/api/v1/search/listings/{slug}` | Chi tiết và ghi nhận lượt xem |

Query: `keyword`, `propertyTypeId`, `purpose`, vị trí, khoảng giá, khoảng diện tích,
`bedrooms`, `bathrooms`, `page`, `size`, `sortBy`, `direction`.

`sortBy` hỗ trợ: `price`, `askingPrice`, `publishedAt`, `createdAt`, `viewCount`.

Khi xem chi tiết anonymous, frontend nên tạo và giữ header:

```http
X-Session-Id: <uuid ổn định theo browser>
```

`PublicListingResponse`: listing/property/type, nội dung, giá, diện tích, phòng,
địa chỉ, `viewCount`, `publishedAt`, `createdAt`.

### 7.3 Favorite

Chỉ role `CUSTOMER`.

| Method | Endpoint |
|---|---|
| `POST` | `/api/v1/listings/{listingId}/favorite` |
| `DELETE` | `/api/v1/listings/{listingId}/favorite` |
| `GET` | `/api/v1/listings/favorites?page=0&size=20` |

## 8. Customer CRM

Role: `AGENT`, `MANAGER`, `ADMIN`.

| Method | Endpoint | Request/Response |
|---|---|---|
| `POST` | `/api/v1/customers` | `CustomerUpsertRequest` |
| `GET` | `/api/v1/customers` | Query -> Page `CustomerResponse` |
| `GET` | `/api/v1/customers/{customerId}` | `CustomerDetailResponse` |
| `PUT` | `/api/v1/customers/{customerId}` | `CustomerUpsertRequest` |
| `DELETE` | `/api/v1/customers/{customerId}` | `null` |
| `POST` | `/api/v1/customers/{customerId}/notes` | `CustomerNoteRequest` |
| `POST` | `/api/v1/customers/{customerId}/requirements` | `CustomerRequirementRequest` |
| `GET` | `/api/v1/customers/{customerId}/timeline` | Timeline array |

Query: `keyword`, `status`, `priority`, `assignedAgentId`, `page`, `size`,
`sortBy=createdAt`, `direction=DESC`.

`CustomerUpsertRequest`

```json
{
  "code": "CUS-001",
  "fullName": "Nguyễn Văn A",
  "email": "a@example.com",
  "phone": "0900000000",
  "status": "ACTIVE",
  "source": "WEBSITE",
  "priority": "HIGH",
  "preferredContactMethod": "PHONE",
  "notes": null,
  "userId": null,
  "assignedAgentId": 20
}
```

Cần ít nhất một trong `email`, `phone`, `userId`.

Note: `{ "content": "...", "pinned": false }`.

Requirement:

```json
{
  "purpose": "SALE",
  "propertyTypeId": 1,
  "provinceId": 1,
  "districtId": 2,
  "wardId": 3,
  "minBudget": 2000000000,
  "maxBudget": 4000000000,
  "currency": "VND",
  "minArea": 50,
  "maxArea": 100,
  "minBedrooms": 2,
  "minBathrooms": 1,
  "description": "Gần trung tâm"
}
```

`CustomerDetailResponse`: `{ customer, requirements, notes }`.

Timeline item: `type`, `referenceId`, `title`, `description`, actor, `occurredAt`.

## 9. Lead pipeline

Role: `AGENT`, `MANAGER`, `ADMIN`.

| Method | Endpoint | Request |
|---|---|---|
| `POST` | `/api/v1/leads` | `LeadCreateRequest` |
| `GET` | `/api/v1/leads` | `LeadSearchRequest` |
| `GET` | `/api/v1/leads/{leadId}` | - |
| `PATCH` | `/api/v1/leads/{leadId}/assign` | `LeadAssignRequest` |
| `PATCH` | `/api/v1/leads/{leadId}/status` | `LeadStatusUpdateRequest` |
| `POST` | `/api/v1/leads/{leadId}/notes` | `LeadNoteRequest` |
| `POST` | `/api/v1/leads/{leadId}/activities` | `LeadActivityRequest` |
| `POST` | `/api/v1/leads/{leadId}/follow-up-tasks` | `FollowUpTaskRequest` |

Query: `keyword`, `status`, `priority`, `sourceId`, `assignedAgentId`, `page`,
`size`, `sortBy=createdAt`, `direction=DESC`.

Tạo lead:

```json
{
  "code": "LEAD-001",
  "sourceCode": "WEBSITE",
  "fullName": "Khách hàng",
  "email": "lead@example.com",
  "phone": null,
  "priority": "HIGH",
  "message": "Quan tâm listing",
  "customerId": null,
  "listingId": 10,
  "assignedAgentId": 20
}
```

Cần ít nhất một trong `email`, `phone`, `customerId`.

Assign: `{ "agentId": 20, "notes": "Phân công" }`.

Đổi status: `{ "status": "CONTACTED", "reason": null }`.

Luồng pipeline:

`NEW -> ASSIGNED -> CONTACTED -> INTERESTED -> VIEWING_SCHEDULED -> NEGOTIATING -> CLOSED_WON/CLOSED_LOST`

`INVALID`, `CLOSED_WON`, `CLOSED_LOST` là trạng thái kết thúc. Lead phải được assign
trước khi đi sâu vào pipeline.

Activity:

```json
{
  "activityType": "CALL",
  "subject": "Gọi tư vấn",
  "details": "...",
  "occurredAt": "2026-06-10T08:30:00Z"
}
```

Frontend không được tạo thủ công activity type `STATUS_CHANGE` và `ASSIGNMENT`.

Follow-up task:

```json
{
  "title": "Gọi lại",
  "description": null,
  "priority": "HIGH",
  "dueAt": "2026-06-11T08:30:00Z",
  "assignedAgentId": 20
}
```

`LeadDetailResponse`: `{ lead, assignments, notes, activities, followUpTasks }`.

Hiện chưa có endpoint đổi trạng thái follow-up task.

## 10. Appointment

Role chung: `CUSTOMER`, `AGENT`, `MANAGER`, `ADMIN`.

| Method | Endpoint | Role/Ghi chú |
|---|---|---|
| `POST` | `/api/v1/appointments` | Tạo lịch |
| `GET` | `/api/v1/appointments` | Agent/Manager/Admin |
| `GET` | `/api/v1/appointments/my?page=0&size=20` | Người đang đăng nhập |
| `GET` | `/api/v1/appointments/{appointmentId}` | Theo quyền truy cập |
| `PATCH` | `/api/v1/appointments/{appointmentId}/confirm` | Confirm |
| `PATCH` | `/api/v1/appointments/{appointmentId}/cancel` | `{reason}` |
| `PATCH` | `/api/v1/appointments/{appointmentId}/reschedule` | Lịch mới |
| `PATCH` | `/api/v1/appointments/{appointmentId}/complete` | Complete |
| `POST` | `/api/v1/appointments/{appointmentId}/feedback` | Feedback |

Query danh sách: `status`, `agentId`, `customerId`, `propertyId`, `from`, `to`,
`page=0`, `size=20`, `sortBy=startAt`, `direction=ASC`.

Tạo lịch:

```json
{
  "code": "APT-001",
  "customerId": 1,
  "agentId": 20,
  "propertyId": 5,
  "listingId": 10,
  "leadId": 8,
  "title": "Xem căn hộ",
  "startAt": "2026-06-15T02:00:00Z",
  "endAt": "2026-06-15T03:00:00Z",
  "timezone": "Asia/Ho_Chi_Minh",
  "meetingLocation": "Tại dự án",
  "notes": null
}
```

`startAt`, `endAt` phải ở tương lai và `endAt > startAt`. Customer phải active,
property phải `AVAILABLE`; listing/lead nếu có phải khớp các đối tượng đã chọn.

Luồng:

- `PENDING -> CONFIRMED`
- `PENDING/CONFIRMED -> CANCELLED`
- `PENDING/CONFIRMED -> RESCHEDULED` và sinh lịch mới
- `CONFIRMED -> COMPLETED`
- Chỉ lịch `COMPLETED` mới nhận feedback

Feedback: `rating` 1-5 tùy chọn, `interestLevel` bắt buộc, cùng `comments`,
`positivePoints`, `concerns`, `nextAction`.

`AppointmentResponse` chứa dữ liệu liên kết, thời gian, trạng thái,
`participants[]`, `feedbacks[]`.

## 11. Contract

Role: `AGENT`, `MANAGER`, `ADMIN`; approve chỉ `MANAGER`, `ADMIN`.

| Method | Endpoint | Request |
|---|---|---|
| `POST` | `/api/v1/contracts` | `ContractCreateRequest` |
| `GET` | `/api/v1/contracts` | Search query |
| `GET` | `/api/v1/contracts/{contractId}` | - |
| `PUT` | `/api/v1/contracts/{contractId}` | `ContractUpdateRequest` |
| `POST` | `/api/v1/contracts/{contractId}/documents` | Multipart |
| `PATCH` | `/api/v1/contracts/{contractId}/submit-review` | - |
| `PATCH` | `/api/v1/contracts/{contractId}/approve` | Manager/Admin |
| `PATCH` | `/api/v1/contracts/{contractId}/mark-signed` | - |
| `PATCH` | `/api/v1/contracts/{contractId}/cancel` | `{reason}` |

Query: `status`, `contractType`, `propertyId`, `customerId`, `agentId`, `page`,
`size`, `sortBy=createdAt`, `direction=DESC`.

Create:

```json
{
  "code": "CTR-001",
  "contractType": "SALE",
  "propertyId": 1,
  "customerId": 2,
  "agentId": 20,
  "templateId": null,
  "title": "Hợp đồng mua bán",
  "totalValue": 3500000000,
  "currency": "VND",
  "effectiveDate": "2026-06-15",
  "expirationDate": "2026-12-15",
  "terms": "...",
  "notes": null
}
```

Update chỉ gồm: `title`, `totalValue`, `currency`, `effectiveDate`,
`expirationDate`, `terms`, `notes`.

Upload document multipart:

- `file`
- `documentType`: `DRAFT|FINAL|SIGNED|ATTACHMENT`
- `displayName`: tùy chọn
- `description`: tùy chọn
- `primaryDocument`: mặc định `false`

Luồng chính:

`DRAFT -> PENDING_REVIEW -> PENDING_SIGNATURE -> SIGNED`

Contract có thể bị cancel ở các trạng thái backend cho phép.

`ContractResponse` chứa thông tin contract, property/customer/owner/agent,
giá trị, thời hạn, các mốc trạng thái, `parties[]`, `documents[]`.

## 12. Transaction và ghi nhận thanh toán ngoài hệ thống

Role: `AGENT`, `MANAGER`, `ADMIN`.

Hệ thống hiện **không tích hợp cổng thanh toán**. Các endpoint payment/deposit chỉ
ghi nhận giao dịch đã thực hiện bên ngoài như chuyển khoản, tiền mặt hoặc ví điện tử.

### Endpoint

| Method | Endpoint | Request |
|---|---|---|
| `POST` | `/api/v1/transactions` | `TransactionCreateRequest` |
| `GET` | `/api/v1/transactions` | Search query |
| `GET` | `/api/v1/transactions/{transactionId}` | - |
| `PATCH` | `/api/v1/transactions/{transactionId}/status` | `{status, reason}` |
| `POST` | `/api/v1/transactions/{transactionId}/deposits` | `DepositCreateRequest` |
| `POST` | `/api/v1/transactions/{transactionId}/payment-schedules` | `PaymentScheduleCreateRequest` |
| `POST` | `/api/v1/transactions/{transactionId}/payments` | `PaymentCreateRequest` |
| `POST` | `/api/v1/transactions/{transactionId}/invoices` | `InvoiceCreateRequest` |
| `POST` | `/api/v1/transactions/{transactionId}/payments/{paymentId}/receipt` | `ReceiptCreateRequest` |

Query: `status`, `transactionType`, `propertyId`, `customerId`, `agentId`, `page`,
`size`, `sortBy=createdAt`, `direction=DESC`.

Create:

```json
{
  "code": "TX-001",
  "contractId": 10,
  "propertyId": 1,
  "customerId": 2,
  "agentId": 20,
  "transactionType": "SALE",
  "agreedValue": 3500000000,
  "currency": "VND",
  "transactionDate": "2026-06-15",
  "expectedCompletionDate": "2026-07-15",
  "notes": null
}
```

Nếu có `contractId`, contract phải `SIGNED` hoặc `ACTIVE`.

Status update:

```json
{ "status": "COMPLETED", "reason": null }
```

`reason` bắt buộc khi chuyển sang `CANCELLED`.

Luồng tổng quát:

`PENDING -> DEPOSITED -> CONTRACT_SIGNED -> PAYMENT_IN_PROGRESS -> COMPLETED`

Các nhánh kết thúc: `CANCELLED`, `REFUNDED`. Transaction kết thúc không thể sửa.
Backend kiểm tra tiền xác nhận trước khi `COMPLETED`/`REFUNDED`.

### Deposit

```json
{
  "amount": 200000000,
  "currency": "VND",
  "paymentMethod": "BANK_TRANSFER",
  "referenceNumber": "BANK-REF-001",
  "idempotencyKey": "deposit-tx-1-v1",
  "dueDate": "2026-06-20",
  "receivedAt": "2026-06-15T08:30:00Z",
  "notes": null
}
```

Deposit được ghi nhận và verify ngay theo flow hiện tại; có thể đưa transaction từ
`PENDING` sang `DEPOSITED`.

### Payment schedule

`installmentNumber`, `label`, `dueDate`, `amount`, `currency`, `notes`.

### Payment ngoài hệ thống

`paymentScheduleId` tùy chọn, `amount`, `currency`, `paymentMethod`,
`referenceNumber`, `idempotencyKey`, `paidAt`, `notes`.

Frontend phải tạo `idempotencyKey` duy nhất cho mỗi thao tác để tránh ghi nhận tiền
hai lần khi người dùng double-click hoặc retry.

### Invoice

`invoiceNumber`, `issueDate`, `dueDate`, `subtotal`, `taxAmount`, `currency`,
`billedToName`, `billedToEmail`, `billedToAddress`, `notes`.

### Receipt

`receiptNumber`, `issuedAt`, `payerName`, `notes`. Payment phải thuộc transaction
và đã `COMPLETED`.

`TransactionResponse` chứa số tiền thỏa thuận/xác nhận/còn lại và các mảng:
`deposits`, `paymentSchedules`, `payments`, `invoices`.

## 13. Commission

### Commission cá nhân và quản lý

| Method | Endpoint | Role |
|---|---|---|
| `GET` | `/api/v1/commissions/my` | Agent/Manager/Admin |
| `GET` | `/api/v1/commissions` | Manager/Admin |
| `PATCH` | `/api/v1/commissions/{commissionId}/mark-paid` | Manager/Admin |

Query: `status`, `transactionId`, `beneficiaryUserId`, `page`, `size`,
`sortBy=createdAt`, `direction=DESC`.

Mark paid:

```json
{
  "paymentReference": "COM-PAY-001",
  "paidAt": "2026-06-10T08:30:00Z",
  "notes": null
}
```

`CommissionResponse`: transaction/rule/beneficiary, calculation type, base amount,
rate, amount, currency, approval/payment metadata, status và timestamps.

### Commission rule

Role: `MANAGER`, `ADMIN`.

| Method | Endpoint |
|---|---|
| `POST` | `/api/v1/commission-rules` |
| `GET` | `/api/v1/commission-rules` |
| `PUT` | `/api/v1/commission-rules/{ruleId}` |

Query: `active`, `transactionType`, `page`, `size`, `sortBy=priority`,
`direction=DESC`.

Rule:

```json
{
  "code": "SALE_DEFAULT",
  "name": "Hoa hồng bán mặc định",
  "transactionType": "SALE",
  "calculationType": "PERCENTAGE",
  "rate": 2.5,
  "fixedAmount": null,
  "currency": "VND",
  "minTransactionValue": 0,
  "maxTransactionValue": null,
  "priority": 10,
  "active": true,
  "effectiveFrom": "2026-01-01",
  "effectiveTo": null,
  "description": null
}
```

`PERCENTAGE` yêu cầu `rate` và không có `fixedAmount`; `FIXED` thì ngược lại.

## 14. Notification

Mọi user đã đăng nhập.

| Method | Endpoint | Response |
|---|---|---|
| `GET` | `/api/v1/notifications?unread=true&page=0&size=20` | Page notification |
| `GET` | `/api/v1/notifications/unread-count` | `{unreadCount}` |
| `PATCH` | `/api/v1/notifications/{notificationId}/read` | Notification |
| `PATCH` | `/api/v1/notifications/read-all` | `{updatedCount}` |

Notification: `id`, `type`, `title`, `message`, `actionUrl`, `referenceType`,
`referenceId`, `metadataJson`, `read`, `readAt`, `createdAt`.

Frontend nên:

- Poll `unread-count` theo chu kỳ phù hợp.
- Điều hướng bằng `actionUrl`.
- Parse `metadataJson` có kiểm tra lỗi vì đây là chuỗi JSON nullable.

## 15. Dashboard

| Method | Endpoint | Role |
|---|---|---|
| `GET` | `/api/v1/dashboard/admin` | Admin |
| `GET` | `/api/v1/dashboard/manager` | Manager/Admin |
| `GET` | `/api/v1/dashboard/agent` | Agent/Manager/Admin |

Admin response:

- `totalUsers`
- `totalProperties`
- `totalListings`
- `pendingListings`
- `totalLeads`, `leadsByStatus[]`
- `totalTransactions`, `transactionsByStatus[]`
- `revenueSummary[]`
- `topAgents[]`

Manager response:

- `totalAgents`
- Lead totals/status và `leadCloseRate`
- Transaction totals/status
- `pendingCommissions`, `paidCommissions`
- `revenueSummary[]`
- `topAgents[]`

Revenue được tách theo `currency`; frontend không cộng trực tiếp VND và USD:

```json
{
  "currency": "VND",
  "completedTransactions": 10,
  "completedTransactionValue": 10000000000,
  "completedPayments": 8000000000,
  "verifiedDeposits": 1000000000,
  "paidCommissions": 200000000
}
```

`TopAgentResponse`: `agentId`, `agentName`, `totalTransactions`,
`completedTransactions`.

Manager dashboard hiện là số liệu toàn hệ thống vì schema chưa có team/manager-agent mapping.

Agent response:

- `myLeads`, `myLeadsByStatus[]`
- `todayAppointments`
- `followUpTasks`, `overdueFollowUpTasks`
- `activeTransactions`, `activeTransactionsByStatus[]`
- `myCommissions`, `myCommissionsByStatus[]`
- `myCommissionAmounts[]`, tách theo `currency`

### Báo cáo

Role: `MANAGER`, `ADMIN`.

Tất cả endpoint báo cáo bắt buộc query `from` và `to` dạng `yyyy-MM-dd`.

| Method | Endpoint | Response |
|---|---|---|
| `GET` | `/api/v1/reports/revenue?from=2026-06-01&to=2026-06-30` | `RevenueReportResponse` |
| `GET` | `/api/v1/reports/leads?from=2026-06-01&to=2026-06-30` | `LeadReportResponse` |
| `GET` | `/api/v1/reports/transactions?from=2026-06-01&to=2026-06-30` | `TransactionReportResponse` |
| `GET` | `/api/v1/reports/commissions?from=2026-06-01&to=2026-06-30` | `CommissionReportResponse` |

Khoảng ngày tính cả `from` và `to`; `from` không được sau `to`.

## 16. Audit log

Role: chỉ `ADMIN`.

| Method | Endpoint | Mục đích |
|---|---|---|
| `GET` | `/api/v1/audit-logs` | Danh sách audit log có phân trang/lọc |
| `GET` | `/api/v1/audit-logs/{auditLogId}` | Chi tiết audit log |

Query danh sách:

- `actorId`, `action`, `resourceType`, `resourceId`
- `from`, `to`: ISO-8601 timestamp
- `page`, `size`, `sortBy`, `direction`
- `sortBy`: `createdAt`, `action`, `resourceType`, `resourceId`

`AuditLogResponse`: `id`, thông tin actor (`actorId`, `actorEmail`,
`actorName`), `action`, `resourceType`, `resourceId`, `oldValue`, `newValue`,
`createdAt`. `oldValue` và `newValue` là JSON object nullable.

Action hiện có:

```text
USER_STATUS_CHANGED, USER_ROLES_CHANGED, LISTING_APPROVED, LISTING_REJECTED,
TRANSACTION_STATUS_CHANGED, CONTRACT_STATUS_CHANGED, COMMISSION_PAID
```

## 17. File upload độc lập

Role: `AGENT`, `MANAGER`, `ADMIN`.

`POST /api/v1/files/upload`, multipart:

- `file`: bắt buộc
- `accessLevel`: `PUBLIC` hoặc `PRIVATE`, mặc định `PRIVATE`

Loại file cho phép hiện tại:

- `image/jpeg`
- `image/png`
- `image/webp`
- `application/pdf`

Giới hạn mặc định: file `10MB`, request multipart `11MB`.

`FileResourceResponse`: `id`, `originalFileName`, `contentType`, `fileSize`,
`checksumSha256`, `storageProvider`, `accessLevel`, `publicUrl`, `uploadedById`,
`createdAt`.

Không có API download file private trong controller hiện tại.

## 18. Danh sách enum frontend

```text
RoleCode: ADMIN, MANAGER, AGENT, CUSTOMER, OWNER
UserStatus: PENDING_VERIFICATION, ACTIVE, INACTIVE, LOCKED

PropertyPurpose: SALE, RENT
PropertyStatus: DRAFT, AVAILABLE, RESERVED, SOLD, RENTED, INACTIVE, DELETED
PropertyDirection: NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST
PropertyLegalStatus: PINK_BOOK, RED_BOOK, SALE_CONTRACT, WAITING_FOR_CERTIFICATE, OTHER, UNKNOWN
FurnitureStatus: UNFURNISHED, PARTIALLY_FURNISHED, FULLY_FURNISHED
AmenityCategory: ACCESS, SECURITY, LEISURE, FEATURE

ListingPurpose: SALE, RENT
ListingStatus: DRAFT, PENDING_REVIEW, APPROVED, REJECTED, PUBLISHED, UNPUBLISHED, EXPIRED, SOLD, RENTED
ListingVisibility: PUBLIC, INTERNAL, PRIVATE

CustomerStatus: ACTIVE, INACTIVE, ARCHIVED
CustomerSource: MANUAL, WEBSITE, REFERRAL, IMPORT, OTHER
CustomerPriority: LOW, MEDIUM, HIGH

LeadPipelineStatus: NEW, ASSIGNED, CONTACTED, INTERESTED, VIEWING_SCHEDULED,
  NEGOTIATING, CLOSED_WON, CLOSED_LOST, INVALID
LeadPriority: LOW, MEDIUM, HIGH
LeadActivityType: CALL, EMAIL, CHAT, MEETING, STATUS_CHANGE, ASSIGNMENT, OTHER
FollowUpTaskStatus: PENDING, IN_PROGRESS, COMPLETED, CANCELLED

AppointmentStatus: PENDING, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW, RESCHEDULED
ViewingInterestLevel: HIGH, MEDIUM, LOW, NOT_INTERESTED
AppointmentParticipantRole: CUSTOMER, AGENT, OWNER, OTHER
ParticipantResponseStatus: INVITED, ACCEPTED, DECLINED, TENTATIVE

ContractType: SALE, LEASE
ContractStatus: DRAFT, PENDING_REVIEW, PENDING_SIGNATURE, SIGNED, ACTIVE,
  EXPIRED, CANCELLED, TERMINATED
ContractDocumentType: DRAFT, FINAL, SIGNED, ATTACHMENT
ContractPartyRole: BUYER, SELLER, TENANT, LANDLORD, AGENT, WITNESS, OTHER

TransactionStatus: PENDING, DEPOSITED, CONTRACT_SIGNED, PAYMENT_IN_PROGRESS,
  COMPLETED, CANCELLED, REFUNDED
DepositStatus: PENDING, RECEIVED, VERIFIED, REFUNDED, CANCELLED
PaymentScheduleStatus: PENDING, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED
PaymentMethod: CASH, BANK_TRANSFER, CREDIT_CARD, DEBIT_CARD, E_WALLET, OTHER
PaymentStatus: PENDING, COMPLETED, FAILED, REFUNDED, CANCELLED
InvoiceStatus: DRAFT, ISSUED, PARTIALLY_PAID, PAID, OVERDUE, VOID

CommissionCalculationType: PERCENTAGE, FIXED
CommissionStatus: PENDING, APPROVED, PAID, CANCELLED

FileAccessLevel: PUBLIC, PRIVATE
StorageProvider: LOCAL
Sort.Direction: ASC, DESC
```

## 19. Đề xuất cấu trúc frontend

### Public

- Trang tìm kiếm listing
- Trang chi tiết listing
- Đăng ký, đăng nhập

### Customer

- Favorite listings
- Lịch hẹn của tôi
- Chi tiết/cancel/reschedule/feedback lịch hẹn
- Notification center

### Agent

- Property list/detail/create/edit/images
- Listing create/edit/workflow
- Customer CRM/detail/timeline
- Lead kanban/detail/activity/task
- Appointment calendar
- Contract list/detail/documents/workflow
- Transaction detail/payment records
- Commission của tôi
- Agent dashboard
- Notification center

### Manager

- Toàn bộ Agent UI
- User management
- Listing review
- Contract approval
- Commission và commission rules
- Manager dashboard
- Reports

### Admin

- Toàn bộ Manager UI
- Admin dashboard
- Audit log

## 20. Khoảng trống API cần bổ sung để frontend đầy đủ

Các phần sau chưa có endpoint dù dữ liệu/model đã tồn tại:

1. Master data: province, district, ward, property type, amenity, lead source,
   listing package, contract template.
2. Danh sách và chi tiết listing nội bộ cho Agent/Manager/Admin.
3. Download/stream file private.
4. Quản lý trạng thái follow-up task.
5. API cho owner portal.
6. API hồ sơ cá nhân, đổi mật khẩu, quên mật khẩu, xác minh email.
7. API đánh dấu no-show appointment hoặc phản hồi lời mời participant.
8. API quản trị deposit/payment/invoice sau khi đã tạo như refund/cancel/void.
9. Team/department mapping để dashboard Manager chỉ phản ánh đội nhóm của họ.
10. API download/xuất invoice, receipt và contract thành file.

Frontend nên mock hoặc trì hoãn các màn hình này cho đến khi backend bổ sung.

## 21. API legacy không nên dùng

Mã nguồn còn controller cũ `/api/buildings/`. Controller này:

- Không theo response envelope chuẩn.
- Không thuộc kiến trúc `/api/v1`.
- Không có security/DTO/validation đồng nhất.
- Không liên quan module property mới.

Frontend mới không nên tích hợp `/api/buildings/`.
