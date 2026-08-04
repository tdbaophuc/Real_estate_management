# UI Screen Design Spec - Real Estate Management

> Muc dich: dung lam checklist thiet ke tat ca man hinh tren Google Stitch/Figma.
> Cap nhat theo backend hien co trong source code ngay 01/07/2026.
> Base URL local: `http://localhost:8081`, API prefix: `/api/v1`.

## 1. Quy uoc chung cho thiet ke

### Role nguoi dung

| Role | Nhom man hinh chinh |
| --- | --- |
| `GUEST` | Trang chu/tim kiem, chi tiet tin dang, dang nhap, dang ky, form lien he/dat lich |
| `CUSTOMER` | Trang ca nhan khach hang, yeu thich, lich hen cua toi, AI chat, thong bao |
| `AGENT` | Dashboard agent, property, listing, customer CRM, lead, lich hen, hop dong, giao dich, hoa hong cua toi, AI tools |
| `MANAGER` | Tat ca nghiep vu cua agent + duyet listing/hop dong, commission rules, reports, team dashboard |
| `ADMIN` | Tat ca + quan ly user, audit log, admin dashboard |
| `OWNER` | Role da co trong auth/AI chat, backend chua co owner portal rieng |

### Response chung

Tat ca API tra wrapper:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request processed successfully",
  "data": {},
  "timestamp": "2026-07-01T09:30:00Z"
}
```

Man hinh chi render du lieu nghiep vu tu `data`.

Phan trang:

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

## 2. Sitemap tong quan

### Public/GUEST

| Route UI | Man hinh |
| --- | --- |
| `/` | Trang chu tim kiem bat dong san |
| `/search` | Ket qua tim kiem listing |
| `/listing/:slug` | Chi tiet listing cong khai |
| `/listing/:slug/inquiry` | Modal/form yeu cau tu van |
| `/listing/:slug/appointment-request` | Modal/form dat lich xem nha |
| `/login` | Dang nhap |
| `/register` | Dang ky |

### CUSTOMER

| Route UI | Man hinh |
| --- | --- |
| `/customer/dashboard` | Tong quan ca nhan |
| `/customer/favorites` | Listing yeu thich |
| `/customer/appointments` | Lich hen cua toi |
| `/customer/appointments/:id` | Chi tiet lich hen |
| `/customer/ai-chat` | Chat AI tu van |
| `/customer/profile` | Ho so ca nhan |
| `/customer/security` | Doi mat khau, quan ly phien dang nhap |
| `/notifications` | Trung tam thong bao |

### Back office: AGENT/MANAGER/ADMIN

| Route UI | Man hinh |
| --- | --- |
| `/dashboard` | Dashboard theo role |
| `/properties` | Danh sach property |
| `/properties/new` | Tao property |
| `/properties/:id` | Chi tiet property |
| `/properties/:id/edit` | Sua property |
| `/properties/:id/images` | Quan ly anh property |
| `/properties/:id/legal-documents` | Quan ly ho so phap ly |
| `/listings` | Danh sach listing noi bo |
| `/listings/new` | Tao listing |
| `/listings/:id` | Chi tiet listing noi bo |
| `/listings/:id/edit` | Sua listing |
| `/listings/review` | Hang doi duyet listing |
| `/customers` | Danh sach customer CRM |
| `/customers/new` | Tao customer |
| `/customers/:id` | Chi tiet customer 360 |
| `/customers/:id/edit` | Sua customer |
| `/leads` | Pipeline/list lead |
| `/leads/:id` | Chi tiet lead |
| `/follow-up-tasks` | Viec can lam |
| `/appointments` | Lich hen/list/calendar |
| `/appointments/:id` | Chi tiet lich hen |
| `/contracts` | Danh sach hop dong |
| `/contracts/new` | Tao hop dong |
| `/contracts/:id` | Chi tiet hop dong |
| `/transactions` | Danh sach giao dich |
| `/transactions/new` | Tao giao dich |
| `/transactions/:id` | Chi tiet giao dich/thanh toan |
| `/commissions` | Hoa hong |
| `/commission-rules` | Cau hinh rule hoa hong |
| `/reports` | Bao cao |
| `/ai` | AI workspace |
| `/files` | Upload/file metadata neu can man hinh rieng |
| `/profile` | Ho so nhan vien |
| `/security` | Bao mat tai khoan |
| `/notifications` | Trung tam thong bao |

### ADMIN rieng

| Route UI | Man hinh |
| --- | --- |
| `/admin/users` | Quan ly nguoi dung |
| `/admin/users/:id` | Chi tiet nguoi dung |
| `/admin/audit-logs` | Tra cuu audit log |
| `/admin/audit-logs/:id` | Chi tiet audit log |

## 3. Man hinh xac thuc va tai khoan

### 3.1 Dang ky

- Route: `/register`
- Doi tuong: Guest.
- Chuc nang: tao tai khoan moi.
- API:
  - `POST /api/v1/auth/register`
- Input chinh: `email`, `password`, `fullName`, co the co `phone`.
- Output data: `RegisterResponse` gom thong tin user moi nhu `id`, `email`, `fullName`, `status`, `roles`, `createdAt`.
- Trang thai UI can co: form validation, password strength, success screen/redirect login, error email trung.

### 3.2 Dang nhap

- Route: `/login`
- Doi tuong: Guest.
- Chuc nang: dang nhap va luu token.
- API:
  - `POST /api/v1/auth/login`
- Input: `email`, `password`.
- Output data: `LoginResponse` gom `accessToken`, `refreshToken`, `expiresInSeconds`, `user`.
- UI sau login: dieu huong theo role den dashboard/customer.

### 3.3 Refresh token va logout

- Man hinh: xu ly ngam trong app shell.
- API:
  - `POST /api/v1/auth/refresh-token`
  - `POST /api/v1/auth/logout`
- Output refresh: access token moi, refresh token moi/hoac token hien hanh, expiry.
- Output logout: success message.

### 3.4 Ho so ca nhan

- Route: `/profile`, `/customer/profile`
- Chuc nang: xem/sua thong tin user hien tai, avatar.
- API:
  - `GET /api/v1/auth/me`
  - `PATCH /api/v1/auth/me/profile`
  - `POST /api/v1/auth/me/avatar`
  - `DELETE /api/v1/auth/me/avatar`
- Output data: `AuthUserResponse` gom `id`, `email`, `fullName`, `phone`, `status`, `roles`, `permissions`, `avatarUrl`, `createdAt`.
- UI: profile form, avatar uploader, remove avatar button.

### 3.5 Bao mat tai khoan va phien dang nhap

- Route: `/security`, `/customer/security`
- Chuc nang: doi mat khau, xem/revoke phien dang nhap.
- API:
  - `POST /api/v1/auth/me/change-password`
  - `GET /api/v1/auth/me/sessions`
  - `DELETE /api/v1/auth/me/sessions/{sessionId}`
  - `DELETE /api/v1/auth/me/sessions`
- Output sessions: danh sach `SessionResponse` gom `id`, `createdAt`, `expiresAt`.
- UI: form doi mat khau, bang sessions, revoke one/all.

## 4. Public listing va guest flow

### 4.1 Trang chu tim kiem

- Route: `/`
- Chuc nang: search nhanh listing public, hero co search bar, goi y listing moi/noi bat.
- API:
  - `GET /api/v1/search/listings`
  - `GET /api/v1/master-data/provinces`
  - `GET /api/v1/master-data/property-types`
  - `GET /api/v1/master-data/amenities`
- Output listing: `PageResponse<PublicListingResponse>` gom `id`, `code`, `title`, `slug`, `description`, `purpose`, `askingPrice`, `currency`, `bedrooms`, `bathrooms`, `landArea`, `floorArea`, `provinceName`, `districtName`, `wardName`, `fullAddress`, `viewCount`, `publishedAt`, anh/cover neu co.
- UI: search input, filter purpose, location, price, bedrooms, property type, listing cards.

### 4.2 Ket qua tim kiem

- Route: `/search`
- Chuc nang: loc/sap xep listing public.
- API:
  - `GET /api/v1/search/listings`
- Query hay dung: `keyword`, `purpose`, `propertyTypeId`, `provinceId`, `districtId`, `wardId`, `minPrice`, `maxPrice`, `minArea`, `maxArea`, `bedrooms`, `bathrooms`, `page`, `size`, `sortBy`, `direction`.
- Output: page listing public.
- UI: sidebar/bottom sheet filter, map/list toggle neu thiet ke, sort select, pagination/infinite scroll.

### 4.3 Chi tiet listing public

- Route: `/listing/:slug`
- Chuc nang: xem anh, thong tin gia/dien tich, tien ich, vi tri, thong tin lien he, tin lien quan.
- API:
  - `GET /api/v1/search/listings/{slug}`
  - Header tuy chon: `X-Session-Id`
- Output: `PublicListingResponse` chi tiet gom field listing + property summary + images + address + amenities + agent/contact info neu backend tra.
- UI: gallery, gia, CTA yeu cau tu van/dat lich, favorite neu da login.

### 4.4 Gui yeu cau tu van

- Route: modal tren `/listing/:slug`
- Chuc nang: guest/customer gui thong tin lien he, backend tao/merge customer va tao lead.
- API:
  - `POST /api/v1/search/listings/{listingId}/inquiries`
- Input: `fullName`, `email`, `phone`, `message`, `preferredContactMethod`.
- Output data: lead/customer/listing response theo service, toi thieu success state de UI hien "da gui yeu cau".
- UI: form ngan, validate email/phone, success modal.

### 4.5 Dat lich xem nha public

- Route: modal tren `/listing/:slug`
- Chuc nang: gui yeu cau lich xem nha, backend tao customer + lead + appointment `PENDING`.
- API:
  - `POST /api/v1/search/listings/{listingId}/appointment-requests`
- Input: `fullName`, `email`, `phone`, `preferredStartAt`, `preferredEndAt`, `message`.
- Output data: appointment/lead created summary.
- UI: date-time picker, message, success state "cho agent xac nhan".

## 5. Customer portal

### 5.1 Customer dashboard

- Route: `/customer/dashboard`
- Chuc nang: tong quan tin yeu thich, lich hen sap toi, goi y AI/chat entry, thong bao moi.
- API:
  - `GET /api/v1/listings/favorites`
  - `GET /api/v1/appointments/my`
  - `GET /api/v1/notifications/unread-count`
- Output: favorite listing page, appointment page, unread count.
- UI: cards tom tat, lich hen gan nhat, listing da luu.

### 5.2 Listing yeu thich

- Route: `/customer/favorites`
- Chuc nang: xem/bo yeu thich.
- API:
  - `GET /api/v1/listings/favorites`
  - `POST /api/v1/listings/{listingId}/favorite`
  - `DELETE /api/v1/listings/{listingId}/favorite`
- Output: page listing da favorite.
- UI: grid/list cards, remove favorite.

### 5.3 Lich hen cua toi

- Route: `/customer/appointments`, `/customer/appointments/:id`
- Chuc nang: xem lich hen cua customer, xac nhan/huy/doi lich neu duoc phep.
- API:
  - `GET /api/v1/appointments/my`
  - `GET /api/v1/appointments/{appointmentId}`
  - `PATCH /api/v1/appointments/{appointmentId}/confirm`
  - `PATCH /api/v1/appointments/{appointmentId}/cancel`
  - `PATCH /api/v1/appointments/{appointmentId}/reschedule`
  - `POST /api/v1/appointments/{appointmentId}/feedback`
- Output: `AppointmentResponse` gom `id`, `code`, `title`, `status`, `startAt`, `endAt`, `timezone`, `meetingLocation`, `customer`, `agent`, `property`, `listing`, `participants`, `feedback`.
- UI: calendar/list, detail drawer, cancel reason, reschedule form, feedback form.

### 5.4 AI chat tu van

- Route: `/customer/ai-chat`
- Chuc nang: tao session, chat, nhan suggested listings.
- API:
  - `POST /api/v1/ai/chat/sessions`
  - `POST /api/v1/ai/chat/sessions/{sessionId}/messages`
  - `GET /api/v1/ai/chat/sessions/{sessionId}`
- Output: `ChatSessionResponse` gom `id`, `title`, `status`, `messages`, `suggestedListings`.
- UI: chat thread, listing cards trong message panel, empty state.

## 6. Dashboard va reports

### 6.1 Dashboard Admin

- Route: `/dashboard` voi role `ADMIN`.
- API:
  - `GET /api/v1/dashboard/admin`
- Output: `AdminDashboardResponse` gom cac KPI tong quan nhu users, properties, listings, pending reviews, revenue, leads, transactions.
- UI: KPI grid, charts, pending approval panels, recent activity.

### 6.2 Dashboard Manager

- Route: `/dashboard` voi role `MANAGER`.
- API:
  - `GET /api/v1/dashboard/manager`
- Output: `ManagerDashboardResponse` gom team performance, lead pipeline, revenue, transaction/commission overview, top agents.
- UI: team KPI, funnel lead, top agent table, report shortcuts.

### 6.3 Dashboard Agent

- Route: `/dashboard` voi role `AGENT`.
- API:
  - `GET /api/v1/dashboard/agent`
- Output: `AgentDashboardResponse` gom assigned leads, appointments today, follow-up tasks, transactions in progress, commission.
- UI: today agenda, task list, lead funnel, personal commission summary.

### 6.4 Reports

- Route: `/reports`
- Role: `MANAGER`, `ADMIN`.
- API:
  - `GET /api/v1/reports/revenue?from=yyyy-MM-dd&to=yyyy-MM-dd`
  - `GET /api/v1/reports/leads?from=yyyy-MM-dd&to=yyyy-MM-dd`
  - `GET /api/v1/reports/transactions?from=yyyy-MM-dd&to=yyyy-MM-dd`
  - `GET /api/v1/reports/commissions?from=yyyy-MM-dd&to=yyyy-MM-dd`
- Output: `RevenueReportResponse`, `LeadReportResponse`, `TransactionReportResponse`, `CommissionReportResponse`, gom series/count/amount/top lists.
- UI: date range filter, tab report, charts, summary cards.

## 7. Property management

### 7.1 Danh sach property

- Route: `/properties`
- Role: `AGENT`, `MANAGER`, `ADMIN`.
- Chuc nang: search, filter, sort, vao detail/edit.
- API:
  - `GET /api/v1/properties`
- Output: `PageResponse<PropertyResponse>` gom `id`, `code`, `name`, `status`, `purpose`, `price`, `currency`, `propertyType`, `address`, `landArea`, `floorArea`, `bedrooms`, `bathrooms`, `assignedAgent`, `coverImageUrl`, `createdAt`, `updatedAt`.
- UI: table/list cards, status badge, filters location/type/status/purpose/price.

### 7.2 Tao/sua property

- Route: `/properties/new`, `/properties/:id/edit`
- API:
  - `POST /api/v1/properties`
  - `PUT /api/v1/properties/{propertyId}`
  - `GET /api/v1/master-data/*`
- Input: `PropertyUpsertRequest` gom thong tin co ban, gia, dien tich, phong, phap ly, noi that, ngay san sang, owner/agent, address, amenities.
- Output: `PropertyResponse`.
- UI: multi-step form: thong tin co ban, dia chi, gia/dien tich, tien ich, phap ly, agent.

### 7.3 Chi tiet property

- Route: `/properties/:id`
- API:
  - `GET /api/v1/properties/{propertyId}`
  - `PATCH /api/v1/properties/{propertyId}/status`
  - `DELETE /api/v1/properties/{propertyId}`
- Output: `PropertyResponse` chi tiet.
- UI: overview, image strip, legal status, amenities, linked listings/contracts/transactions neu co, action đổi status/soft delete.

### 7.4 Quan ly anh property

- Route: `/properties/:id/images`
- API:
  - `GET /api/v1/properties/{propertyId}/images`
  - `POST /api/v1/properties/{propertyId}/images`
  - `PATCH /api/v1/properties/{propertyId}/images/{imageId}`
  - `PATCH /api/v1/properties/{propertyId}/cover-image/{imageId}`
  - `PUT /api/v1/properties/{propertyId}/images/reorder`
  - `DELETE /api/v1/properties/{propertyId}/images/{imageId}`
- Output: `PropertyImageResponse` gom `id`, `url/publicUrl`, `altText`, `displayOrder`, `cover`, `fileResource`.
- UI: uploader, drag reorder, set cover, edit alt text, delete.

### 7.5 Quan ly ho so phap ly

- Route: `/properties/:id/legal-documents`
- API:
  - `GET /api/v1/properties/{propertyId}/legal-documents`
  - `POST /api/v1/properties/{propertyId}/legal-documents`
  - `GET /api/v1/properties/{propertyId}/legal-documents/{documentId}`
  - `PATCH /api/v1/properties/{propertyId}/legal-documents/{documentId}`
  - `PATCH /api/v1/properties/{propertyId}/legal-documents/{documentId}/verify`
  - `DELETE /api/v1/properties/{propertyId}/legal-documents/{documentId}`
- Output: `PropertyLegalDocumentResponse` gom document type/number, issuer, issue/expiry date, verification status, notes, file metadata.
- UI: table tai lieu, upload modal, verify/reject action, download.

## 8. Listing management

### 8.1 Danh sach listing noi bo

- Route: `/listings`
- API:
  - `GET /api/v1/listings`
- Output: `PageResponse<InternalListingDetailResponse>` gom listing fields, `property`, `creator`, `reviewer`, `listingPackage`, `statusHistory`, `viewCount`, `favoriteCount`.
- UI: table voi status workflow, filters status/purpose/creator/property/keyword.

### 8.2 Tao/sua listing

- Route: `/listings/new`, `/listings/:id/edit`
- API:
  - `POST /api/v1/listings`
  - `PUT /api/v1/listings/{listingId}`
  - `GET /api/v1/properties`
  - `GET /api/v1/master-data/listing-packages`
- Input: `propertyId`, `code`, `title`, `slug`, `description`, `purpose`, `visibility`, `askingPrice`, `currency`, `listingPackageId`, SEO fields.
- Output: `InternalListingDetailResponse`.
- UI: property picker, content/SEO editor, package selector, preview panel.

### 8.3 Chi tiet listing noi bo va workflow

- Route: `/listings/:id`
- API:
  - `GET /api/v1/listings/{listingId}`
  - `PATCH /api/v1/listings/{listingId}/submit`
  - `PATCH /api/v1/listings/{listingId}/approve`
  - `PATCH /api/v1/listings/{listingId}/reject`
  - `PATCH /api/v1/listings/{listingId}/publish`
  - `PATCH /api/v1/listings/{listingId}/unpublish`
- Output: `InternalListingDetailResponse`.
- UI: status timeline, preview public, action buttons theo role/status, reject reason modal.

### 8.4 Hang doi duyet listing

- Route: `/listings/review`
- Role: `MANAGER`, `ADMIN`.
- API:
  - `GET /api/v1/listings?status=PENDING_REVIEW`
  - approve/reject endpoints.
- Output: page listing cho duyet.
- UI: review queue, compare property/listing data, approve/reject.

## 9. Customer CRM

### 9.1 Danh sach customer

- Route: `/customers`
- API:
  - `GET /api/v1/customers`
- Output: `PageResponse<CustomerResponse>` gom `id`, `code`, `fullName`, `email`, `phone`, `status`, `source`, `priority`, `assignedAgent`, `createdAt`, `updatedAt`.
- UI: CRM table, filters status/source/priority/agent/keyword.

### 9.2 Tao/sua customer

- Route: `/customers/new`, `/customers/:id/edit`
- API:
  - `POST /api/v1/customers`
  - `PUT /api/v1/customers/{customerId}`
- Input: `code`, `fullName`, `email`, `phone`, `status`, `source`, `priority`, `preferredContactMethod`, `notes`, `userId`, `assignedAgentId`.
- Output: `CustomerResponse`.
- UI: contact form, assignment, priority/source/status.

### 9.3 Chi tiet customer 360

- Route: `/customers/:id`
- API:
  - `GET /api/v1/customers/{customerId}`
  - `GET /api/v1/customers/{customerId}/timeline`
  - `GET /api/v1/ai/customers/{customerId}/summary`
  - `POST /api/v1/ai/customers/{customerId}/recommendations`
- Output: `CustomerDetailResponse`, `CustomerTimelineItemResponse[]`, AI summary/recommendations.
- UI: profile header, notes, requirements, tags, timeline, AI insight, recommended listings.

### 9.4 Notes, requirements, tags

- Route: tabs trong `/customers/:id`
- API:
  - `POST /api/v1/customers/{customerId}/notes`
  - `PUT /api/v1/customers/{customerId}/notes/{noteId}`
  - `DELETE /api/v1/customers/{customerId}/notes/{noteId}`
  - `PATCH /api/v1/customers/{customerId}/notes/{noteId}/pin`
  - `POST /api/v1/customers/{customerId}/requirements`
  - `PUT /api/v1/customers/{customerId}/requirements/{requirementId}`
  - `DELETE /api/v1/customers/{customerId}/requirements/{requirementId}`
  - `GET /api/v1/customers/{customerId}/tags`
  - `POST /api/v1/customers/{customerId}/tags`
  - `DELETE /api/v1/customers/{customerId}/tags/{tagId}`
- Output: `CustomerNoteResponse`, `CustomerRequirementResponse`, `CustomerTagResponse`.
- UI: pinned notes, requirement cards, tag chips.

## 10. Lead va follow-up

### 10.1 Lead pipeline/list

- Route: `/leads`
- API:
  - `GET /api/v1/leads`
- Output: `PageResponse<LeadResponse>` gom `id`, `code`, `source`, `fullName`, `email`, `phone`, `priority`, `pipelineStatus`, `customer`, `listing`, `assignedAgent`, `createdAt`, `updatedAt`.
- UI: kanban theo `LeadPipelineStatus` hoac table, filters priority/status/source/agent.

### 10.2 Tao lead

- Route: modal/new trong `/leads`
- API:
  - `POST /api/v1/leads`
- Input: `code`, `sourceCode`, `fullName`, `email`, `phone`, `priority`, `message`, `customerId`, `listingId`, `assignedAgentId`.
- Output: `LeadDetailResponse` hoac `LeadResponse`.
- UI: quick create, link customer/listing.

### 10.3 Chi tiet lead

- Route: `/leads/:id`
- API:
  - `GET /api/v1/leads/{leadId}`
  - `PATCH /api/v1/leads/{leadId}/assign`
  - `PATCH /api/v1/leads/{leadId}/status`
  - `POST /api/v1/leads/{leadId}/notes`
  - `POST /api/v1/leads/{leadId}/activities`
  - `POST /api/v1/leads/{leadId}/follow-up-tasks`
  - `POST /api/v1/ai/leads/{leadId}/score`
- Output: `LeadDetailResponse`, `LeadAssignmentResponse`, `LeadNoteResponse`, `LeadActivityResponse`, `FollowUpTaskResponse`, `LeadScoreResponse`.
- UI: lead header, customer/listing context, activity timeline, notes, tasks, AI score panel.

### 10.4 Follow-up tasks

- Route: `/follow-up-tasks`
- API:
  - `GET /api/v1/follow-up-tasks`
  - `GET /api/v1/follow-up-tasks/my`
  - `GET /api/v1/follow-up-tasks/{taskId}`
  - `PUT /api/v1/follow-up-tasks/{taskId}`
  - `PATCH /api/v1/follow-up-tasks/{taskId}/status`
  - `DELETE /api/v1/follow-up-tasks/{taskId}`
- Output: `FollowUpTaskResponse` gom `id`, `title`, `description`, `status`, `priority`, `dueAt`, `completedAt`, `lead`, `assignedAgent`, `createdBy`.
- UI: my tasks, overdue/today/upcoming sections, status change.

## 11. Appointment management

### 11.1 Lich hen back office

- Route: `/appointments`
- API:
  - `GET /api/v1/appointments`
  - `GET /api/v1/appointments/my`
- Output: `PageResponse<AppointmentResponse>`.
- UI: calendar/list toggle, filters status/agent/customer/property/date range.

### 11.2 Tao lich hen

- Route: modal/new trong `/appointments`
- API:
  - `POST /api/v1/appointments`
- Input: `code`, `customerId`, `agentId`, `propertyId`, `listingId`, `leadId`, `title`, `startAt`, `endAt`, `timezone`, `meetingLocation`, `notes`.
- Output: `AppointmentResponse`.
- UI: date-time picker, customer/property/lead picker, conflict hint.

### 11.3 Chi tiet lich hen va feedback

- Route: `/appointments/:id`
- API:
  - `GET /api/v1/appointments/{appointmentId}`
  - `PATCH /api/v1/appointments/{appointmentId}/confirm`
  - `PATCH /api/v1/appointments/{appointmentId}/cancel`
  - `PATCH /api/v1/appointments/{appointmentId}/reschedule`
  - `PATCH /api/v1/appointments/{appointmentId}/complete`
  - `POST /api/v1/appointments/{appointmentId}/feedback`
- Output: `AppointmentResponse`, `ViewingFeedbackResponse`.
- UI: status actions, reschedule modal, cancel reason, viewing feedback form.

## 12. Contract management

### 12.1 Danh sach hop dong

- Route: `/contracts`
- API:
  - `GET /api/v1/contracts`
- Output: `PageResponse<ContractResponse>` gom `id`, `code`, `contractType`, `status`, `property`, `customer`, `agent`, `title`, `totalValue`, `currency`, `effectiveDate`, `expirationDate`.
- UI: table, filters status/type/property/customer/agent.

### 12.2 Tao/sua hop dong

- Route: `/contracts/new`, `/contracts/:id/edit`
- API:
  - `POST /api/v1/contracts`
  - `PUT /api/v1/contracts/{contractId}`
- Input: `code`, `contractType`, `propertyId`, `customerId`, `agentId`, `templateId`, `title`, `totalValue`, `currency`, `effectiveDate`, `expirationDate`, `terms`, `notes`.
- Output: `ContractResponse`.
- UI: linked property/customer, value/date fields, terms editor.

### 12.3 Chi tiet hop dong va workflow

- Route: `/contracts/:id`
- API:
  - `GET /api/v1/contracts/{contractId}`
  - `POST /api/v1/contracts/{contractId}/documents`
  - `PATCH /api/v1/contracts/{contractId}/submit-review`
  - `PATCH /api/v1/contracts/{contractId}/approve`
  - `PATCH /api/v1/contracts/{contractId}/mark-signed`
  - `PATCH /api/v1/contracts/{contractId}/cancel`
- Output: `ContractResponse`, `ContractDocumentResponse`.
- UI: status timeline, parties/documents, upload signed/final/attachment, approve/sign/cancel actions.

## 13. Transaction, payment, invoice, receipt

### 13.1 Danh sach giao dich

- Route: `/transactions`
- API:
  - `GET /api/v1/transactions`
- Output: `PageResponse<TransactionResponse>` gom `id`, `code`, `status`, `transactionType`, `contract`, `property`, `customer`, `agent`, `agreedValue`, `currency`, `transactionDate`, `expectedCompletionDate`.
- UI: table, filters status/type/customer/property/agent.

### 13.2 Tao giao dich

- Route: `/transactions/new`
- API:
  - `POST /api/v1/transactions`
- Input: `code`, `contractId`, `propertyId`, `customerId`, `agentId`, `transactionType`, `agreedValue`, `currency`, `transactionDate`, `expectedCompletionDate`, `notes`.
- Output: `TransactionResponse`.
- UI: create from contract, value/date, agent/customer summary.

### 13.3 Chi tiet giao dich

- Route: `/transactions/:id`
- API:
  - `GET /api/v1/transactions/{transactionId}`
  - `PATCH /api/v1/transactions/{transactionId}/status`
  - `POST /api/v1/transactions/{transactionId}/deposits`
  - `POST /api/v1/transactions/{transactionId}/payment-schedules`
  - `POST /api/v1/transactions/{transactionId}/payments`
  - `POST /api/v1/transactions/{transactionId}/invoices`
  - `POST /api/v1/transactions/{transactionId}/payments/{paymentId}/receipt`
- Output: `TransactionResponse` gom deposits, schedules, payments, invoices/receipts neu service tra nested; action endpoints tra `DepositResponse`, `PaymentScheduleResponse`, `PaymentResponse`, `InvoiceResponse`, `ReceiptResponse`.
- UI: financial timeline, deposit panel, schedules table, payments table, invoice/receipt modals, status action.

## 14. Commission

### 14.1 Hoa hong cua toi

- Route: `/commissions?scope=my`
- Role: `AGENT`, `MANAGER`, `ADMIN`.
- API:
  - `GET /api/v1/commissions/my`
- Output: `PageResponse<CommissionResponse>` gom `id`, `status`, `transaction`, `beneficiaryUser`, `amount`, `currency`, `calculationType`, `rate`, `paidAt`.
- UI: amount summary, status tabs, commission table.

### 14.2 Quan ly hoa hong

- Route: `/commissions`
- Role: `MANAGER`, `ADMIN`.
- API:
  - `GET /api/v1/commissions`
  - `PATCH /api/v1/commissions/{commissionId}/mark-paid`
- Output: `CommissionResponse`.
- UI: filters, mark paid modal with `paymentReference`, `paidAt`, `notes`.

### 14.3 Commission rules

- Route: `/commission-rules`
- Role: `MANAGER`, `ADMIN`.
- API:
  - `POST /api/v1/commission-rules`
  - `GET /api/v1/commission-rules`
  - `PUT /api/v1/commission-rules/{ruleId}`
- Output: `CommissionRuleResponse` gom `id`, `code`, `name`, `transactionType`, `calculationType`, `rate`, `fixedAmount`, `currency`, `priority`, `active`, `effectiveFrom`, `effectiveTo`.
- UI: rule table, create/edit drawer, active toggle.

## 15. Notification center

### 15.1 Thong bao

- Route: `/notifications`
- API:
  - `GET /api/v1/notifications`
  - `GET /api/v1/notifications/unread-count`
  - `PATCH /api/v1/notifications/{notificationId}/read`
  - `PATCH /api/v1/notifications/read-all`
- Output: `NotificationResponse` gom `id`, `title`, `message`, `channel`, `read`, `createdAt`, link/resource metadata neu co; `UnreadCountResponse`, `MarkAllReadResponse`.
- UI: notification bell badge, inbox list, mark one/all read.

## 16. Admin user management

### 16.1 Danh sach user

- Route: `/admin/users`
- Role: `ADMIN`.
- API:
  - `GET /api/v1/admin/users`
- Output: `PageResponse<UserManagementResponse>` gom `id`, `email`, `fullName`, `phone`, `status`, `roles`, `avatarUrl`, `createdAt`, `updatedAt`.
- UI: search/filter by role/status/keyword, user table.

### 16.2 Chi tiet user va phan quyen

- Route: `/admin/users/:id`
- API:
  - `GET /api/v1/admin/users/{userId}`
  - `PATCH /api/v1/admin/users/{userId}/status`
  - `PUT /api/v1/admin/users/{userId}/roles`
- Output: `UserManagementResponse`.
- UI: profile summary, status selector, role multi-select, activity hint.

## 17. Audit log

### 17.1 Tra cuu audit log

- Route: `/admin/audit-logs`
- Role: `ADMIN`.
- API:
  - `GET /api/v1/audit-logs`
- Query: `actorId`, `action`, `resourceType`, `resourceId`, `from`, `to`, `page`, `size`, `sortBy`, `direction`.
- Output: `PageResponse<AuditLogResponse>` gom `id`, `actor`, `action`, `resourceType`, `resourceId`, `oldValue`, `newValue`, `ipAddress`, `userAgent`, `createdAt`.
- UI: filter bar, audit table, JSON diff preview.

### 17.2 Chi tiet audit log

- Route: `/admin/audit-logs/:id`
- API:
  - `GET /api/v1/audit-logs/{auditLogId}`
- Output: `AuditLogResponse`.
- UI: metadata, old/new JSON viewer.

## 18. File management

### 18.1 Upload/file metadata

- Route: khong bat buoc rieng; dung trong avatar, property image, legal document, contract document. Neu can co route `/files`.
- API:
  - `POST /api/v1/files/upload`
  - `GET /api/v1/files/{fileId}`
  - `GET /api/v1/files/{fileId}/download`
  - `DELETE /api/v1/files/{fileId}`
  - `PATCH /api/v1/files/{fileId}/access-level`
- Output: `FileResourceResponse` gom `id`, `originalFilename`, `contentType`, `size`, `storageProvider`, `accessLevel`, `publicUrl`, `createdAt`, `uploadedBy`.
- UI: upload dropzone, file metadata, download/delete/access-level controls.

## 19. AI workspace

### 19.1 AI sinh mo ta listing

- Route: `/ai/listing-description` hoac panel trong listing form.
- API:
  - `POST /api/v1/ai/listing-description`
- Input: `propertyId` hoac `listingId`, `sellingPoints`, `tone`, `language`.
- Output: `ListingDescriptionResponse` gom `title`, `shortDescription`, `fullDescription`, `seoKeywords`, `socialMediaCaption`, `fallbackUsed`, `aiStatus`, `provider`, `model`, `errorMessage`.
- UI: generate button, result preview, copy/apply to listing form.

### 19.2 AI goi y listing cho customer

- Route: panel trong `/customers/:id` hoac `/ai/recommendations`.
- API:
  - `POST /api/v1/ai/customers/{customerId}/recommendations`
- Input: `maxResults`, `candidateLimit`, `naturalLanguageNeed`, `language`.
- Output: `PropertyRecommendationResponse` gom `customerId`, `fallbackUsed`, `aiStatus`, `recommendations[]`; moi item co `listing`, `matchScore`, `reason`, `suggestedAction`.
- UI: cards listing, match score, reason/action.

### 19.3 AI cham diem lead

- Route: panel trong `/leads/:id`.
- API:
  - `POST /api/v1/ai/leads/{leadId}/score`
- Output: `LeadScoreResponse` gom `leadId`, `leadCode`, `score`, `priority`, `reason`, `suggestedFollowUp`, `fallbackUsed`, `aiStatus`.
- UI: score gauge, priority badge, suggested follow-up CTA.

### 19.4 AI tom tat customer

- Route: panel trong `/customers/:id`.
- API:
  - `GET /api/v1/ai/customers/{customerId}/summary`
- Output: `CustomerSummaryResponse` gom `needsSummary`, `interactionSummary`, `interestedProperties`, `potentialLevel`, `nextBestAction`, AI metadata.
- UI: summary card, next action.

### 19.5 AI phan tich anh property

- Route: `/properties/:id/images` hoac `/ai/image-analysis`.
- API:
  - `POST /api/v1/ai/property-images/analyze`
- Input: `imageIds`.
- Output: `ImageAnalysisResponse` gom `images[]` voi `imageId`, `imageUrl`, `blurry`, `dark`, `duplicateSuspected`, `irrelevant`, `suggestedCover`, `caption`, `issues`, `recommendation`, AI metadata.
- UI: image quality badges, suggested cover, apply caption.

### 19.6 AI chat

- Route: `/ai/chat` cho back office, `/customer/ai-chat` cho customer.
- API:
  - `POST /api/v1/ai/chat/sessions`
  - `POST /api/v1/ai/chat/sessions/{sessionId}/messages`
  - `GET /api/v1/ai/chat/sessions/{sessionId}`
- Output: `ChatSessionResponse` gom `messages[]` va `suggestedListings[]`.
- UI: chat layout, suggested listing sidebar/card.

## 20. Master data dung chung

| Man hinh dung | Endpoint | Output |
| --- | --- | --- |
| Location selector | `GET /api/v1/master-data/provinces` | `LocationOptionResponse[]` |
| District selector | `GET /api/v1/master-data/provinces/{provinceId}/districts` | `LocationOptionResponse[]` |
| Ward selector | `GET /api/v1/master-data/districts/{districtId}/wards` | `LocationOptionResponse[]` |
| Property type selector | `GET /api/v1/master-data/property-types` | `PropertyTypeOptionResponse[]` |
| Amenity selector | `GET /api/v1/master-data/amenities` | `AmenityOptionResponse[]` |
| Listing package selector | `GET /api/v1/master-data/listing-packages` | `ListingPackageOptionResponse[]` |
| Lead source selector | `GET /api/v1/master-data/lead-sources` | `LeadSourceOptionResponse[]` |

## 21. Trang loi va trang he thong can thiet ke

| Route UI | Chuc nang | API lien quan |
| --- | --- | --- |
| `/403` | Khong co quyen | Render khi API tra `403` |
| `/404` | Khong tim thay | Render khi route/API tra `404` |
| `/session-expired` | Het phien dang nhap | Sau khi refresh token that bai |
| `/server-error` | Loi he thong | Render khi `5xx` |
| Empty states | Chua co du lieu | Moi list page |
| Loading/skeleton | Dang tai du lieu | Moi screen co API |

## 22. Workflow thiet ke quan trong

### Flow public den CRM

1. Guest search listing: `GET /api/v1/search/listings`.
2. Guest xem chi tiet: `GET /api/v1/search/listings/{slug}`.
3. Guest gui inquiry: `POST /api/v1/search/listings/{listingId}/inquiries`.
4. Backend tao/merge customer va tao lead.
5. Agent xem lead trong `/leads`.
6. Agent cham soc, tao lich hen, tao follow-up task.

### Flow property den listing public

1. Agent tao property: `POST /api/v1/properties`.
2. Upload anh: `POST /api/v1/properties/{id}/images`.
3. Doi status property: `PATCH /api/v1/properties/{id}/status`.
4. Tao listing: `POST /api/v1/listings`.
5. Submit review: `PATCH /api/v1/listings/{id}/submit`.
6. Manager approve: `PATCH /api/v1/listings/{id}/approve`.
7. Publish: `PATCH /api/v1/listings/{id}/publish`.
8. Listing xuat hien public search.

### Flow lead den giao dich

1. Agent tao/nhan lead.
2. Assign/status/notes/activities: `/api/v1/leads/{id}/...`.
3. Tao appointment: `POST /api/v1/appointments`.
4. Complete appointment va feedback.
5. Tao contract: `POST /api/v1/contracts`.
6. Upload document, submit review, approve, mark signed.
7. Tao transaction: `POST /api/v1/transactions`.
8. Ghi deposit/payment schedule/payment/invoice/receipt.
9. Complete transaction va xem commission/report.

## 23. Ghi chu khoang trong backend

- Backend hien chua co forgot password/reset password/email verification endpoint, khong thiet ke flow nay nhu chuc nang live tru khi du dinh lam them.
- Backend chua co owner portal rieng, du role `OWNER` ton tai.
- Notification chua co WebSocket, UI nen dung polling unread count.
- Public inquiry/appointment request co rate limit, UI can xu ly `429`.
- AI co fallback/noop; UI can hien badge `fallbackUsed` hoac `aiStatus`.
