# TÀI LIỆU TÍCH HỢP API PROGRAM SERVICE (MASTER GUIDE)

**Phiên bản:** 3.0 (Master Full Coverage)
**Cập nhật:** 04/12/2025
**Đối tượng:** Frontend Developer (Mobile/Web)
**Mục tiêu:** Cung cấp đầy đủ tất cả API Endpoint, cấu trúc dữ liệu, và luồng nghiệp vụ để tích hợp hoàn chỉnh ứng dụng.

---

## 1. CẤU HÌNH CHUNG (GENERAL CONFIGURATION)

### 1.1. Base URL
*   **Development:** `http://localhost:8080`
*   **Production:** `https://api.smokefree.app/program`

### 1.2. Authentication Headers
Mọi request API (trừ các public endpoint được ghi chú) **BẮT BUỘC** phải kèm theo các header sau:

| Header | Giá trị mẫu | Mô tả |
| :--- | :--- | :--- |
| `Authorization` | `Bearer <JWT_TOKEN>` | Token xác thực từ Auth Service. |
| `X-User-Id` | `UUID` | ID của user đang thực hiện request. | `550e8400-e29b...` |
| `X-User-Group` | `CUSTOMER` | Nhóm người dùng: `CUSTOMER`, `COACH`, `ADMIN`. |
| `X-User-Tier` | `BASIC` | Hạng thành viên: `BASIC`, `PREMIUM`, `VIP`. |

### 1.3. Timezone & Format
*   **DateTime Format:** ISO-8601 UTC (Ví dụ: `2025-12-04T15:30:00Z`).
*   **Date Format:** `YYYY-MM-DD` (Ví dụ: `2025-12-04`).
*   **Frontend Rule:** Server luôn trả về giờ UTC. Frontend chịu trách nhiệm convert sang Local Time của user.

---

## 2. LUỒNG 1: ONBOARDING (NGƯỜI DÙNG MỚI)

### 2.1. Lấy bộ câu hỏi đánh giá đầu vào (Baseline Quiz)
*   **Endpoint:** `GET /api/onboarding/baseline/quiz`
*   **Mô tả:** Lấy danh sách câu hỏi trắc nghiệm để đánh giá mức độ nghiện.
*   **Response (200 OK):**
    ```json
    {
      "attemptId": null, // Luôn null ở bước này vì chưa tạo attempt thật
      "templateId": "uuid-template",
      "version": 1,
      "questions": [
        {
          "questionNo": 1,
          "questionText": "Bạn hút điếu thuốc đầu tiên khi nào?",
          "choices": {
            "A": "Trong vòng 5 phút",
            "B": "Sau 30 phút"
          }
        }
      ]
    }
    ```

### 2.2. Nộp bài đánh giá & Nhận lộ trình gợi ý
*   **Endpoint:** `POST /api/onboarding/baseline`
*   **Mô tả:** Gửi câu trả lời, hệ thống sẽ tính toán và gợi ý gói lộ trình phù hợp.
*   **Request Body:**
    ```json
    {
      "templateId": "uuid-template-lay-o-buoc-truoc",
      "answers": [
        { "q": 1, "score": 3 }, // Score lấy từ weight của đáp án user chọn
        { "q": 2, "score": 1 }
      ]
    }
    ```
*   **Response (200 OK):**
    ```json
    {
      "userId": "uuid",
      "baselineScore": 8,
      "addictionLevel": "HIGH",
      "recommendedTemplateId": "uuid-plan-template", // LƯU ID NÀY ĐỂ TẠO PROGRAM
      "recommendationReason": "Mức độ phụ thuộc cao, cần lộ trình giảm dần..."
    }
    ```

### 2.3. Xem chi tiết gói lộ trình (Optional)
*   **Endpoint:** `GET /api/plan-templates/{id}`
*   **Mô tả:** Xem chi tiết gói lộ trình trước khi quyết định tham gia.

---

## 3. LUỒNG 2: ĐĂNG KÝ & KHỞI ĐỘNG (ENROLLMENT)

### 3.1. Tạo chương trình (Bắt đầu hành trình)
*   **Endpoint:** `POST /v1/programs`
*   **Mô tả:** Đăng ký tham gia lộ trình.
*   **Request Body:**
    ```json
    {
      "planTemplateId": "uuid-recommended-template",
      "trial": true, // true: Dùng thử 7 ngày | false: Trả phí ngay
      "coachId": null // (Optional) UUID của Coach nếu có
    }
    ```
