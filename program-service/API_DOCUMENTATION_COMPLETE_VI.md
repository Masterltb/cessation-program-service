# Tài liệu API - Program Service

**Phiên bản:** 2.2.0
**Ngày cập nhật:** 2023-12-08
**Người cập nhật:** AI Assistant

---

## 1. Tổng quan & Nguyên tắc thiết kế

Tài liệu này mô tả các API do `program-service` cung cấp. Service này chịu trách nhiệm quản lý toàn bộ vòng đời chương trình cai thuốc của người dùng, từ đánh giá ban đầu, thực thi lộ trình, cho đến quản lý dữ liệu và nội dung.

### 1.1. Môi trường

-   **Production Base URL:** `https://api.smokefree.app/program`
-   **Development Base URL:** `http://localhost:8080`

### 1.2. Cấu trúc & Tiền tố API (API Structure & Prefixes)

Các API được tổ chức theo chức năng và đối tượng sử dụng thông qua các tiền tố URL:

-   `/v1/...`: Các API cốt lõi, ổn định, tuân thủ versioning.
    -   `/v1/programs`: API liên quan đến chương trình của người dùng.
    -   `/v1/me/...`: API dành riêng cho người dùng đã xác thực để truy cập dữ liệu của chính họ (ví dụ: quiz).
    -   `/v1/admin/...`: API dành riêng cho quản trị viên.
-   `/api/...`: Các API phụ trợ hoặc có thể thay đổi thường xuyên hơn.
    -   `/api/onboarding`: Luồng giới thiệu và đánh giá ban đầu.
    -   `/api/plan-templates`: API quản lý các lộ trình mẫu.
    -   `/api/modules`: API quản lý các module nội dung.

### 1.3. Xác thực & Phân quyền (Authentication & Authorization)

Hệ thống áp dụng mô hình **Xác thực Ủy quyền (Delegated Authentication)**. `program-service` tin tưởng vào thông tin do API Gateway cung cấp qua các HTTP Header.

**HTTP Headers:**

| Header          | Bắt buộc | Mô tả                                                                     | Ví dụ                |
| :-------------- | :------- | :------------------------------------------------------------------------ | :------------------- |
| `X-User-Id`     | **Có**   | UUID của người dùng đã được xác thực.                                     | `uuid-user-123`      |
| `X-User-Role`   | **Có**   | Vai trò của người dùng.                                                    | `CUSTOMER`, `ADMIN`  |
| `X-User-Tier`   | Không    | Gói dịch vụ người dùng đang sử dụng. Dùng để xác định quyền lợi (features). | `BASIC`, `PREMIUM`   |

**Cơ chế phân quyền:**

1.  **Kiểm tra Vai trò (Role-based):** Sử dụng `@PreAuthorize` ở tầng Controller để kiểm tra vai trò (`hasRole('ADMIN')`).
2.  **Kiểm tra Quyền sở hữu (Ownership-based):** Tầng Service luôn xác minh `userId` để đảm bảo người dùng chỉ có thể thao tác trên dữ liệu của chính mình.

### 1.4. Mã trạng thái HTTP (Common Status Codes)

| Code | Ý nghĩa                  | Mô tả                                                              |
| :--- | :----------------------- | :----------------------------------------------------------------- |
| `200`  | OK                       | Yêu cầu thành công.                                                |
| `201`  | Created                  | Tài nguyên được tạo thành công.                                    |
| `400`  | Bad Request              | Dữ liệu đầu vào không hợp lệ (sai định dạng, thiếu trường).        |
| `401`  | Unauthorized             | Thiếu thông tin xác thực (ví dụ: thiếu header `X-User-Id`).        |
| `402`  | Payment Required         | Gói dùng thử đã hết hạn, yêu cầu nâng cấp.                         |
| `403`  | Forbidden                | Đã xác thực nhưng không có quyền truy cập tài nguyên.               |
| `404`  | Not Found                | Không tìm thấy tài nguyên được yêu cầu.                            |
| `409`  | Conflict                 | Xung đột trạng thái (ví dụ: tạo chương trình khi đã có một chương trình đang hoạt động). |

---

## 2. API dành cho Người dùng (User-Facing APIs)

### 2.1. Onboarding - Đánh giá & Đăng ký

