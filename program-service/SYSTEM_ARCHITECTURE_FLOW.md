# 🏛️ HỆ THỐNG PROGRAM SERVICE - KIẾN TRÚC & LUỒNG HOẠT ĐỘNG (Đã cập nhật)

## 1. Tổng Quan Hệ Thống (System Overview)
`program-service` là một microservice chịu trách nhiệm quản lý vòng đời cai thuốc của người dùng. Nó hoạt động như một API Server, nhận request từ Gateway/Frontend, xử lý logic nghiệp vụ và lưu trữ vào PostgreSQL.

*   **Authentication:** Stateless. Tin tưởng `X-User-Id`, `X-User-Group`, `X-User-Tier` từ Header.
*   **Authorization:** Sử dụng `@PreAuthorize` và các filter tùy chỉnh (`HeaderUserContextFilter`, `DevAutoUserFilter`) để phân quyền.
*   **Core Domains:** Program (Lộ trình), Quiz (Đánh giá), Step (Bài học), Tracking (Theo dõi).

---

## 2. Luồng Nghiệp Vụ Chính (Business Flows)

### A. Onboarding & Enrollment (Luồng Bắt Đầu)
Đây là luồng nghiệp vụ quan trọng nhất, được chia làm 2 giai đoạn bắt buộc.

#### Giai đoạn 1: Onboarding (Điều kiện tiên quyết)
Mục tiêu: Đánh giá mức độ ban đầu của người dùng. Người dùng **không thể** ghi danh nếu chưa hoàn thành bước này.

1.  **Client** gọi `GET /api/onboarding/baseline/quiz` để lấy nội dung bài quiz đánh giá.
2.  **`OnboardingFlowController`** xử lý, truy vấn `QuizTemplateRepository` để tìm mẫu quiz có `code = 'ONBOARDING_ASSESSMENT'`.
3.  **Client** hiển thị câu hỏi và nộp câu trả lời qua `POST /api/onboarding/baseline`.
4.  **`OnboardingFlowService`** nhận câu trả lời, tính toán kết quả và lưu vào bảng `UserBaselineResult`. Từ thời điểm này, người dùng đủ điều kiện để ghi danh.

#### Giai đoạn 2: Enrollment (Ghi danh vào Lộ trình)
Mục tiêu: Tạo một lộ trình cá nhân hóa cho người dùng. Luồng này được điều phối bởi `EnrollmentService`.

1.  **Client** gọi API ghi danh (ví dụ: `POST /api/me/enrollments`) với `planTemplateId` mà người dùng đã chọn.
2.  **Controller** (ví dụ: `MeController`) nhận request và gọi `EnrollmentService.startTrialOrPaid`.
3.  **`EnrollmentServiceImpl`** thực thi một giao dịch (transaction) duy nhất bao gồm các bước:
    *   **Validation:** Kiểm tra xem user đã hoàn thành Onboarding chưa (`baselineResultService.hasBaseline`) và đã có program `ACTIVE` nào khác chưa.
    *   **Template Loading:** Tải `PlanTemplate` từ DB dựa trên `planTemplateId`.
    *   **Program Creation:** Gọi `ProgramCreationService` để tạo một đối tượng `Program` trong bộ nhớ (với logic cho Trial hoặc Paid).
    *   **Save Program:** Lưu đối tượng `Program` vào DB thông qua `ProgramRepository`.
    *   **Content Generation:** Gọi `StepAssignmentService` để tạo các bản ghi `StepAssignment` (bài học hàng ngày) cho người dùng.
    *   **Quiz Automation:** Đọc bảng `plan_quiz_schedules` để tìm các quiz định kỳ (ví dụ: Weekly Check-in) được cấu hình cho `PlanTemplate` này, sau đó tạo các bản ghi tương ứng trong `QuizAssignment`.
    *   **Gamification:** Gọi `BadgeService` để kiểm tra và trao huy hiệu "Bắt đầu hành trình".
4.  **Kết quả:** User có một lộ trình hoàn chỉnh với danh sách bài học và các bài kiểm tra định kỳ đã được lên lịch.

### B. Học Tập Hàng Ngày (Daily Step Management)
User truy cập ứng dụng mỗi ngày để xem nhiệm vụ.

