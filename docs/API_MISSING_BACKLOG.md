# API Missing Backlog - Real Estate Management

Ngay lap: 2026-06-26

Pham vi ra soat: cac controller hien co trong `src/main/java/com/javaweb/**/controller`
va tai lieu `docs/API_FRONTEND_REFERENCE.md`.

Muc tieu cua file nay la ghi lai nhung API can xay dung bo sung de backend
gan hon voi mot he thong real estate management thuc te. Day la backlog thiet
ke, chua phai implementation.

## 1. Tom tat nhanh

Backend hien da co cac module cot loi:

- Auth co register, login, refresh token, logout, current user.
- Admin user management co list user, detail user, update status, assign roles.
- Property co CRUD, search, status, upload/list/delete image, set cover image.
- Listing co create, update, submit, approve, reject, publish, unpublish.
- Public listing co search va detail theo slug.
- Favorite listing co add, remove, list current user's favorites.
- Customer CRM co CRUD, note, requirement, timeline.
- Lead co CRUD co ban, assign, update status, note, activity, follow-up task.
- Appointment co create/search/my/detail/confirm/cancel/reschedule/complete/feedback.
- Contract co CRUD co ban, upload document, submit review, approve, mark signed, cancel.
- Transaction co create/search/detail/status/deposit/schedule/payment/invoice/receipt.
- Commission co my/list/mark-paid va commission rule create/list/update.
- Notification co list/unread/read/read-all.
- Dashboard/report/audit/file upload/AI basic da co.

Ket luan: he thong da du khung chinh cho demo end-to-end, nhung chua du day
cho van hanh thuc te. Thieu nhieu API ve self-service account, master data,
owner portal, internal listing read, legal document, team/organization,
saved search, payment lifecycle, va CRUD chi tiet cho notes/tasks/documents.

## 2. Uu tien trien khai

### P0 - Can lam som vi anh huong truc tiep den UI/flow hien tai

- Current account APIs: edit profile, change password, avatar, sessions.
- Forgot/reset password va email verification.
- Internal listing list/detail.
- Master data APIs cho dropdown: provinces, districts, wards, property types,
  amenities, listing packages, lead sources.
- Property legal document APIs.
- Update/delete customer notes, requirements, tags.
- Follow-up task list/update/complete/cancel.
- File detail/download/delete.

### P1 - Can cho san pham thuc te

- Owner portal.
- Public inquiry/contact API tao lead tu listing.
- Saved search va listing alert.
- Team/agency/agent assignment.
- Contract template/document/signature APIs day du.
- Payment/deposit/invoice/receipt lifecycle update/refund/cancel.
- Commission approve/cancel/recalculate.
- Export/import CSV/Excel cho property, customer, lead, report.

### P2 - Nang cao

- Geo search/autocomplete/comparable listings.
- WebSocket/SSE notifications.
- Audit export va retention.
- AI price insight, AI chatbot tao lead/appointment co guardrail.
- Public review/testimonial, open house, marketing campaign.

## 3. Account va authentication

### 3.1 Update profile cua tai khoan hien tai

Trang thai hien tai: chua co. Hien chi co `GET /api/v1/auth/me`.

Ly do can bo sung: user can tu sua ho ten, so dien thoai, avatar, thong tin
lien he. Vi du ban da thay thieu API edit thong tin tai khoan.

Endpoint de xuat:

| Method | Path | Auth | Mo ta |
| --- | --- | --- | --- |
| PATCH | `/api/v1/auth/me/profile` | Bearer | Cap nhat profile cua user dang dang nhap |

Request:

```json
{
  "fullName": "Tran Ngoc Lan",
  "phone": "+84901234567",
  "preferredLanguage": "vi",
  "timezone": "Asia/Ho_Chi_Minh"
}
```

Response:

```json
{
  "id": 10,
  "email": "agent.lan@example.com",
  "fullName": "Tran Ngoc Lan",
  "phone": "+84901234567",
  "status": "ACTIVE",
  "roles": ["AGENT"]
}
```

Ghi chu implementation:

- Khong cho update `email`, `status`, `roles` o endpoint nay.
- Phone phai unique neu schema dang enforce unique.
- Ghi audit action `USER_PROFILE_UPDATED`.

### 3.2 Doi mat khau

Trang thai hien tai: chua co.

Endpoint de xuat:

| Method | Path | Auth | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/me/change-password` | Bearer | User doi mat khau khi con dang nhap |

Request:

```json
{
  "currentPassword": "OldPass@123",
  "newPassword": "NewPass@123",
  "confirmPassword": "NewPass@123"
}
```

Quy tac:

- Verify `currentPassword`.
- Validate strong password nhu register.
- Thu hoi refresh token cu sau khi doi mat khau, tru token hien tai neu muon UX mem hon.
- Ghi audit action `PASSWORD_CHANGED`.

### 3.3 Avatar cua tai khoan hien tai

Trang thai hien tai: chua co. Co file upload chung nhung chua gan vao user.

Endpoint de xuat:

| Method | Path | Auth | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/me/avatar` | Bearer | Upload avatar |
| DELETE | `/api/v1/auth/me/avatar` | Bearer | Xoa avatar |

Request multipart:

```http
file=<binary image>
```

