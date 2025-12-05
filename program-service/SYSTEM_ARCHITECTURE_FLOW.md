# 🏛️ HỆ THỐNG PROGRAM SERVICE - KIẾN TRÚC & LUỒNG HOẠT ĐỘNG

## 1. Tổng Quan Hệ Thống (System Overview)
`program-service` là một microservice chịu trách nhiệm quản lý vòng đời cai thuốc của người dùng. Nó hoạt động như một API Server, nhận request từ Gateway/Frontend, xử lý logic nghiệp vụ và lưu trữ vào PostgreSQL.

*   **Authentication:** Stateless. Tin tưởng `X-User-Id`, `X-User-Group`, `X-User-Tier` từ Header.
*   **Authorization:** Sử dụng `@PreAuthorize` và `HeaderUserContextFilter` để phân quyền.
*   **Core Domains:** Program (Lộ trình), Quiz (Đánh giá), Step (Bài học), Tracking (Theo dõi).

---

## 2. Luồng Nghiệp Vụ Chính (Business Flows)

### A. Khởi Tạo Lộ Trình (Program Creation & Enrollment)
Đây là điểm bắt đầu của mọi user.

1.  **Client** gọi `POST /v1/programs`.
2.  **`ProgramController`** nhận request, gọi `ProgramService.createProgram`.
3.  **`ProgramServiceImpl`**:
    *   **Validation:** Kiểm tra user đã có program `ACTIVE` chưa.
    *   **Template Selection:** Chọn `PlanTemplate` (ví dụ: 30 ngày) từ DB.
    *   **Program Creation:** Tạo entity `Program` mới (Paid hoặc Trial 7 ngày).
    *   **Content Generation:** Gọi `StepAssignmentService` để sao chép (`clone`) toàn bộ các bước (`PlanStep`) từ Template sang bảng `StepAssignment` của riêng user.
    *   **Quiz Automation:** Tự động gán 2 bài quiz quan trọng vào bảng `QuizAssignment`:
        *   *Onboarding Assessment:* Làm ngay lập tức.
        *   *Weekly Check-in:* Lặp lại mỗi 7 ngày.
4.  **Kết quả:** User có một lộ trình hoàn chỉnh với danh sách bài học và bài kiểm tra đã được lên lịch.

### B. Học Tập Hàng Ngày (Daily Step Management)
User truy cập ứng dụng mỗi ngày để xem nhiệm vụ.

1.  **Client** gọi `GET /api/programs/{id}/steps/today` (hoặc list all).
2.  **`StepController`** gọi `StepAssignmentService`.
3.  **Logic:**
    *   Lọc các `StepAssignment` có `scheduledAt` trùng với ngày hôm nay.
    *   Nếu step có `contentModuleCode`, hệ thống (thông qua FE hoặc API riêng `/api/modules`) sẽ tải nội dung bài học (JSON) để hiển thị.
4.  **Tương tác:**
    *   User hoàn thành bài học -> `PATCH .../status` (`COMPLETED`).
    *   User bận -> `POST .../skip` hoặc `PATCH .../reschedule`.

### C. Đánh Giá & Kiểm Tra (Quiz System)
Hệ thống đánh giá tiến độ user thông qua các bài Quiz.

1.  **Kiểm tra bài tập (`GET /v1/me/quizzes`):**
    *   `QuizFlowService` quét bảng `QuizAssignment`.
    *   Tính toán `dueDate` dựa trên ngày bắt đầu program hoặc lần làm bài cuối.
    *   Nếu đến hạn (Due) -> Trả về danh sách.
2.  **Làm bài (`POST .../open`):**
    *   **Hard Stop Check:** Kiểm tra xem Trial còn hạn không? Nếu hết -> Chặn (`402 Payment Required`).
    *   Tạo `QuizAttempt` (trạng thái `OPEN`).
3.  **Nộp bài (`POST .../submit`):**
    *   Tính điểm tổng (`totalScore`).
    *   Xếp loại mức độ nghiện (`SeverityLevel`: LOW/MODERATE/HIGH).
    *   Lưu kết quả vào `QuizResult`.
    *   Đóng `QuizAttempt`.

### D. Theo Dõi Hành Vi (Tracking & Gamification)
User báo cáo trạng thái cai thuốc (Check-in).

1.  **Client** gọi `POST /api/programs/{id}/smoke-events`.
2.  **`SmokeEventService`**:
    *   Lưu sự kiện (`SmokeEvent`) vào DB.
    *   Cập nhật `lastSmokeAt` trong `Program`.
3.  **Xử lý Streak (`StreakService`):**
    *   Nếu sự kiện là `SLIP` (lỡ hút) hoặc `RELAPSE` (tái nghiện) -> **Reset Streak** về 0 (Tạo `StreakBreak`).
    *   Nếu sự kiện là `NO_SMOKE` -> Tăng `currentStreak`.
4.  **Hiển thị:** Client gọi `GET /api/me` để xem số ngày streak hiện tại.

---

## 3. Cơ Chế Bảo Vệ & Logic Đặc Biệt

### 🛡️ Trial Hard Stop (Chặn Dùng Thử)
*   **Logic:** Tại `ProgramService.getActive()`, hệ thống luôn kiểm tra:
    `if (trialEndExpected != null && trialEndExpected < NOW)` -> **Throw Exception**.
*   **Tác động:** Mọi API dựa vào `getActive` (như làm Quiz, xem Dashboard) sẽ tự động bị chặn khi hết hạn dùng thử, buộc user phải thanh toán (`upgrade-from-trial`).

### 🔄 Auto-Assign Quiz
*   **Logic:** Không cần Coach hay Admin gán tay. Ngay khi tạo Program, hệ thống tự động inject các bản ghi `QuizAssignment` dựa trên quy ước tên Template ("Onboarding Assessment", "Weekly Check-in").

### 🧩 Content Decoupling
*   **Cấu trúc:** `PlanStep` chỉ lưu mã tham chiếu (`moduleCode`) chứ không lưu nội dung.
*   **Lợi ích:** Admin có thể cập nhật nội dung bài học (`ContentModule`) độc lập mà không làm hỏng lịch trình của người dùng.

---

## 4. Luồng Dữ Liệu (Data Flow Diagram - Mental Model)

```text
[USER] 
  |
  v
(Gateway/Auth Filter) -> Xác thực UserID/Role
  |
  v
[CONTROLLERS] (Program, Step, MeQuiz, Streak...)
  |
  v
[SERVICES] 
  |-- ProgramService: Orchestrator (Điều phối)
  |     |-- Gọi StepAssignmentService (Tạo bài học)
  |     |-- Gọi QuizAssignmentRepo (Gán đề thi)
  |
  |-- QuizFlowService: Xử lý logic làm bài
  |-- StreakService: Tính toán chuỗi ngày
  |
  v
[REPOSITORIES] -> Giao tiếp PostgreSQL
  |-- Program, PlanTemplate, StepAssignment
  |-- QuizTemplate, QuizAttempt, QuizResult
  |-- SmokeEvent, Streak
```