1.  **Client** gọi API để lấy nhiệm vụ hôm nay (ví dụ: `GET /api/me/dashboard`).
2.  **Service** liên quan sẽ lọc các `StepAssignment` có `scheduledAt` trùng với ngày hôm nay.
3.  Nếu step có `contentModuleCode`, hệ thống (thông qua FE hoặc API riêng `/api/modules`) sẽ tải nội dung bài học để hiển thị.
4.  **Tương tác:** User hoàn thành bài học -> `PATCH .../status` (`COMPLETED`).

### C. Đánh Giá & Kiểm Tra (Quiz System)
Hệ thống đánh giá tiến độ user thông qua các bài Quiz.

1.  **Kiểm tra bài tập (`GET /api/me/quizzes`):**
    *   Service (ví dụ: `MeService`) lấy `Program.currentDay` và tất cả `QuizAssignment` của user.
    *   **Lọc trong bộ nhớ:** Áp dụng logic `(currentDay - startDay) % every_days == 0` để xác định quiz nào đến hạn **hôm nay**.
    *   Trả về danh sách các quiz đến hạn.
2.  **Làm bài (`POST .../open`):**
    *   **Hard Stop Check:** Kiểm tra xem Trial còn hạn không. Nếu hết -> Chặn (`402 Payment Required`).
    *   Tạo `QuizAttempt` (trạng thái `OPEN`).
3.  **Nộp bài (`POST .../submit`):**
    *   Tính điểm, xếp loại `SeverityLevel`, lưu `QuizResult` và đóng `QuizAttempt`.

### D. Theo Dõi Hành Vi (Tracking & Gamification)
User báo cáo trạng thái cai thuốc (Check-in).

1.  **Client** gọi `POST /api/programs/{id}/smoke-events`.
2.  **`SmokeEventService`** lưu sự kiện và cập nhật `lastSmokeAt` trong `Program`.
3.  **Xử lý Streak (`StreakService`):**
    *   Nếu sự kiện là `SLIP` hoặc `RELAPSE` -> Reset `currentStreak` về 0 và tạo `StreakBreak`.
    *   Nếu là `NO_SMOKE` -> Tăng `currentStreak`.
4.  **Hiển thị:** Client gọi `GET /api/me` để xem số ngày streak hiện tại.

---

## 3. Cơ Chế Bảo Vệ & Logic Đặc Biệt

### 🛡️ Trial Hard Stop (Chặn Dùng Thử)
*   **Logic:** Tại các service quan trọng, hệ thống luôn kiểm tra `trialEndExpected` của `Program`. Nếu `trialEndExpected < NOW` -> **Throw Exception**.
*   **Tác động:** Mọi API quan trọng (làm Quiz, xem nội dung premium) sẽ tự động bị chặn khi hết hạn dùng thử.

### 🔄 Auto-Assign Quiz
*   **Logic:** Không cần gán tay. Ngay khi tạo Program, `EnrollmentServiceImpl` đọc bảng `plan_quiz_schedules` để tìm các quy tắc gán quiz.
*   **Cơ chế:** Dựa trên `plan_template_id`, hệ thống sẽ tìm các `quiz_template_id` tương ứng và lịch trình của chúng (`start_offset_day`, `every_days`) để tạo ra các bản ghi `QuizAssignment` cho người dùng.

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
[CONTROLLERS] (OnboardingFlow, Me, Step, QuizTemplate...)
  |
  +----------------------------------------------------------------+
  |                                                                |
  v                                                                v
[OnboardingFlowService]                                        [EnrollmentService] (Orchestrator chính)
-- Xử lý quiz baseline                                          |-- Gọi ProgramCreationService (Tạo object)
                                                                 |-- Gọi ProgramRepository (Lưu Program)
                                                                 |-- Gọi StepAssignmentService (Tạo bài học)
                                                                 |-- Gọi PlanQuizScheduleRepo (Đọc lịch quiz)
                                                                 |-- Gọi QuizAssignmentRepo (Gán quiz định kỳ)
                                                                 |-- Gọi BadgeService (Trao huy hiệu)
  v                                                                v
[REPOSITORIES] -> Giao tiếp PostgreSQL
  |-- QuizTemplateRepo, UserBaselineResultRepo                     |-- ProgramRepo, PlanTemplateRepo, StepAssignmentRepo
                                                                   |-- QuizAssignmentRepo, BadgeRepo
```
