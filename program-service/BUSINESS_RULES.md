# Smoking Cessation Program - Business Rules & Logic

> **Phiên bản:** 2.0 (Cập nhật theo Source Code & Java 25 Architecture)
> **Trạng thái:** Chính xác với Implementation (Streak Recovery, Hard Stop, Quiz Engine)

---

## 🎯 Tổng quan

**Program Service** là trái tim của hệ thống, quản lý lộ trình cai thuốc lá cá nhân hóa. Nó không chỉ lưu trữ dữ liệu mà còn vận hành một **State Engine** phức tạp để xử lý các trạng thái dùng thử, chuỗi thành tích (streak), và lịch học.

---

## 📋 1. Quy trình Onboarding & Tạo Program

### 1.1. Đánh giá đầu vào (Baseline Assessment)
**Logic:**
1.  Người dùng làm bài kiểm tra Fagerström (hoặc tương tự).
2.  Hệ thống tính `TotalScore` và xác định `Severity` (LOW, MODERATE, HIGH).
3.  **Đề xuất lộ trình (Plan Recommendation):**
    *   **LOW:** Gợi ý gói `L1_30D` (30 ngày).
    *   **MODERATE:** Gợi ý gói `L2_45D` (45 ngày).
    *   **HIGH:** Gợi ý gói `L3_60D` (60 ngày).

### 1.2. Khởi tạo chương trình (Program Creation)
**Class:** `ProgramServiceImpl.createProgram`
**Logic:**
1.  **Kiểm tra trùng lặp:** Một User chỉ được có **1 Active Program**. Nếu đã có -> Lỗi `409 Conflict`.
2.  **Chế độ Dùng thử (Trial):**
    *   Mặc định kích hoạt nếu không có thông tin thanh toán.
    *   Thiết lập `trialEndExpected = Now + 7 days`.
3.  **Tự động gán (Auto-Assignment):**
    *   Hệ thống quét bảng `plan_quiz_schedules` của template đã chọn.
    *   Tạo hàng loạt bản ghi `quiz_assignments` cho User với trạng thái `ACTIVE`.
    *   Các bài tập này đóng vai trò là "cam kết tương lai", sẽ hiển thị khi đến ngày (`startOffsetDay`).

---

## 🛑 2. Cơ chế Dùng thử & Hard Stop (Subscription Enforcement)

Khác với các hệ thống thụ động, chúng tôi áp dụng cơ chế **Lazy Hard Stop**.

**Quy tắc:**
*   **Không chạy Background Job:** Không có job nào chạy ngầm để quét và khóa tài khoản.
*   **Kiểm tra tại thời điểm truy cập (On-Access Check):**
    *   Khi User gọi API lấy thông tin (`getActive`) hoặc làm bài (`openAttempt`).
    *   Hệ thống kiểm tra: `IF (trialEndExpected != null AND Now > trialEndExpected)`.
    *   **Hành động:** Ném ngoại lệ `SubscriptionRequiredException` ngay lập tức.
*   **Hệ quả:** Frontend nhận mã lỗi (402/403) và hiện màn hình chặn (Paywall). API hoàn toàn không trả về dữ liệu program.

---

## 🔥 3. Logic Chuỗi (Streak) & Tính năng "Cứu Chuỗi" (Recovery)

Đây là tính năng phức tạp và giá trị nhất ("Killer Feature").

### 3.1. Định nghĩa Streak
*   **Streak:** Một khoảng thời gian liên tục (`startedAt` -> `endedAt`) không hút thuốc.
*   **StreakBreak:** Một sự kiện gãy chuỗi (User báo cáo hút thuốc).

### 3.2. Quy trình Gãy chuỗi (Relapse Flow)
Khi User báo cáo hút thuốc (`StreakService.breakStreak`):
1.  Chuỗi hiện tại (`Streak`) bị đóng lại: `endedAt = Now`.
2.  Tạo bản ghi `StreakBreak` liên kết với sự kiện hút thuốc.
3.  Bộ đếm hiển thị (`streakCurrent`) trên Program reset về 0.