Can them DB field:

- `users.avatar_file_resource_id` hoac bang rieng `user_profiles`.

### 3.4 Forgot password va reset password

Trang thai hien tai: co `otp_tokens` nhung chua co API flow.

Endpoint de xuat:

| Method | Path | Auth | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/forgot-password` | Public | Gui OTP/reset link qua email |
| POST | `/api/v1/auth/reset-password` | Public | Reset mat khau bang token |

Forgot request:

```json
{
  "email": "customer@example.com"
}
```

Reset request:

```json
{
  "email": "customer@example.com",
  "token": "123456",
  "newPassword": "NewPass@123",
  "confirmPassword": "NewPass@123"
}
```

Quy tac:

- Khong leak email ton tai hay khong ton tai.
- Rate limit theo IP va email.
- OTP co expiry, consumed_at, failed_attempts.
- Revoke refresh tokens sau reset.

### 3.5 Email verification

Trang thai hien tai: `users.email_verified` va `otp_tokens` ton tai, chua co API.

Endpoint de xuat:

| Method | Path | Auth | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/verify-email` | Public | Xac thuc email bang OTP |
| POST | `/api/v1/auth/resend-verification` | Public/Bearer | Gui lai OTP verify |

Request:

```json
{
  "email": "user@example.com",
  "token": "123456"
}
```

### 3.6 Session management

Trang thai hien tai: co refresh token table, nhung chua co API quan ly session.

Endpoint de xuat:

| Method | Path | Auth | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/auth/me/sessions` | Bearer | Liet ke refresh sessions dang active |
| DELETE | `/api/v1/auth/me/sessions/{sessionId}` | Bearer | Revoke mot session |
| DELETE | `/api/v1/auth/me/sessions` | Bearer | Logout tat ca thiet bi |

Can them fields neu muon hien thi tot:

- `refresh_tokens.user_agent`
- `refresh_tokens.ip_hash`
- `refresh_tokens.last_used_at`

## 4. Admin user, role va permission

### 4.1 Admin tao user noi bo

Trang thai hien tai: register public co tao user, admin chua co create user.

Endpoint de xuat:

| Method | Path | Auth | Role | Mo ta |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/admin/users` | Bearer | ADMIN/MANAGER | Tao user noi bo cho agent/manager/owner |

Request:

```json
{
  "email": "agent.new@example.com",
  "fullName": "New Agent",
  "phone": "+84900001111",
  "roles": ["AGENT"],
  "status": "ACTIVE",
  "sendInviteEmail": true
}
```

Ghi chu:

- MANAGER chi nen tao AGENT/CUSTOMER/OWNER, khong tao ADMIN.
- Neu `sendInviteEmail=true`, tao token set password.

### 4.2 Admin sua thong tin user

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| PATCH | `/api/v1/admin/users/{userId}` | ADMIN/MANAGER | Sua fullName, phone, email neu duoc phep |

Request:

```json
{
  "fullName": "Agent Updated",
  "phone": "+84900002222",
  "email": "agent.updated@example.com"
}
```

### 4.3 Reset password cho user

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/admin/users/{userId}/reset-password` | ADMIN | Tao password tam hoac invite reset |

Request:

```json
{
  "sendEmail": true,
  "temporaryPassword": null
}
```

### 4.4 Role va permission read APIs

Trang thai hien tai: seed role/permission co san, nhung chua co API list.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/admin/roles` | ADMIN/MANAGER | List roles |
| GET | `/api/v1/admin/roles/{roleId}` | ADMIN/MANAGER | Role detail va permissions |
| GET | `/api/v1/admin/permissions` | ADMIN | List permissions |

P1 neu can dynamic RBAC:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/admin/roles` | ADMIN | Tao custom role |
| PUT | `/api/v1/admin/roles/{roleId}` | ADMIN | Sua custom role |
| PUT | `/api/v1/admin/roles/{roleId}/permissions` | ADMIN | Gan permissions |

## 5. Master data APIs

Trang thai hien tai: cac bang master data co trong DB, nhung chua co controller
de frontend lay dropdown dong.

### 5.1 Location APIs

Endpoint de xuat:

| Method | Path | Auth | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/master-data/provinces` | Public/Bearer | List tinh/thanh |
| GET | `/api/v1/master-data/provinces/{provinceId}/districts` | Public/Bearer | List quan/huyen |
| GET | `/api/v1/master-data/districts/{districtId}/wards` | Public/Bearer | List phuong/xa |

Response province item:

```json
{
  "id": 1,
  "code": "HCM",
  "name": "Ho Chi Minh City",
  "administrativeType": "Municipality",
  "active": true
}
```

### 5.2 Property type, amenity, package, lead source

Endpoint de xuat:

