# 📘 TÀI LIỆU KỸ THUẬT: PROGRAM SERVICE API REFERENCE

**Phiên bản:** 7.0 (Final Verified & Polished Edition)
**Ngày cập nhật:** 04/12/2025
**Trạng thái:** Stable
**Base URL:**
*   Dev: `http://localhost:8080`
*   Prod: `https://api.smokefree.app/program`

---

## 📑 MỤC LỤC

1.  [Quy chuẩn chung (Conventions)](#1-quy-chuẩn-chung)
2.  [Luồng 1: Onboarding & Khám phá](#2-luồng-1-onboarding--khám-phá)
3.  [Luồng 2: Enrollment & Khởi tạo](#3-luồng-2-enrollment--khởi-tạo)
4.  [Luồng 3: Dashboard & Trạng thái chung](#4-luồng-3-dashboard--trạng-thái-chung)
5.  [Luồng 4: Thực thi lộ trình hằng ngày](#5-luồng-4-thực-thi-lộ-trình-hằng-ngày)
6.  [Luồng 5: Hệ thống bài kiểm tra (Quiz Engine)](#6-luồng-5-hệ-thống-bài-kiểm-tra-quiz-engine)
7.  [Luồng 6: Theo dõi & Phân tích (Tracking & Analytics)](#7-luồng-6-theo-dõi--phân-tích-tracking--analytics)
8.  [Luồng 7: Quản lý Gói cước (Subscription)](#8-luồng-7-quản-lý-gói-cước-subscription)
9.  [Luồng 8: Quản lý & Cài đặt Program](#9-luồng-8-quản-lý--cài-đặt-program)
10. [Luồng 9: Admin Portal (CMS)](#10-luồng-9-admin-portal-cms)
11. [Luồng 10: Debug & Development Tools](#11-luồng-10-debug--development-tools)
12. [Mã lỗi & Xử lý](#12-mã-lỗi--xử-lý)

---

## 1. QUY CHUẨN CHUNG

### 1.1. Authentication & Headers
Hầu hết các API yêu cầu xác thực qua JWT. Client cần gửi kèm các headers sau:

| Header Key | Bắt buộc | Kiểu | Mô tả | Ví dụ |
| :--- | :---: | :--- | :--- | :--- |
| `Authorization` | ✅ | String | Bearer Token từ Auth Service | `Bearer eyJhbGci...` |
| `X-User-Id` | ✅ | UUID | ID của người dùng hiện tại | `550e8400-e29b...` |
| `X-User-Group` | ❌ | String | Nhóm người dùng (`CUSTOMER`, `COACH`, `ADMIN`) | `CUSTOMER` |
| `X-User-Tier` | ❌ | String | Hạng thành viên (`BASIC`, `PREMIUM`, `VIP`) | `PREMIUM` |
| `Accept-Language` | ❌ | String | Ngôn ngữ nội dung (`vi`, `en`) | `vi` |

### 1.2. Định dạng dữ liệu
*   **Date:** `YYYY-MM-DD` (Ví dụ: `2025-12-04`)
*   **DateTime:** ISO-8601 UTC (Ví dụ: `2025-12-04T15:30:00Z`)
*   **UUID:** Chuỗi 36 ký tự chuẩn (Ví dụ: `123e4567-e89b-12d3-a456-426614174000`)

---

## 2. LUỒNG 1: ONBOARDING & KHÁM PHÁ

Luồng này dành cho người dùng mới, từ lúc mở app lần đầu đến khi sẵn sàng đăng ký một lộ trình.

### 2.1. Lấy bộ câu hỏi đánh giá (Baseline Quiz)
> **Mục đích:** Lấy cấu trúc và nội dung bài quiz đánh giá ban đầu để hiển thị cho người dùng.

*   **Method:** `GET`
*   **URL:** `/api/onboarding/baseline/quiz`
*   **Auth:** Public (hoặc Authenticated User chưa có Program)

**Response (200 OK):**
```json
{
  "attemptId": null, // Luôn null ở bước này vì chưa tạo attempt thật
  "templateId": "c56a4180-65aa-42ec-a945-5fd21dec0538",
  "version": 1,
  "questions": [
    {
      "questionNo": 1,
      "questionText": "Bạn thường hút điếu thuốc đầu tiên trong ngày khi nào?",
      "choices": {
        "A": "Trong vòng 5 phút sau khi thức dậy",
        "B": "Từ 6 đến 30 phút",
        "C": "Sau 60 phút"
      }
    }
  ]
}
```

### 2.2. Nộp kết quả & Nhận gợi ý lộ trình
> **Mục đích:** Gửi câu trả lời của người dùng lên server để tính điểm và nhận về gợi ý lộ trình phù hợp.

*   **Method:** `POST`
*   **URL:** `/api/onboarding/baseline`
*   **Auth:** Authenticated User

**Request Body:**
| Field | Type | Required | Description |
| :--- | :--- | :---: | :--- |
| `templateId` | UUID | ✅ | ID của template quiz (lấy từ API 2.1) |
| `answers` | Array | ✅ | Danh sách câu trả lời |
| `answers[].questionNo` | Integer | ✅ | Số thứ tự câu hỏi |
| `answers[].answer` | String | ✅ | Mã đáp án đã chọn (A, B, C...) |

**Example Request:**
```json
{
  "templateId": "c56a4180-65aa-42ec-a945-5fd21dec0538",
  "answers": [
    { "questionNo": 1, "answer": "A" },
    { "questionNo": 2, "answer": "C" }
  ]
}
```

**Response (200 OK):**
```json
{
  "userId": "550e8400-e29b...",
  "baselineScore": 7,
  "addictionLevel": "HIGH", // LOW, MODERATE, HIGH
  "recommendedTemplateId": "plan-uuid-123", // QUAN TRỌNG: Dùng ID này để gọi API tạo Program
  "recommendationReason": "Bạn có mức độ phụ thuộc vật lý cao. Lộ trình này tập trung vào..."
}
```

### 2.3. Khám phá các gói lộ trình (Plan Templates)
> **Mục đích:** Lấy danh sách tóm tắt tất cả các gói lộ trình có sẵn để hiển thị màn hình "Chọn lộ trình".

*   **Method:** `GET`
*   **URL:** `/api/plan-templates`
*   **Auth:** Public / Authenticated

**Response (200 OK):**
```json
[
  {
    "id": "plan-uuid-1",
    "code": "21_DAY_COLD_TURKEY",
    "name": "Lộ trình 21 ngày dứt điểm",
    "totalDays": 21,
    "shortDescription": "Dành cho người có ý chí cao, muốn bỏ ngay lập tức.",
    "thumbnailUrl": "https://..."
  },
  {
    "id": "plan-uuid-2",
    "code": "30_DAY_GRADUAL",
    "name": "Lộ trình 30 ngày giảm dần",
    "totalDays": 30,
    "shortDescription": "Phù hợp cho người muốn giảm từ từ.",
    "thumbnailUrl": "https://..."
  }
]
```

### 2.4. Xem chi tiết một gói lộ trình
> **Mục đích:** Xem chi tiết cấu trúc (các ngày, các bài học) của một gói lộ trình cụ thể.

*   **Method:** `GET`
*   **URL:** `/api/plan-templates/{id}`
*   **Path Params:** `id` (UUID) - ID của gói lộ trình.

**Response (200 OK):**
```json
{
  "id": "plan-uuid-1",
  "name": "Lộ trình 21 ngày dứt điểm",
  "totalDays": 21,
  "longDescription": "Chi tiết về lộ trình...",
  "days": [
    {
      "dayNo": 1,
      "title": "Ngày đầu tiên quan trọng",
      "steps": [
        { "slot": 1, "title": "Đọc bài: Tác hại của thuốc lá", "moduleCode": "EDU_HARMS_01" },
        { "slot": 2, "title": "Bài tập hít thở", "moduleCode": "EXERCISE_BREATH_01" }
      ]
    }
  ]
}
```

---

## 3. LUỒNG 2: ENROLLMENT & KHỞI TẠO

### 3.1. Đăng ký tham gia chương trình (Create Program)
> **Mục đích:** Người dùng chính thức bắt đầu một lộ trình cai thuốc.

*   **Method:** `POST`
*   **URL:** `/v1/programs`
*   **Auth:** Authenticated User

**Request Body:**
| Field | Type | Required | Description |
| :--- | :--- | :---: | :--- |
| `planTemplateId` | UUID | ✅ | ID của gói lộ trình muốn tham gia |
| `trial` | Boolean | ✅ | `true` nếu muốn dùng thử, `false` nếu trả phí ngay |
| `coachId` | UUID | ❌ | ID của huấn luyện viên (nếu có) |

**Example Request:**
```json
{
  "planTemplateId": "plan-uuid-123",
  "trial": true,
  "coachId": null
}
```

**Business Logic:**
*   Hệ thống sẽ tạo bản ghi `Program` mới với trạng thái `ACTIVE`.
*   Toàn bộ bài tập (`StepAssignment`) sẽ được copy từ Template sang Program của user.
*   Phiên bản nội dung (`moduleVersion`) sẽ được "đóng băng" tại thời điểm tạo.

### 3.2. Lấy lịch sử tham gia
> **Mục đích:** Xem danh sách tất cả các chương trình user đã từng tham gia (Active, Completed, Cancelled).

*   **Method:** `GET`
*   **URL:** `/v1/programs`
*   **Auth:** Authenticated User

---

## 4. LUỒNG 3: DASHBOARD & TRẠNG THÁI CHUNG

### 4.1. Lấy dữ liệu tổng hợp (Dashboard Aggregation)
> **Mục đích:** API "All-in-One" cho màn hình Home. Cung cấp mọi thông tin cần thiết để render dashboard.

*   **Method:** `GET`
*   **URL:** `/api/me`
*   **Auth:** Authenticated User

**Response (200 OK):**
```json
{
  "userId": "user-uuid",
  "subscription": {
    "tier": "BASIC",        // BASIC, PREMIUM, VIP
    "status": "ACTIVE",     // ACTIVE, EXPIRED, CANCELLED
    "expiresAt": null
  },
  "activeProgram": {
    "id": "program-uuid",
    "templateName": "Lộ trình 21 ngày",
    "status": "ACTIVE",     // ACTIVE, PAUSED, COMPLETED
    "currentDay": 3,        // Ngày hiện tại trong lộ trình (1-based index)
    "planDays": 21,         // Tổng số ngày của lộ trình
    "isTrial": true,
    "trialRemainingDays": 4
  },
  "dueQuizzes": [           // Danh sách bài kiểm tra đến hạn
    {
      "templateId": "quiz-uuid",
      "templateName": "Check-in Tuần 1",
      "isOverdue": false
    }
  ],
  "streakInfo": {
    "currentStreak": 3,     // Số ngày liên tiếp hoàn thành nhiệm vụ
    "daysWithoutSmoke": 3   // Số ngày không hút thuốc
  }
}
```

### 4.2. Lấy chi tiết tiến độ (Progress Detail)
> **Mục đích:** Cung cấp số liệu chi tiết để vẽ biểu đồ tiến độ hoặc màn hình "Hành trình của tôi".

*   **Method:** `GET`
*   **URL:** `/api/programs/{id}/progress`
*   **Path Params:** `id` (UUID) - ID của Program.

**Response (200 OK):**
```json
{
  "programId": "program-uuid",
  "status": "ACTIVE",
  "currentDay": 5,
  "planDays": 30,
  "percentComplete": 16.66, // (currentDay / planDays) * 100
  "daysRemaining": 25,
  "stepsCompleted": 12,     // Tổng số task đã hoàn thành
  "stepsTotal": 90,         // Tổng số task của cả lộ trình
  "streakCurrent": 5,
  "trialRemainingDays": 2
}
```

### 4.3. Lấy danh sách huy hiệu (My Badges)
> **Mục đích:** Lấy danh sách các huy hiệu người dùng **đã đạt được**.

*   **Method:** `GET`
*   **URL:** `/api/me/badges`

**Response (200 OK):**
```json
[
  {
    "code": "PROG_LV1",
    "category": "PROGRAM",
    "level": 1,
    "name": "Khởi hành",
    "description": "Bắt đầu chương trình đầu tiên của bạn.",
    "iconUrl": "https://...",
    "earnedAt": "2025-12-01T10:00:00Z"
  }
]
```

### 4.4. Lấy toàn bộ định nghĩa huy hiệu (All Badges)
> **Mục đích:** Lấy danh sách TẤT CẢ huy hiệu có trong hệ thống, dùng để hiển thị danh sách đầy đủ (bao gồm cả huy hiệu chưa đạt được - hiển thị mờ).

*   **Method:** `GET`
*   **URL:** `/api/me/badges/all`

**Response (200 OK):**
```json
{
  "PROGRAM": [ // Group theo Category
    { "code": "PROG_LV1", "name": "Khởi hành", "iconUrl": "...", "description": "..." },
    { "code": "PROG_LV2", "name": "Kiên trì", "iconUrl": "...", "description": "..." }
  ],
  "STREAK": [
    { "code": "STREAK_LV1", "name": "Chuỗi 3 ngày", "iconUrl": "...", "description": "..." }
  ]
}
```

---

## 5. LUỒNG 4: THỰC THI LỘ TRÌNH HẰNG NGÀY

### 5.1. Lấy danh sách nhiệm vụ hôm nay
> **Mục đích:** Lấy các bài tập cần làm trong ngày hiện tại (dựa trên `currentDay` của Program).

*   **Method:** `GET`
*   **URL:** `/api/programs/{programId}/steps/today`

**Response (200 OK):**
```json
[
  {
    "id": "step-assign-uuid",
    "stepNo": 1,
    "titleOverride": "Đọc bài: Tại sao bạn nghiện?",
    "status": "PENDING",       // PENDING, COMPLETED, SKIPPED
    "moduleCode": "EDU_NICOTINE_01",
    "moduleVersion": "1",      // Phiên bản nội dung được đóng băng lúc tạo program
    "scheduledAt": "2025-12-04T00:00:00Z"
  }
]
```

### 5.2. Lấy nội dung bài học (Content Module)
> **Mục đích:** Lấy nội dung chi tiết (HTML, Video URL, JSON payload) của một bài học để hiển thị.

*   **Method:** `GET`
*   **URL:** `/api/modules/by-code/{code}`
*   **Query Params:** `lang` (default: `vi`)

**Response (200 OK):**
```json
{
  "id": "module-uuid",
  "code": "EDU_NICOTINE_01",
  "type": "ARTICLE", // ARTICLE, VIDEO, AUDIO, QUIZ
  "version": 1,
  "payload": {
    "title": "Cơ chế của Nicotine",
    "content": "<p>Nicotine tác động lên não bộ...</p>",
    "videoUrl": "https://..."
  }
}
```

### 5.3. Cập nhật trạng thái nhiệm vụ
> **Mục đích:** Đánh dấu một bài tập là đã hoàn thành hoặc bỏ qua.

*   **Method:** `PUT`
*   **URL:** `/api/programs/{programId}/steps/{stepId}/status`

**Request Body:**
```json
{
  "status": "COMPLETED", // Giá trị hợp lệ: COMPLETED, SKIPPED
  "note": "Cảm thấy khá dễ dàng" // (Optional)
}
```

**Business Logic:**
*   Khi tất cả task trong ngày hoàn thành -> Hệ thống tự động tăng `Streak`.
*   Nếu là ngày cuối cùng của lộ trình -> Program chuyển trạng thái `COMPLETED`.

---

## 6. LUỒNG 5: HỆ THỐNG BÀI KIỂM TRA (QUIZ ENGINE)

### 6.1. Lấy danh sách Quiz cần làm
*   **Method:** `GET`
*   **URL:** `/v1/me/quizzes`
*   **Response (200 OK):**
    ```json
    {
      "success": true,
      "data": [
        {
          "templateId": "quiz-uuid-1",
          "templateName": "Check-in Tuần 1",
          "isDue": true,
          "isOverdue": false
        }
      ],
      "count": 1
    }
    ```

### 6.2. Mở đề (Start Quiz)
*   **Method:** `POST`
*   **URL:** `/v1/me/quizzes/{templateId}/open`
*   **Response (201 Created):**
    ```json
    {
      "success": true,
      "data": {
        "attemptId": "attempt-uuid",
        "questions": [ ... ]
      }
    }
    ```

### 6.3. Lưu câu trả lời (Save Draft)
> **Mục đích:** Lưu câu trả lời cho từng câu hỏi (gọi mỗi khi user chọn đáp án).

*   **Method:** `PUT`
*   **URL:** `/v1/me/quizzes/{attemptId}/answer`
*   **Request Body:** `{ "questionNo": 1, "answer": "A" }`
*   **Response (200 OK):**
    ```json
    {
      "success": true,
      "message": "Answer saved successfully"
    }
    ```

### 6.4. Nộp bài (Submit)
> **Mục đích:** Kết thúc bài kiểm tra và nhận kết quả.

*   **Method:** `POST`
*   **URL:** `/v1/me/quizzes/{attemptId}/submit`
*   **Response (200 OK):**
    ```json
    {
      "success": true,
      "data": {
        "attemptId": "attempt-uuid",
        "totalScore": 15,
        "severity": "MODERATE",
        "feedback": "Bạn đã làm rất tốt! Hãy tiếp tục phát huy.",
        "earnedBadges": [
          { "code": "QUIZ_MASTER_1", "name": "Chuyên gia Quiz" }
        ]
      }
    }
    ```

---

## 7. LUỒNG 6: THEO DÕI & PHÂN TÍCH (TRACKING & ANALYTICS)

### 7.1. Báo cáo sự cố (Smoke Event)
> **Mục đích:** Người dùng báo cáo khi họ lỡ hút thuốc (Slip) hoặc có cơn thèm thuốc (Urge).

*   **Method:** `POST`
*   **URL:** `/api/programs/{programId}/smoke-events`

**Request Body:**
```json
{
  "eventType": "SMOKE", // SMOKE (Hút thật) | URGE (Chỉ thèm)
  "kind": "SLIP",       // SLIP (Lỡ 1 điếu) | LAPSE (Hút vài điếu) | RELAPSE (Tái nghiện)
  "puffs": 5,           // Số hơi hút (ước lượng)
  "reason": "STRESS",   // Lý do: STRESS, BOREDOM, SOCIAL, HABIT...
  "eventAt": "2025-12-04T10:30:00Z"
}
```

---

## 8. LUỒNG 7: QUẢN LÝ GÓI CƯỚC (SUBSCRIPTION)

### 8.1. Lấy trạng thái gói cước
*   **Method:** `GET`
*   **URL:** `/api/subscriptions/me`

### 8.2. Nâng cấp gói cước (Mock Payment)
> **Mục đích:** Mô phỏng việc nâng cấp gói cước. API này có thể được gọi từ màn hình "Nâng cấp tài khoản" chung, không phụ thuộc vào một program cụ thể.

*   **Method:** `POST`
*   **URL:** `/api/subscriptions/upgrade`
*   **Request Body:**
    ```json
    {
      "targetTier": "PREMIUM",
      "paymentProvider": "MOCK",
      "transactionId": "mock-tx-123"
    }
    ```

---

## 9. LUỒNG 8: QUẢN LÝ & CÀI ĐẶT PROGRAM

### 9.1. Các hành động quản lý Program
*   **Tạm dừng (Pause):** `POST /api/programs/{id}/pause`
*   **Tiếp tục (Resume):** `POST /api/programs/{id}/resume`
*   **Kết thúc sớm (End):** `POST /api/programs/{id}/end`

### 9.2. Nâng cấp từ Trial
> **Mục đích:** Chuyển một program đang ở trạng thái `trial` thành program trả phí. API này được gọi khi người dùng đang trong một lộ trình và quyết định nâng cấp.

*   **Method:** `POST`
*   **URL:** `/api/programs/{id}/upgrade-from-trial`

---

## 10. LUỒNG 9: ADMIN PORTAL (CMS)
(Giữ nguyên như phiên bản trước)

---

*   **Method:** `POST`
*   **URL:** `/api/debug/programs/{id}/reset`

---

| Code | Error Type | Mô tả | Hành động Frontend |
| :--- | :--- | :--- | :--- |
| **400** | `ValidationException` | Dữ liệu gửi lên không hợp lệ (thiếu trường, sai format). | Hiển thị lỗi form. |
| **401** | `Unauthorized` | Token hết hạn hoặc không hợp lệ. | Logout & Redirect Login. |
| **402** | `PaymentRequired` | Hết hạn dùng thử. | Hiển thị Popup thanh toán. |
| **403** | `ForbiddenException` | Không có quyền truy cập resource này. | Hiển thị thông báo lỗi quyền. |
| **404** | `NotFoundException` | Không tìm thấy ID tương ứng. | Redirect về trang 404 hoặc List. |
| **409** | `ConflictException` | Lỗi logic (VD: Đã có program active, Quiz đã nộp rồi). | Hiển thị thông báo lỗi logic. |
| **500** | `InternalServerError` | Lỗi không xác định từ phía Server. | Hiển thị "Vui lòng thử lại sau". |
