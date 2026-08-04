# AI Context Binding Plan

## Mục tiêu

Ràng buộc toàn bộ AI trong hệ thống real estate vào dữ liệu thật của hệ thống, ताकि:

- Chat AI luôn trả lời dựa trên dữ liệu đang có trong DB.
- AI sinh listing description phải bám theo property/listing thực tế, không bịa thông tin.
- AI recommendation phải truy xuất danh sách sản phẩm thật để đề xuất cho buyer/customer.
- AI scoring, summary, image analysis phải dùng context nội bộ thay vì trả lời chung chung.

## Vấn đề hiện tại

AI đã gọi được provider, nhưng đang hoạt động như một LLM tổng quát:

- Người chat hỏi nhu cầu mua/thuê, AI chưa tìm sản phẩm phù hợp trong hệ thống.
- Endpoint sinh description chỉ dựa vào prompt, chưa kéo dữ liệu property/listing đầy đủ.
- Recommendation chưa có lớp retrieval đủ mạnh để tìm listing theo nhu cầu tự nhiên của người dùng.
- Một số luồng vẫn có fallback text nên user có cảm giác AI “không biết hệ thống đang có gì”.

## Nguyên tắc thiết kế

1. AI không được tự bịa dữ liệu hệ thống.
2. Mọi câu trả lời nghiệp vụ phải dựa trên context lấy từ DB hoặc service nội bộ.
3. Prompt chỉ là lớp diễn đạt cuối cùng, không phải nguồn sự thật.
4. Nếu context không đủ, AI phải hỏi lại hoặc trả lời rằng chưa tìm được sản phẩm phù hợp.
5. Có thể dùng LLM để diễn giải, nhưng tìm kiếm và lọc phải do backend quyết định.

## Kiến trúc đích

### 1. Context Builder Layer

Thêm một lớp chuyên tạo context cho từng loại AI request:

- `ChatContextBuilder`
- `ListingDescriptionContextBuilder`
- `PropertyRecommendationContextBuilder`
- `LeadScoreContextBuilder`
- `CustomerSummaryContextBuilder`
- `ImageAnalysisContextBuilder`

Mỗi builder sẽ:

- Nhận input từ request + actor.
- Query dữ liệu thật từ repository/service.
- Chuẩn hóa dữ liệu thành context JSON/text ngắn gọn.
- Trả về object context có cấu trúc rõ ràng để đưa vào prompt.

### 2. Retrieval Layer

Thêm lớp truy vấn theo ngữ nghĩa nghiệp vụ, không chỉ theo ID:

- Tìm listing theo khu vực, giá, số phòng ngủ, mục đích thuê/mua.
- Tìm property theo trạng thái, loại hình, pháp lý, tiện ích.
- Tìm customer/lead theo nhu cầu, lịch sử tương tác, pipeline.
- Tìm image metadata theo property image.

### 3. Prompt Contract Layer

Mỗi AI endpoint phải có prompt contract rõ:

- Input schema.
- Allowed context fields.
- Output schema bắt buộc.
- Rule không được bịa ra listing/property/customer không tồn tại trong context.

## Dữ liệu cần ràng buộc

### Cho chat AI

Chat phải được gắn với:

- User profile
- Customer profile nếu có
- Danh sách listing phù hợp
- Danh sách property phù hợp
- Lịch sử chat
- Lịch sử lead/customer nếu actor là agent/manager
- Khu vực, ngân sách, loại hình, số phòng ngủ, diện tích, pháp lý, trạng thái listing

### Cho listing description

Phải có:

- Property details
- Listing details
- Cấu trúc mặt bằng, vị trí, diện tích, pháp lý, tiện ích, nội thất
- Audience target
- Selling points thật từ dữ liệu hệ thống

### Cho recommendation

Phải có:

- Customer requirement
- Customer budget
- Desired purpose
- Area preference
- Bedroom count
- Property type
- Candidate listings thật từ repository
- Ranking score do backend tính

### Cho lead score

Phải có:

- Lead activities
- Follow-up tasks
- Appointments
- Reply history nếu có
- Score heuristic backend

### Cho image analysis

Phải có:

- Ảnh gốc
- Property metadata
- Image metadata
- Listing status
- Cover image status

## Cách triển khai theo endpoint

### A. Chat AI

Hiện tại chat đang chỉ gửi conversation history vào LLM. Cần thêm retrieval trước khi gọi AI:

1. Parse ý định người dùng từ câu chat.
2. Detect các slot:
   - thuê hoặc mua
   - khu vực
   - số phòng ngủ
   - ngân sách
   - loại sản phẩm
   - diện tích
3. Query listing/property thật trong DB.
4. Build top-N candidates.
5. Đưa candidate summary vào prompt.
6. Bắt LLM chỉ được:
   - trả lời dựa trên candidate list
   - hỏi thêm nếu thiếu thông tin
   - không được bịa listing ngoài context

### B. Listing description AI

Thay vì prompt chỉ mô tả chung, cần:

1. Load property/listing đầy đủ.
2. Chuẩn hóa metadata thành `structured context`.
3. Chỉ cho phép LLM viết lại thành mô tả marketing từ dữ liệu có sẵn.
4. Cấm thêm thông tin không tồn tại trong source context.

### C. Recommendation AI

Đây là chỗ quan trọng nhất vì user đang cần đề xuất sản phẩm thật.