| Method | Path | Auth | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/master-data/property-types` | Public/Bearer | Dropdown loai BDS |
| GET | `/api/v1/master-data/amenities` | Public/Bearer | Dropdown tien ich, filter category |
| GET | `/api/v1/master-data/listing-packages` | Bearer | Goi tin dang |
| GET | `/api/v1/master-data/lead-sources` | Bearer | Nguon lead |

P1 admin CRUD:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/admin/master-data/property-types` | ADMIN | Tao type |
| PUT | `/api/v1/admin/master-data/property-types/{id}` | ADMIN | Sua type |
| PATCH | `/api/v1/admin/master-data/property-types/{id}/active` | ADMIN | Bat/tat type |
| POST | `/api/v1/admin/master-data/amenities` | ADMIN | Tao amenity |
| PUT | `/api/v1/admin/master-data/amenities/{id}` | ADMIN | Sua amenity |
| POST | `/api/v1/admin/master-data/listing-packages` | ADMIN/MANAGER | Tao package |
| PUT | `/api/v1/admin/master-data/listing-packages/{id}` | ADMIN/MANAGER | Sua package |

## 6. Property APIs can bo sung

### 6.1 Property legal documents

Trang thai hien tai: bang `property_legal_documents` va enum ton tai, nhung
chua co endpoint upload/list/verify.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/properties/{propertyId}/legal-documents` | AGENT/MANAGER/ADMIN | List tai lieu phap ly |
| POST | `/api/v1/properties/{propertyId}/legal-documents` | AGENT/MANAGER/ADMIN | Upload tai lieu |
| GET | `/api/v1/properties/{propertyId}/legal-documents/{documentId}` | AGENT/MANAGER/ADMIN | Detail |
| PATCH | `/api/v1/properties/{propertyId}/legal-documents/{documentId}` | AGENT/MANAGER/ADMIN | Sua metadata |
| PATCH | `/api/v1/properties/{propertyId}/legal-documents/{documentId}/verify` | MANAGER/ADMIN | Verify/reject |
| DELETE | `/api/v1/properties/{propertyId}/legal-documents/{documentId}` | MANAGER/ADMIN | Xoa tai lieu |

Upload request multipart:

```http
file=<binary>
documentType=PINK_BOOK
documentNumber=CS123456
issuedBy=Department of Natural Resources
issuedDate=2024-01-15
expiryDate=
notes=Original owner copy scanned
```

Verify request:

```json
{
  "verificationStatus": "VERIFIED",
  "notes": "Matched owner and address"
}
```

### 6.2 Update image metadata va reorder

Trang thai hien tai: co upload/delete/set cover, chua co update alt text/order.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| PATCH | `/api/v1/properties/{propertyId}/images/{imageId}` | AGENT/MANAGER/ADMIN | Sua altText, displayOrder |
| PUT | `/api/v1/properties/{propertyId}/images/reorder` | AGENT/MANAGER/ADMIN | Sap xep nhieu anh |

Request reorder:

```json
{
  "items": [
    { "imageId": 10, "displayOrder": 0 },
    { "imageId": 11, "displayOrder": 1 }
  ]
}
```

### 6.3 Import/export property

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/properties/import` | MANAGER/ADMIN | Import CSV/Excel |
| GET | `/api/v1/properties/export` | MANAGER/ADMIN | Export theo filter |
| GET | `/api/v1/properties/import/template` | MANAGER/ADMIN | Download template |

### 6.4 Owner-facing property submission

Neu co role `OWNER`, nen tach flow owner gui BDS cho san duyet.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/owner/properties` | OWNER | BDS cua owner |
| POST | `/api/v1/owner/properties` | OWNER | Owner gui BDS moi |
| PUT | `/api/v1/owner/properties/{propertyId}` | OWNER | Owner sua khi chua approve |
| PATCH | `/api/v1/owner/properties/{propertyId}/submit` | OWNER | Gui san duyet |

Can them status workflow hoac audit rieng:

- `OWNER_DRAFT`
- `PENDING_OWNER_REVIEW`
- `APPROVED`
- `REJECTED`

## 7. Listing APIs can bo sung

### 7.1 Internal list/detail listing

Trang thai hien tai: thieu `GET /api/v1/listings` va
`GET /api/v1/listings/{id}` cho draft/pending/moderation.

Ghi chu implemented: da bo sung `GET /api/v1/listings` va
`GET /api/v1/listings/{listingId}` cho AGENT/MANAGER/ADMIN, gom filter noi bo,
phan quyen theo owner/assigned agent, detail status history, view count va
favorite count.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/listings` | AGENT/MANAGER/ADMIN | Search listing noi bo |
| GET | `/api/v1/listings/{listingId}` | AGENT/MANAGER/ADMIN | Detail listing noi bo |

Query filter:

```http
?status=PENDING_REVIEW&purpose=SALE&createdBy=10&propertyId=5&keyword=apartment&page=0&size=20&sortBy=createdAt&sortDirection=DESC
```

Response detail nen gom:

- Listing fields.
- Property summary.
- Created by/reviewed by.
- Package.
- Status history.
- View count.
- Favorite count.