*   **Response (200 OK):** Object `ProgramRes` (chi tiết bên dưới).
*   **Side Effect:** User nhận ngay huy hiệu **"Khởi Hành" (PROG_LV1)**.

---

## 4. LUỒNG 3: DASHBOARD & HOME SCREEN (TRUNG TÂM)

### 4.1. Lấy dữ liệu tổng hợp Dashboard
*   **Endpoint:** `GET /api/me`
*   **Mô tả:** API quan trọng nhất, gọi mỗi khi user vào App. Trả về mọi thứ cần thiết cho màn hình chính.
*   **Response Structure:**
    ```json
    {
      "userId": "uuid",
      "subscription": {
        "tier": "BASIC",
        "status": "ACTIVE",
        "expiresAt": null
      },
      "activeProgram": {
        "id": "uuid",
        "templateName": "Lộ trình 30 ngày",
        "status": "ACTIVE", // Quan trọng: Check ACTIVE/PAUSED/COMPLETED
        "currentDay": 5, // (Lưu ý: Server tự động tính toán dựa trên ngày bắt đầu, Frontend không cần tự tính)
        "planDays": 30,
        "isTrial": true,
        "trialRemainingDays": 2
      },
      "dueQuizzes": [ // Bài kiểm tra cần làm ngay
        { "templateId": "...", "templateName": "Check-in Tuần 1", "isOverdue": false }
      ],
      "streakInfo": {
        "currentStreak": 5, // Số ngày streak
        "daysWithoutSmoke": 5 // Tổng số ngày không hút
      }
    }
    ```

### 4.2. Hệ thống Huy hiệu (Badges)
*   **Endpoint:** `GET /api/me/badges`
*   **Mô tả:** Lấy danh sách huy hiệu user ĐÃ đạt được.
*   **Endpoint:** `GET /api/me/badges/all`
*   **Mô tả:** Lấy danh sách TẤT CẢ huy hiệu (để hiển thị danh sách mờ/bị khóa).

---

## 5. LUỒNG 4: THỰC HIỆN HẰNG NGÀY (DAILY TASKS)

### 5.1. Lấy bài tập hôm nay
*   **Endpoint:** `GET /api/programs/{programId}/steps/today`
*   **Mô tả:** Lấy danh sách nhiệm vụ cần làm trong ngày.
*   **Lưu ý quan trọng:** Hệ thống tính "Ngày hôm nay" theo giờ **Server (UTC)**. Frontend không cần gửi ngày lên, nhưng cần biết để giải thích cho user nếu thấy bài tập mới xuất hiện lệch múi giờ.
*   **Response:** Array `StepAssignment`.
    ```json
    [
      {
        "id": "uuid-step-assignment",
        "step": {
          "id": "uuid-step",
          "title": "Đọc bài viết về Nicotine",
          "type": "READ", // READ, WATCH, LISTEN, EXERCISE, QUIZ, CHECKIN
          "contentModuleId": "uuid-content-module" // Dùng ID này gọi API content
        },
        "status": "PENDING", // PENDING, COMPLETED, SKIPPED
        "scheduledAt": "2025-12-04"
      }
    ]
    ```

### 5.2. Lấy nội dung bài học
*   **Endpoint:** `GET /api/modules/by-code/{code}` (Hoặc dùng ID nếu có API by ID)
*   **Mô tả:** Lấy nội dung chi tiết (Text, Video URL) của bài học.

### 5.3. Cập nhật trạng thái bài tập
*   **Endpoint:** `PATCH /api/programs/{programId}/steps/{stepId}/status`
*   **Request Body:**
    ```json
    {
      "status": "COMPLETED",
      "note": "User note (optional)"
    }
    ```

### 5.4. Bỏ qua bài tập (Skip)
*   **Endpoint:** `POST /api/programs/{programId}/steps/{stepId}/skip`

---

## 6. LUỒNG 5: QUIZ ENGINE (KIỂM TRA ĐỊNH KỲ)

### 6.1. Danh sách Quiz cần làm
*   **Endpoint:** `GET /v1/me/quizzes`
*   **Mô tả:** Lấy danh sách các bài quiz đang mở.