#### `POST /api/onboarding/baseline`
Đánh giá mức độ nghiện ban đầu và gợi ý lộ trình phù hợp.
-   **Phân quyền:** `CUSTOMER`
-   **Request Body:** `QuizAnswerReq`
-   **Response (200 OK):** `BaselineResultRes` (Chi tiết các lộ trình được gợi ý).

### 2.2. Programs - Quản lý chương trình

#### `POST /v1/programs`
Tạo một chương trình trả phí mới.
-   **Phân quyền:** `CUSTOMER`
-   **Request Body:** `CreateProgramReq`
    | Tên | Kiểu | Bắt buộc | Mô tả |
    | :--- | :--- | :--- | :--- |
    | `planDays` | `Integer` | Có | Số ngày của lộ trình (ví dụ: 30, 45, 60). |
-   **Response (201 Created):** `ProgramRes`

#### `GET /v1/programs/active`
Lấy chương trình đang hoạt động.
-   **Phân quyền:** `CUSTOMER`
-   **Response (200 OK):** `ProgramRes`
-   **Response (404 Not Found):** Nếu không có chương trình nào đang hoạt động.

#### `GET /v1/programs`
Liệt kê tất cả các chương trình của người dùng.
-   **Phân quyền:** `CUSTOMER`
-   **Response (200 OK):** `List<ProgramRes>`

#### `POST /api/programs/{programId}/pause`
Tạm dừng chương trình đang hoạt động.
-   **Phân quyền:** `CUSTOMER` (Chủ sở hữu)
-   **Response (200 OK):** `ProgramRes` với `status: PAUSED`.

#### `POST /api/programs/{programId}/resume`
Tiếp tục chương trình đã tạm dừng.
-   **Phân quyền:** `CUSTOMER` (Chủ sở hữu)
-   **Response (200 OK):** `ProgramRes` với `status: ACTIVE`.

### 2.3. Program Execution - Thực thi chương trình

#### `GET /api/programs/{programId}/steps/today`
Lấy danh sách nhiệm vụ của ngày hôm nay (UTC).
-   **Phân quyền:** `CUSTOMER`, `COACH`
-   **Response (200 OK):** `List<StepAssignment>`

#### `PATCH /api/programs/{programId}/steps/{stepId}/status`
Cập nhật trạng thái một nhiệm vụ.
-   **Phân quyền:** `CUSTOMER`
-   **Request Body:** `{ "status": "COMPLETED" | "SKIPPED" }`
-   **Response (200 OK):** `StepAssignment`

#### `POST /api/programs/{programId}/smoke-events`
Ghi lại một sự kiện hút thuốc.
-   **Phân quyền:** `CUSTOMER`
-   **Request Body:** `CreateSmokeEventReq`
-   **Logic:** Sự kiện `SLIP` hoặc `RELAPSE` sẽ ngắt chuỗi ngày không hút thuốc (`streak`).
-   **Response (201 Created):** `SmokeEventRes`

#### `GET /api/programs/{programId}/streak`
Lấy thông tin chuỗi ngày không hút thuốc.
-   **Phân quyền:** `CUSTOMER`, `COACH`
-   **Response (200 OK):** `StreakView`

### 2.4. Quizzes - Làm bài đánh giá

#### `GET /v1/me/quizzes`
Lấy danh sách các bài quiz đến hạn.
-   **Phân quyền:** `CUSTOMER` (Xác thực qua `X-User-Id`)
-   **Response (200 OK):** `List<DueItem>`

#### `POST /v1/me/quizzes/{templateId}/open`
Bắt đầu một lượt làm bài quiz.
-   **Phân quyền:** `CUSTOMER`
-   **Response (201 Created):** `OpenAttemptRes` (Chứa câu hỏi và các lựa chọn).

#### `POST /v1/me/quizzes/{attemptId}/submit`
Nộp bài và nhận kết quả.
-   **Phân quyền:** `CUSTOMER`
-   **Response (200 OK):** `SubmitRes` (Chứa điểm số và mức độ đánh giá).

---

## 3. API dành cho Quản trị viên (Admin-Facing APIs)

### 3.1. Program Management - Quản lý chương trình người dùng