### 7.2 Listing status history

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/listings/{listingId}/status-history` | AGENT/MANAGER/ADMIN | Lich su workflow |

### 7.3 Delete/archive listing

Trang thai hien tai: entity co `deleted_at`, chua co endpoint delete.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| DELETE | `/api/v1/listings/{listingId}` | AGENT/MANAGER/ADMIN | Soft delete draft/unpublished |
| PATCH | `/api/v1/listings/{listingId}/restore` | MANAGER/ADMIN | Restore listing da delete |

### 7.4 Mark sold/rented/expired

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| PATCH | `/api/v1/listings/{listingId}/mark-sold` | AGENT/MANAGER/ADMIN | Danh dau da ban |
| PATCH | `/api/v1/listings/{listingId}/mark-rented` | AGENT/MANAGER/ADMIN | Danh dau da cho thue |
| PATCH | `/api/v1/listings/{listingId}/expire` | MANAGER/ADMIN/System | Het han tin |
| PATCH | `/api/v1/listings/{listingId}/extend` | AGENT/MANAGER/ADMIN | Gia han tin |

Extend request:

```json
{
  "listingPackageId": 2,
  "expiresAt": "2026-08-01T00:00:00Z"
}
```

### 7.5 Public inquiry tu listing

Trang thai hien tai: public search/detail co, nhung guest/customer gui yeu cau
tu van tu listing chua co API rieng.

Ghi chu implemented: da bo sung
`POST /api/v1/search/listings/{listingId}/inquiries`, chap nhan Public/Bearer,
merge/tao customer theo email/phone, tao lead source `LISTING_INQUIRY`, assign
agent cua property/listing va notify agent.

Endpoint de xuat:

| Method | Path | Auth | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/search/listings/{listingId}/inquiries` | Public/Bearer | Tao lead tu form lien he listing |

Request:

```json
{
  "fullName": "Nguyen Van A",
  "email": "a@example.com",
  "phone": "+84901234567",
  "message": "Toi muon xem nha vao cuoi tuan",
  "preferredContactMethod": "PHONE"
}
```

Behavior:

- Tao/merge customer neu email/phone trung.
- Tao lead source `LISTING_INQUIRY`.
- Assign agent theo listing/property assigned agent.
- Tao notification cho agent.

## 8. Search, saved search va alert

### 8.1 Saved search

Trang thai hien tai: chua co.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/saved-searches` | CUSTOMER/AGENT/MANAGER/ADMIN | Luu bo loc search |
| GET | `/api/v1/saved-searches` | CUSTOMER/AGENT/MANAGER/ADMIN | List saved search cua user |
| PUT | `/api/v1/saved-searches/{savedSearchId}` | Owner | Sua saved search |
| DELETE | `/api/v1/saved-searches/{savedSearchId}` | Owner | Xoa saved search |
| PATCH | `/api/v1/saved-searches/{savedSearchId}/alerts` | Owner | Bat/tat alert |

Request:

```json
{
  "name": "Can ho 2PN Quan 1 duoi 5 ty",
  "criteria": {
    "purpose": "SALE",
    "propertyTypeId": 1,
    "districtId": 1,
    "maxPrice": 5000000000,
    "minBedrooms": 2
  },
  "alertEnabled": true,
  "alertFrequency": "DAILY"
}
```

Can them DB:

- `saved_searches`
- `saved_search_alert_logs`

### 8.2 Autocomplete va suggestion

Endpoint de xuat:

| Method | Path | Auth | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/search/suggestions` | Public/Bearer | Goi y keyword/location/listing |

Query:

```http
?q=nguyen&type=LOCATION,LISTING,PROPERTY
```

### 8.3 Geo search

Endpoint de xuat:

| Method | Path | Auth | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/search/listings/nearby` | Public | Search listing quanh toa do |

Query:

```http
?lat=10.7769&lng=106.7009&radiusKm=3&purpose=SALE
```

## 9. Customer CRM APIs can bo sung

### 9.1 Update/delete note va requirement

Trang thai hien tai: chi co add note/add requirement.

Ghi chu implemented: da bo sung update/delete note, pin/unpin note, update va
deactivate requirement cho AGENT/MANAGER/ADMIN, kem ownership/assignment rules
va audit log.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| PUT | `/api/v1/customers/{customerId}/notes/{noteId}` | AGENT/MANAGER/ADMIN | Sua note |
| DELETE | `/api/v1/customers/{customerId}/notes/{noteId}` | AGENT/MANAGER/ADMIN | Xoa note |
| PATCH | `/api/v1/customers/{customerId}/notes/{noteId}/pin` | AGENT/MANAGER/ADMIN | Pin/unpin note |
| PUT | `/api/v1/customers/{customerId}/requirements/{requirementId}` | AGENT/MANAGER/ADMIN | Sua nhu cau |
| DELETE | `/api/v1/customers/{customerId}/requirements/{requirementId}` | AGENT/MANAGER/ADMIN | Xoa/deactivate nhu cau |

### 9.2 Customer tag management

Trang thai hien tai: entity `CustomerTag` co, nhung chua co API.

Ghi chu implemented: da bo sung list/add/delete tag cho customer,
AGENT/MANAGER/ADMIN, kem ownership/assignment rules va audit log.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/customers/{customerId}/tags` | AGENT/MANAGER/ADMIN | List tag |
| POST | `/api/v1/customers/{customerId}/tags` | AGENT/MANAGER/ADMIN | Them tag |
| DELETE | `/api/v1/customers/{customerId}/tags/{tagId}` | AGENT/MANAGER/ADMIN | Xoa tag |

Request:

```json
{
  "name": "Hot buyer",
  "color": "#D92D20"
}
```