### 3.3. Quy trình Cứu chuỗi (Streak Recovery Flow)
User có cơ hội "sửa sai" bằng cách học bài học.
1.  **Gán bài tập phục hồi:** Hệ thống (hoặc Admin/Logic tự động) gán một bài Quiz đặc biệt với `origin = STREAK_RECOVERY`.
2.  **Người dùng hoàn thành Quiz:**
    *   Gọi `QuizFlowServiceImpl.submit`.
    *   Logic kiểm tra: Nếu bài quiz có origin là `STREAK_RECOVERY`.
3.  **Hành động "Chữa lành":**
    *   Tìm `StreakBreak` mới nhất.
    *   Tìm bản ghi `Streak` lịch sử tương ứng.
    *   **Thao tác:** Xóa `endedAt` của bản ghi `Streak` (set về NULL) và tính toán lại độ dài chuỗi.
    *   **Kết quả:** Chuỗi được nối lại như chưa từng bị gãy.

---

## 📝 4. Quiz Engine (Hệ thống Bài tập)

### 4.1. Lịch trình & Hiển thị (Scheduling)
**Class:** `QuizFlowServiceImpl.listDue`
**Logic Tối ưu (N+1 Prevention):**
*   Thay vì query từng bài, hệ thống tải toàn bộ `Assignments`, `Results` (mới nhất) và `Templates` vào bộ nhớ.
*   **Tính toán Due Date (In-Memory):**
    *   **ONCE:** Bài chỉ làm 1 lần. Hiển thị vào ngày `startOffset`. Ẩn nếu đã làm xong (trừ khi là Recovery).
    *   **RECURRING (Hàng ngày/tuần):** Dựa vào `everyDays`. Nếu đã làm, tính ngày tiếp theo = `LastSubmitDate + everyDays`.
    *   **Kết quả:** Danh sách trả về được sắp xếp theo độ ưu tiên và thời gian.

### 4.2. Lưu trữ & Chấm điểm
*   **Lưu nháp (`saveAnswer`):** Ghi vào bảng `quiz_answers`. Sử dụng khóa phức hợp `(attempt_id, question_no)` để đảm bảo mỗi câu chỉ có 1 đáp án.
*   **Nộp bài (`submit`):**
    *   Tính `TotalScore` ngay lập tức.
    *   Xác định `Severity` của lần làm bài đó.
    *   Kích hoạt các side-effect (như Badge, Recovery).

---

## 📚 5. Quản lý Nội dung (Content Modules)

**Class:** `ContentModuleServiceImpl`
**Chiến lược:** Versioning (Không ghi đè)
*   **Create:** Tạo bản ghi mới, version 1.
*   **Update:** Không sửa bản ghi cũ. Tạo bản ghi mới với `version = oldVersion + 1`.
*   **Get:** Luôn lấy bản ghi có version cao nhất (`findTopByCode...OrderByVersionDesc`) trừ khi yêu cầu lịch sử cụ thể.
*   **Mục đích:** Đảm bảo User đang học dở nội dung cũ không bị lỗi dữ liệu, đồng thời hỗ trợ Audit log.

---

## 🔒 6. Bảo mật & Phân quyền

*   **Stateless:** Service không lưu Session.
*   **Identity:** Tin tưởng tuyệt đối vào Header `X-User-Id` từ API Gateway.
*   **Role:**
    *   **CUSTOMER:** Chỉ thao tác trên dữ liệu của chính mình (`owner`).
    *   **COACH:** Được xem/sửa dữ liệu của học viên được gán (`coachId`).
    *   **ADMIN:** Full quyền quản trị Template và System.

---

## ⚠️ 7. Các trường hợp biên (Edge Cases)

1.  **User dùng thử hết hạn cố gọi API:**
    *   Bị chặn bởi Exception `SubscriptionRequiredException`. API trả lỗi 402/403.
2.  **User cố làm lại bài Quiz kiểu ONCE:**
    *   Bị chặn bởi logic check `Result` đã tồn tại (trừ khi là bài Recovery).
3.  **User cố "hack" Streak bằng cách xóa app:**
    *   Dữ liệu lưu trên Server (PostgreSQL). Cài lại app vẫn giữ nguyên trạng thái cũ.
4.  **Xung đột dữ liệu Quiz:**
    *   Nếu User mở 2 attempt cùng lúc trên 2 thiết bị -> Hệ thống chặn attempt thứ 2 vì trạng thái `OPEN` đã tồn tại.

---
*Tài liệu này là nguồn chân lý (Single Source of Truth) cho logic nghiệp vụ của Program Service.*