#### `POST /api/programs/{programId}/upgrade-from-trial`
Nâng cấp chương trình từ dùng thử sang trả phí.
-   **Phân quyền:** `ADMIN`, `PAYMENT_SERVICE`
-   **Logic:** Xóa các trường `trial...` khỏi `Program`.
-   **Response (200 OK):** `ProgramRes`

#### `PATCH /api/programs/{programId}/current-day`
Cập nhật ngày hiện tại của chương trình cho người dùng.
-   **Phân quyền:** `ADMIN`
-   **Request Body:** `{ "currentDay": 5 }`
-   **Response (200 OK):** `ProgramRes`

### 3.2. Plan Template Management - Quản lý lộ trình mẫu

#### `GET /api/plan-templates`
Lấy danh sách tóm tắt các lộ trình mẫu.
-   **Phân quyền:** `isAuthenticated()` (Bất kỳ vai trò nào)
-   **Response (200 OK):** `List<PlanTemplateSummaryRes>`

#### `GET /api/plan-templates/{id}`
Lấy chi tiết một lộ trình mẫu, bao gồm các `PlanStep`.
-   **Phân quyền:** `isAuthenticated()`
-   **Response (200 OK):** `PlanTemplateDetailRes`

### 3.3. Content Module Management - Quản lý nội dung

#### `POST /api/modules`
Tạo một module nội dung mới (bài viết, video, audio).
-   **Phân quyền:** `ADMIN`
-   **Request Body:** `ContentModuleCreateReq`
-   **Response (201 Created):** `ContentModuleRes`

#### `PUT /api/modules/{id}`
Cập nhật một module nội dung.
-   **Phân quyền:** `ADMIN`
-   **Request Body:** `ContentModuleUpdateReq`
-   **Response (200 OK):** `ContentModuleRes`

### 3.4. Quiz Management - Quản lý Quiz

#### `POST /v1/admin/quizzes`
Tạo một bộ quiz hoàn chỉnh (template, câu hỏi, lựa chọn).
-   **Phân quyền:** `ADMIN`
-   **Request Body:** `CreateFullQuizReq`
-   **Response (201 Created):** `{ "id": "...", "message": "..." }`

#### `POST /v1/admin/quizzes/{templateId}/publish`
Công khai một quiz template từ `DRAFT` sang `PUBLISHED`.
-   **Phân quyền:** `ADMIN`
-   **Response (200 OK):** `{ "message": "Quiz published" }`

---

## 4. Ghi chú cho nhà phát triển (Developer Notes)

### 4.1. Luồng xác thực chi tiết

-   **`HeaderUserContextFilter`**: Filter này chạy trên môi trường production. Nó đọc các header `X-User-Id`, `X-User-Role`, `X-User-Tier` và tạo `Authentication` object cho Spring Security.
-   **`DevAutoUserFilter`**: Filter này chỉ chạy với profile `dev`. Nó cho phép giả lập người dùng bằng cách đọc các header đặc biệt (`X-Claims`, `X-User-Roles`) để đơn giản hóa việc kiểm thử API mà không cần API Gateway.

### 4.2. Cấu hình OAuth2/JWT

-   Hệ thống có thể được cấu hình để hoạt động như một OAuth2 Resource Server. Nếu các thuộc tính `spring.security.oauth2.resourceserver.jwt.*` được cung cấp, Spring Security sẽ tự động kích hoạt cơ chế xác thực bằng JWT Bearer Token. Khi đó, `HeaderUserContextFilter` sẽ được bỏ qua nếu token hợp lệ.

### 4.3. Đối tượng dữ liệu chính (Core DTOs)

#### `ProgramRes`
Đại diện cho một chương trình của người dùng.

| Tên | Kiểu | Mô tả |
| :--- | :--- | :--- |
| `id` | `UUID` | ID của chương trình. |
| `status` | `String` | Trạng thái: `ACTIVE`, `PAUSED`, `COMPLETED`. |
| `planDays` | `Integer` | Tổng số ngày của lộ trình. |
| `startDate` | `String` | Ngày bắt đầu (YYYY-MM-DD). |
| `currentDay` | `Integer` | Ngày hiện tại trong lộ trình. |
| `entitlements` | `Object` | Các quyền lợi dựa trên gói dịch vụ. |
| `access` | `Object` | Thông tin về trạng thái truy cập (dùng thử, trả phí). |
```