### 9.3 Merge duplicate customers

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/customers/duplicates` | MANAGER/ADMIN | Goi y duplicate theo email/phone |
| POST | `/api/v1/customers/{targetCustomerId}/merge` | MANAGER/ADMIN | Merge customer khac vao target |

Request:

```json
{
  "sourceCustomerIds": [12, 15],
  "mergeNotes": true,
  "mergeRequirements": true,
  "mergeFavorites": true,
  "archiveSources": true
}
```

### 9.4 Customer self-service

Neu CUSTOMER login vao portal, nen co endpoint rieng khong yeu cau role agent.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/me/customer-profile` | CUSTOMER | Ho so CRM cua chinh customer |
| PUT | `/api/v1/me/customer-profile` | CUSTOMER | Sua nhu cau/lien he |
| GET | `/api/v1/me/requirements` | CUSTOMER | Nhu cau cua toi |
| POST | `/api/v1/me/requirements` | CUSTOMER | Tao nhu cau |
| PUT | `/api/v1/me/requirements/{requirementId}` | CUSTOMER | Sua nhu cau |

## 10. Lead va follow-up APIs can bo sung

### 10.1 Follow-up task CRUD

Trang thai hien tai: tao task tu lead co, nhung chua list/update/complete/cancel.

Ghi chu implemented: da bo sung search, my tasks, detail, update, status update
va cancel/delete follow-up task cho AGENT/MANAGER/ADMIN, kem assignment rules
va audit log.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/follow-up-tasks` | AGENT/MANAGER/ADMIN | Search task |
| GET | `/api/v1/follow-up-tasks/my` | AGENT/MANAGER/ADMIN | Task cua toi |
| GET | `/api/v1/follow-up-tasks/{taskId}` | AGENT/MANAGER/ADMIN | Detail |
| PUT | `/api/v1/follow-up-tasks/{taskId}` | AGENT/MANAGER/ADMIN | Sua title/due/priority |
| PATCH | `/api/v1/follow-up-tasks/{taskId}/status` | AGENT/MANAGER/ADMIN | Doi status |
| DELETE | `/api/v1/follow-up-tasks/{taskId}` | AGENT/MANAGER/ADMIN | Cancel/delete task |

Status request:

```json
{
  "status": "COMPLETED",
  "completedAt": "2026-06-26T10:00:00Z"
}
```

### 10.2 Lead note/activity update/delete

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| PUT | `/api/v1/leads/{leadId}/notes/{noteId}` | AGENT/MANAGER/ADMIN | Sua note |
| DELETE | `/api/v1/leads/{leadId}/notes/{noteId}` | AGENT/MANAGER/ADMIN | Xoa note |
| DELETE | `/api/v1/leads/{leadId}/activities/{activityId}` | MANAGER/ADMIN | Xoa activity sai |

### 10.3 Convert lead

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/leads/{leadId}/convert/customer` | AGENT/MANAGER/ADMIN | Tao/link customer tu lead |
| POST | `/api/v1/leads/{leadId}/convert/transaction` | AGENT/MANAGER/ADMIN | Chuyen lead won thanh transaction |

Request convert customer:

```json
{
  "existingCustomerId": null,
  "createRequirement": true
}
```

### 10.4 Lead import/export

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/leads/import` | MANAGER/ADMIN | Import lead CSV/Excel |
| GET | `/api/v1/leads/export` | MANAGER/ADMIN | Export lead theo filter |

## 11. Appointment APIs can bo sung

### 11.1 Availability va conflict check

Trang thai hien tai: create appointment co the co business rule trong service,
nhung chua co API de frontend xem slot truoc.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/appointments/availability` | CUSTOMER/AGENT/MANAGER/ADMIN | Xem slot trong cua agent/property |
| POST | `/api/v1/appointments/conflicts` | AGENT/MANAGER/ADMIN | Check conflict truoc khi tao |

Query availability:

```http
?agentId=10&propertyId=5&from=2026-07-01T00:00:00Z&to=2026-07-07T23:59:59Z
```

### 11.2 Participant response

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| PATCH | `/api/v1/appointments/{appointmentId}/participants/{participantId}/response` | Participant | Accept/decline/tentative |

Request:

```json
{
  "responseStatus": "ACCEPTED",
  "notes": "I will arrive 10 minutes early"
}
```

### 11.3 No-show va feedback read/update

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| PATCH | `/api/v1/appointments/{appointmentId}/no-show` | AGENT/MANAGER/ADMIN | Danh dau khach khong den |
| GET | `/api/v1/appointments/{appointmentId}/feedback` | Participant/Agent | Xem feedback |
| PUT | `/api/v1/appointments/{appointmentId}/feedback/{feedbackId}` | Author/Manager | Sua feedback |

## 12. Contract APIs can bo sung

### 12.1 Contract template management