1. Lấy nhu cầu từ customer hoặc message chat.
2. Chuyển nhu cầu thành query domain.
3. Search listing phù hợp bằng repository/specification.
4. Rank bằng backend scorer.
5. Đưa top results vào prompt để LLM giải thích lý do đề xuất.
6. Nếu không có sản phẩm phù hợp, LLM phải trả lời theo template fallback:
   - chưa tìm thấy sản phẩm phù hợp
   - đề xuất nới tiêu chí
   - gợi ý tạo alert hoặc mở rộng khu vực/ngân sách

### D. Customer summary AI

1. Load customer, notes, requirements, favorites, interactions.
2. Tạo context timeline ngắn.
3. Bắt LLM trả summary theo format cố định.
4. Không cho summary suy đoán ngoài dữ liệu.

## Cấu trúc context đề xuất

Mỗi request nên có payload kiểu:

```json
{
  "actor": {
    "id": 1,
    "role": "AGENT"
  },
  "intent": "find_rental_listing",
  "constraints": {
    "purpose": "RENT",
    "bedrooms": 2,
    "budgetMin": 12000000,
    "budgetMax": 18000000,
    "district": "District 7"
  },
  "candidates": [
    {
      "listingId": 101,
      "code": "LST-101",
      "title": "2-bedroom apartment near Phu My Hung",
      "price": 15000000,
      "bedrooms": 2,
      "area": 68,
      "location": "District 7",
      "status": "ACTIVE"
    }
  ],
  "systemRules": [
    "Only use candidates from context",
    "Do not invent listings",
    "Ask follow-up questions if constraints are insufficient"
  ]
}
```

## Prompt rules

Prompt phải ép model theo các luật sau:

- Nếu có candidate list thì chỉ được chọn từ list đó.
- Nếu không có candidate đủ điều kiện thì nói rõ không tìm thấy.
- Không được tạo ra address, price, bedroom count, legal status nếu không có trong context.
- Nếu nhu cầu chưa đủ rõ, hỏi tối đa 3 câu ngắn để làm rõ.

## Backend changes cần làm

### 1. Query APIs

Thêm hoặc mở rộng query cho:

- Listing by purpose, district, ward, price range, bedroom count, area
- Property by status, purpose, type, legal status
- Customer requirement by budget, area, keywords
- Lead by score inputs

### 2. Specifications / Search Services

Nên dùng lại pattern hiện có:

- `ListingSpecifications`
- `PropertySpecifications`
- `CustomerSpecifications`
- `LeadSpecifications`

Nếu thiếu field, mở rộng specification thay vì lọc thủ công trong service.

### 3. AI Context Services

Mỗi AI service nên gọi một context service trước khi gọi `AiService.complete(...)`.

### 4. Structured output validation

Sau khi LLM trả về:

- Validate JSON schema
- Validate references là candidates thật
- Nếu response không hợp lệ, fallback sang backend-generated response

## Ưu tiên triển khai

### Phase 1

- Thêm `ChatContextBuilder`
- Thêm candidate retrieval cho chat
- Bắt chat AI chỉ trả lời dựa trên candidate list

### Phase 2

- Ràng buộc `ListingDescriptionService`
- Dùng property/listing metadata thật
- Cấm hallucination thông tin listing

### Phase 3

- Ràng buộc `PropertyRecommendationService`
- Query top-N sản phẩm thật từ DB
- Cho AI explain recommendation thay vì tự nghĩ ra sản phẩm

### Phase 4

- Chuẩn hóa context cho lead score, customer summary, image analysis
- Bổ sung validation tầng response

### Phase 5

- Thêm tests cho prompt/context contract
- Thêm integration tests cho retrieval + AI

## Test cần có

1. Chat AI khi user hỏi “muốn thuê 2 phòng ngủ” phải trả về listing thật trong DB.
2. Chat AI khi không có listing phù hợp phải nói không tìm thấy, không bịa.
3. Listing description AI chỉ dùng dữ liệu property/listing thật.
4. Recommendation AI chỉ đề xuất listing nằm trong candidate list.
5. Response JSON sai schema phải fallback an toàn.

## Acceptance criteria

- User chat về nhu cầu thuê/mua sẽ nhận câu trả lời có listing thực tế từ hệ thống.
- AI description không thêm chi tiết ngoài dữ liệu property/listing.
- Recommendation có thể map trực tiếp tới listingId thật.
- Không còn tình huống AI trả lời “chung chung” khi DB có dữ liệu phù hợp.

## Ghi chú kỹ thuật

- Không nên nhét toàn bộ DB context vào prompt.
- Ưu tiên retrieval top-N + ranking trước, LLM chỉ làm phần diễn giải.
- Prompt phải nhỏ, có cấu trúc và có guardrails rõ ràng.
- Nếu cần, tạo một lớp `AiContextAssembler` dùng chung để chuẩn hóa payload.

## Yêu cầu thêm

- chỉ là owner của session AI đó mới được mở lại session đó, mỗi tài khoản đều có thể xem lại danh sách những sesion AI mà bản thân đã toạ trước đó
- ở những API mà khi hỏi đáp với user, nếu khi đề xuất kết quả thì phải trả về sao cho sau này frontend có thể nhấp vào kết quả đó để đi đến trang sản phẩm đó