### 6.2. Làm bài Quiz
1.  **Mở đề:** `POST /v1/me/quizzes/{templateId}/open` -> Trả về `attemptId` và danh sách câu hỏi.
2.  **Lưu câu trả lời:** `PUT /v1/me/quizzes/{attemptId}/answer`
    ```json
    { "questionNo": 1, "answer": 2 }
    ```
3.  **Nộp bài:** `POST /v1/me/quizzes/{attemptId}/submit` -> Trả về kết quả chấm điểm.
    *   **Tác động:** Hệ thống tự động check huy hiệu Quiz Level 1, 2, 3.

---

## 7. LUỒNG 6: TRACKING & SỰ KIỆN HÚT THUỐC

### 7.1. Báo cáo sự cố (Slip/Relapse)
*   **Endpoint:** `POST /api/programs/{programId}/smoke-events`
*   **Mô tả:** User báo cáo lỡ hút thuốc hoặc thèm thuốc.
*   **Request Body:**
    ```json
    {
      "eventType": "SMOKE", // SMOKE (Hút) hoặc URGE (Thèm)
      "kind": "SLIP",       // SLIP, LAPSE, RELAPSE
      "puffs": 3,           // Số hơi
      "reason": "STRESS",
      "eventAt": "ISO-8601-Time"
    }
    ```
*   **Hệ quả:** Reset Streak về 0 (nhưng không mất huy hiệu cũ).

### 7.2. Lịch sử & Thống kê
*   **Endpoint:** `GET /api/programs/{programId}/smoke-events/history`
*   **Endpoint:** `GET /api/programs/{programId}/smoke-events/stats?period=WEEK`

### 7.3. Xem chi tiết Streak
*   **Endpoint:** `GET /api/programs/{programId}/streak`

---

## 8. LUỒNG 7: QUẢN LÝ CHƯƠNG TRÌNH (SETTINGS)

### 8.1. Tạm dừng (Pause)
*   **Endpoint:** `POST /api/programs/{id}/pause`
*   **CẢNH BÁO:** Frontend cần hiện popup: "Tạm dừng sẽ làm mất cơ hội nhận huy hiệu 'Kiên Trì' và 'Về Đích'. Bạn chắc chắn chứ?".

### 8.2. Tiếp tục (Resume)
*   **Endpoint:** `POST /api/programs/{id}/resume`

### 8.3. Kết thúc sớm (End)
*   **Endpoint:** `POST /api/programs/{id}/end`

### 8.4. Kiểm tra Trial & Nâng cấp
*   **Endpoint:** `GET /api/programs/{id}/trial-status`
*   **Endpoint:** `POST /api/subscriptions/upgrade` (Mock Payment)
    ```json
    { "targetTier": "PREMIUM" }
    ```

---

## 9. ADMIN APIs (DÀNH CHO CMS/BACKOFFICE)

*   **Quiz Template:** `POST /v1/admin/quizzes`
*   **Plan Template:** `POST /v1/admin/plan-templates`
*   **Content Module:** `POST /api/modules`
*   **List Programs:** `GET /api/programs/admin` (Phân trang)
*   **Dev Tool (Time Travel):** `PATCH /api/programs/{id}/current-day` (Chỉnh ngày hiện tại).
*   **Dev Tool (View Steps):** `GET /api/programs/{id}/steps/debug/by-date/{date}` (Xem bài tập ngày bất kỳ - Chỉ môi trường Dev).

---

## 10. MÃ LỖI (ERROR HANDLING)

Frontend cần xử lý các mã lỗi HTTP chuẩn sau:

*   **400 Bad Request:** Dữ liệu gửi lên sai định dạng (Validation Error).
*   **401 Unauthorized:** Token hết hạn hoặc không hợp lệ -> Logout & Redirect Login.
*   **402 Payment Required:** Hết hạn dùng thử -> Show Payment Modal.
*   **403 Forbidden:** Không có quyền truy cập.
*   **404 Not Found:** Resource không tồn tại.
*   **409 Conflict:** Lỗi logic nghiệp vụ (VD: Đang Active không được tạo mới, Quiz chưa đến giờ làm).
*   **500 Internal Server Error:** Lỗi hệ thống -> Show "Try again later".