Trang thai hien tai: co entity template, chua co controller CRUD.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/contract-templates` | AGENT/MANAGER/ADMIN | List template active |
| GET | `/api/v1/contract-templates/{templateId}` | AGENT/MANAGER/ADMIN | Detail template |
| POST | `/api/v1/contract-templates` | MANAGER/ADMIN | Tao template |
| PUT | `/api/v1/contract-templates/{templateId}` | MANAGER/ADMIN | Sua template |
| PATCH | `/api/v1/contract-templates/{templateId}/active` | MANAGER/ADMIN | Bat/tat template |

### 12.2 Contract parties

Trang thai hien tai: parties duoc tao qua contract create, nhung chua co API
quan ly rieng.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/contracts/{contractId}/parties` | AGENT/MANAGER/ADMIN | List parties |
| POST | `/api/v1/contracts/{contractId}/parties` | AGENT/MANAGER/ADMIN | Them party |
| PUT | `/api/v1/contracts/{contractId}/parties/{partyId}` | AGENT/MANAGER/ADMIN | Sua party |
| DELETE | `/api/v1/contracts/{contractId}/parties/{partyId}` | AGENT/MANAGER/ADMIN | Xoa party khi draft |

### 12.3 Contract documents

Trang thai hien tai: upload document co, nhung chua list/download/delete/version.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/contracts/{contractId}/documents` | AGENT/MANAGER/ADMIN | List documents |
| GET | `/api/v1/contracts/{contractId}/documents/{documentId}/download` | AGENT/MANAGER/ADMIN | Download |
| PATCH | `/api/v1/contracts/{contractId}/documents/{documentId}` | AGENT/MANAGER/ADMIN | Sua metadata |
| DELETE | `/api/v1/contracts/{contractId}/documents/{documentId}` | AGENT/MANAGER/ADMIN | Xoa document |

### 12.4 Contract signature workflow

Trang thai hien tai: `mark-signed` dang don gian, chua co signature detail.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/contracts/{contractId}/signatures` | AGENT/MANAGER/ADMIN | List signature status |
| POST | `/api/v1/contracts/{contractId}/signature-requests` | AGENT/MANAGER/ADMIN | Gui yeu cau ky |
| PATCH | `/api/v1/contracts/{contractId}/signatures/{signatureId}/sign` | Signer | Ky dien tu/upload |
| PATCH | `/api/v1/contracts/{contractId}/signatures/{signatureId}/decline` | Signer | Tu choi ky |
| PATCH | `/api/v1/contracts/{contractId}/terminate` | MANAGER/ADMIN | Cham dut contract active |

## 13. Transaction, payment, invoice APIs can bo sung

### 13.1 List va detail sub-resources

Trang thai hien tai: tao deposit/schedule/payment/invoice/receipt co, nhung
chua co list/detail rieng.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/transactions/{transactionId}/deposits` | AGENT/MANAGER/ADMIN | List deposits |
| GET | `/api/v1/transactions/{transactionId}/payment-schedules` | AGENT/MANAGER/ADMIN | List schedules |
| GET | `/api/v1/transactions/{transactionId}/payments` | AGENT/MANAGER/ADMIN | List payments |
| GET | `/api/v1/transactions/{transactionId}/invoices` | AGENT/MANAGER/ADMIN | List invoices |
| GET | `/api/v1/transactions/{transactionId}/receipts` | AGENT/MANAGER/ADMIN | List receipts |

### 13.2 Deposit lifecycle

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| PATCH | `/api/v1/transactions/{transactionId}/deposits/{depositId}/verify` | MANAGER/ADMIN | Xac minh coc |
| PATCH | `/api/v1/transactions/{transactionId}/deposits/{depositId}/refund` | MANAGER/ADMIN | Hoan coc |
| PATCH | `/api/v1/transactions/{transactionId}/deposits/{depositId}/cancel` | MANAGER/ADMIN | Huy coc |

Refund request:

```json
{
  "refundAmount": 50000000,
  "refundReason": "Customer cancelled within allowed period"
}
```

### 13.3 Payment lifecycle

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| PATCH | `/api/v1/transactions/{transactionId}/payments/{paymentId}/confirm` | MANAGER/ADMIN | Confirm payment |
| PATCH | `/api/v1/transactions/{transactionId}/payments/{paymentId}/fail` | MANAGER/ADMIN | Mark failed |
| PATCH | `/api/v1/transactions/{transactionId}/payments/{paymentId}/refund` | MANAGER/ADMIN | Refund payment |
| PATCH | `/api/v1/transactions/{transactionId}/payment-schedules/{scheduleId}` | AGENT/MANAGER/ADMIN | Sua lich thanh toan |
| PATCH | `/api/v1/transactions/{transactionId}/payment-schedules/{scheduleId}/status` | MANAGER/ADMIN | Mark paid/overdue/cancel |

### 13.4 Invoice/receipt PDF va void

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/transactions/{transactionId}/invoices/{invoiceId}/pdf` | AGENT/MANAGER/ADMIN | Download PDF invoice |
| PATCH | `/api/v1/transactions/{transactionId}/invoices/{invoiceId}/void` | MANAGER/ADMIN | Void invoice |
| GET | `/api/v1/transactions/{transactionId}/receipts/{receiptId}/pdf` | AGENT/MANAGER/ADMIN | Download PDF receipt |

### 13.5 Payment gateway/webhook

Neu he thong co online payment, can bo sung:

| Method | Path | Auth | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/payments/checkout` | Bearer | Tao checkout/payment intent |
| POST | `/api/v1/payments/webhooks/{provider}` | Provider signature | Nhan webhook |
| GET | `/api/v1/payments/{paymentId}/status` | Bearer | Check payment status |

## 14. Commission APIs can bo sung

### 14.1 Approve/cancel/recalculate

Trang thai hien tai: co list va mark-paid, chua co approve/cancel/recalculate.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| PATCH | `/api/v1/commissions/{commissionId}/approve` | MANAGER/ADMIN | Duyet commission |
| PATCH | `/api/v1/commissions/{commissionId}/cancel` | MANAGER/ADMIN | Huy commission |
| POST | `/api/v1/transactions/{transactionId}/commissions/recalculate` | MANAGER/ADMIN | Tinh lai commission |

Cancel request:

```json
{
  "reason": "Transaction cancelled before completion"
}
```

### 14.2 Commission split

Neu co co-broker/team commission:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/transactions/{transactionId}/commission-splits` | MANAGER/ADMIN | Chia hoa hong |
| GET | `/api/v1/transactions/{transactionId}/commission-splits` | MANAGER/ADMIN | Xem split |

Can them DB:

- `commission_splits`
- beneficiary user, percentage/fixed amount, role in deal.

## 15. Owner portal

Trang thai hien tai: role `OWNER` co trong auth, nhung portal/API rieng hau nhu
chua co.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/owner/dashboard` | OWNER | Tong quan BDS/tin/giao dich |
| GET | `/api/v1/owner/properties` | OWNER | BDS cua toi |
| GET | `/api/v1/owner/listings` | OWNER | Tin dang cua BDS cua toi |
| GET | `/api/v1/owner/appointments` | OWNER | Lich xem nha lien quan |
| GET | `/api/v1/owner/contracts` | OWNER | Hop dong lien quan |
| GET | `/api/v1/owner/transactions` | OWNER | Giao dich/thanh toan lien quan |
| PATCH | `/api/v1/owner/appointments/{appointmentId}/response` | OWNER | Owner accept/decline lich xem |

Quyen:

- Owner chi xem duoc records co `owner_id = currentUser.id`.
- Khong cho owner sua listing da published truc tiep neu can manager review.

## 16. Organization, team va assignment

Trang thai hien tai: chua co agency/team despite product plan co neu.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/organizations/agencies` | ADMIN | List agency |
| POST | `/api/v1/organizations/agencies` | ADMIN | Tao agency |
| PUT | `/api/v1/organizations/agencies/{agencyId}` | ADMIN | Sua agency |
| GET | `/api/v1/organizations/teams` | ADMIN/MANAGER | List team |
| POST | `/api/v1/organizations/teams` | ADMIN/MANAGER | Tao team |
| PUT | `/api/v1/organizations/teams/{teamId}` | ADMIN/MANAGER | Sua team |
| POST | `/api/v1/organizations/teams/{teamId}/members` | ADMIN/MANAGER | Them member |
| DELETE | `/api/v1/organizations/teams/{teamId}/members/{userId}` | ADMIN/MANAGER | Xoa member |

Assignment rules:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/assignment-rules` | MANAGER/ADMIN | List rule chia lead |
| POST | `/api/v1/assignment-rules` | MANAGER/ADMIN | Tao rule |
| PUT | `/api/v1/assignment-rules/{ruleId}` | MANAGER/ADMIN | Sua rule |

## 17. Notification va email APIs can bo sung

### 17.1 Notification templates

Trang thai hien tai: templates co DB seed, chua co API quan ly.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/admin/notification-templates` | ADMIN/MANAGER | List template |
| POST | `/api/v1/admin/notification-templates` | ADMIN | Tao template |
| PUT | `/api/v1/admin/notification-templates/{templateId}` | ADMIN | Sua template |
| PATCH | `/api/v1/admin/notification-templates/{templateId}/active` | ADMIN | Bat/tat |

### 17.2 Email logs

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/admin/email-logs` | ADMIN/MANAGER | Search email log |
| GET | `/api/v1/admin/email-logs/{emailLogId}` | ADMIN/MANAGER | Detail |
| POST | `/api/v1/admin/email-logs/{emailLogId}/retry` | ADMIN/MANAGER | Gui lai email failed |

### 17.3 Real-time notifications

Endpoint/giao thuc de xuat:

| Method | Path | Auth | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/notifications/stream` | Bearer | SSE stream notification |

Hoac WebSocket:

```text
/ws/notifications
```

## 18. File APIs can bo sung

Trang thai hien tai: chi co `POST /api/v1/files/upload`.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/files/{fileId}` | Owner/authorized | Metadata file |
| GET | `/api/v1/files/{fileId}/download` | Owner/authorized | Download/private signed URL |
| DELETE | `/api/v1/files/{fileId}` | Owner/authorized | Delete file neu chua linked |
| PATCH | `/api/v1/files/{fileId}/access-level` | ADMIN/MANAGER | Doi public/private |

Access rule:

- PUBLIC file co the tra direct URL.
- PRIVATE file phai check permission theo resource lien ket.
- Khong cho delete file da linked voi contract signed/legal document verified,
  tru khi admin force va co audit.

## 19. Reports va export

Trang thai hien tai: co report APIs co ban, chua co export va scheduling.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/reports/revenue/export` | MANAGER/ADMIN | Export CSV/XLSX |
| GET | `/api/v1/reports/leads/export` | MANAGER/ADMIN | Export lead report |
| GET | `/api/v1/reports/transactions/export` | MANAGER/ADMIN | Export transaction report |
| GET | `/api/v1/reports/commissions/export` | MANAGER/ADMIN | Export commission report |
| POST | `/api/v1/reports/schedules` | ADMIN/MANAGER | Len lich gui report |
| GET | `/api/v1/reports/schedules` | ADMIN/MANAGER | List scheduled reports |

## 20. Audit APIs can bo sung

Trang thai hien tai: co search/detail.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/audit-logs/export` | ADMIN | Export audit logs |
| GET | `/api/v1/audit-logs/resources/{resourceType}/{resourceId}` | ADMIN/MANAGER | Timeline audit cua resource |

Can can nhac:

- Mask sensitive fields trong old/new JSON.
- Retention policy.
- Tamper-resistant logs neu production nghiem tuc.

## 21. AI APIs can bo sung

### 21.1 Price insight

Trang thai hien tai: product plan co de cap, code chua co.

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/ai/properties/{propertyId}/price-insight` | AGENT/MANAGER/ADMIN | Goi y khoang gia |

Request:

```json
{
  "purpose": "SALE",
  "includeComparableListings": true,
  "forceRefresh": false
}
```

Response:

```json
{
  "suggestedMinPrice": 11500000000,
  "suggestedMaxPrice": 13000000000,
  "currency": "VND",
  "confidence": 0.76,
  "reason": "Comparable District 1 apartments with similar area are listed in this range.",
  "comparableListings": []
}
```

### 21.2 AI generated content history

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/ai/request-logs` | ADMIN/MANAGER | Search AI request logs |
| GET | `/api/v1/ai/listings/{listingId}/generated-content` | AGENT/MANAGER/ADMIN | Lich su content generated |
| POST | `/api/v1/ai/listings/{listingId}/generated-content/{contentId}/apply` | AGENT/MANAGER/ADMIN | Apply content vao listing draft |

### 21.3 Chatbot action APIs

Hien chat co session/message, nhung neu muon chatbot thuc te can action:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/ai/chat/sessions/{sessionId}/create-lead` | CUSTOMER/AGENT | Tao lead tu chat |
| POST | `/api/v1/ai/chat/sessions/{sessionId}/book-appointment` | CUSTOMER/AGENT | Tao lich hen tu chat |

## 22. Public/customer-facing APIs can bo sung

### 22.1 Public appointment request

Ghi chu implemented: da bo sung
`POST /api/v1/search/listings/{listingId}/appointment-requests`, chap nhan
Public/Bearer, merge/tao customer, tao lead source `LISTING_INQUIRY`, tao
appointment `PENDING` va notify assigned agent.

Endpoint de xuat:

| Method | Path | Auth | Mo ta |
| --- | --- | --- | --- |
| POST | `/api/v1/search/listings/{listingId}/appointment-requests` | Public/Bearer | Khach yeu cau xem nha |

Request:

```json
{
  "fullName": "Nguyen Van A",
  "email": "a@example.com",
  "phone": "+84901234567",
  "preferredStartAt": "2026-07-02T09:00:00Z",
  "preferredEndAt": "2026-07-02T10:00:00Z",
  "message": "Toi muon xem nha buoi sang"
}
```

Behavior:

- Tao lead neu chua co.
- Tao appointment status `PENDING`.
- Notify assigned agent.

### 22.2 My transactions/contracts for customer

Endpoint de xuat:

| Method | Path | Role | Mo ta |
| --- | --- | --- | --- |
| GET | `/api/v1/me/appointments` | CUSTOMER/OWNER | Lich hen cua toi |
| GET | `/api/v1/me/contracts` | CUSTOMER/OWNER | Hop dong cua toi |
| GET | `/api/v1/me/transactions` | CUSTOMER/OWNER | Giao dich cua toi |
| GET | `/api/v1/me/payments` | CUSTOMER/OWNER | Thanh toan cua toi |

Ly do:

- Khong nen de customer goi endpoint admin/agent nhu `/api/v1/transactions`.
- Can response da mask truong noi bo: commission, internal notes, audit, owner
  private info.

## 23. Checklist Definition of Done cho moi API bo sung

Moi API moi nen co:

- Migration neu can bang/cot/index moi.
- Entity/repository/service/controller hoac update service hien co.
- Request/response DTO rieng, khong expose entity truc tiep.
- Bean Validation.
- Authorization rule bang `@PreAuthorize` va check owner/resource trong service.
- Audit log cho action quan trong.
- Swagger/OpenAPI annotation ro request/response.
- Integration test cho happy path, validation, forbidden, not found.
- Cap nhat `docs/API_FRONTEND_REFERENCE.md` sau khi implement.

## 24. Thu tu lam de co gia tri nhanh

1. Lam current account APIs: profile, change password, avatar.
2. Lam master-data read APIs cho frontend dropdown.
3. Lam internal listing list/detail.
4. Lam property legal documents.
5. Lam update/delete CRM notes/requirements/tags va follow-up task CRUD.
6. Lam file detail/download/delete.
7. Lam public inquiry va appointment request tu listing.
8. Lam owner portal read APIs.
9. Lam contract template/document/signature APIs day du.
10. Lam payment lifecycle va report export.
