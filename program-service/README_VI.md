# Program Service 🚀

> **Core Engine cho Nền tảng Cai thuốc lá Smokefree**
>
> Service này quản lý toàn bộ hành trình của người dùng: từ việc tạo lộ trình cá nhân hóa, lên lịch bài kiểm tra (quiz) cho đến theo dõi thói quen (streaks) và cung cấp nội dung bài học.

---

## 📋 Mục lục
- [Tổng quan](#-tổng-quan)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Logic Nghiệp vụ Cốt lõi](#-logic-nghiệp-vụ-cốt-lõi)
- [Cơ sở dữ liệu](#-cơ-sở-dữ-liệu)
- [Hướng dẫn cài đặt](#-hướng-dẫn-cài-đặt)
- [Xác thực API](#-xác-thực-api)

---

## 📖 Tổng quan

**Program Service** là một microservice quản lý trạng thái (state-managed), được thiết kế để hỗ trợ người dùng cai thuốc thông qua một lộ trình khoa học. Nó không chỉ đơn thuần là CRUD mà còn xử lý các logic nghiệp vụ phức tạp như **Khôi phục chuỗi (Streak Recovery)**, **Cưỡng chế dùng thử (Trial Enforcement)**, và **Lên lịch Quiz động**.

### Tính năng chính
*   **Lộ trình cá nhân hóa:** Tự động chỉ định lộ trình 30/45/60 ngày dựa trên mức độ nghiện của người dùng (tính từ bài test đầu vào).
*   **Quiz Engine thông minh:** Lên lịch cho các bài Quiz Hàng ngày/Hàng tuần với cơ chế tính toán ngày đến hạn (Due Date) hiệu năng cao (Lazy Loading).
*   **Gamification & Theo dõi:** Theo dõi chuỗi ngày không hút thuốc, quản lý các lần "trượt ngã" (breaks), và cho phép khôi phục chuỗi thông qua các bài học can thiệp.
*   **Quản lý nội dung:** Cung cấp tài liệu giáo dục (`ContentModule`) với cơ chế quản lý phiên bản (Versioning).
*   **Cưỡng chế dùng thử (Trial Hard Stop):** Logic chặn truy cập các tính năng trả phí ngay lập tức khi gói dùng thử hết hạn.

---

## 🛠 Công nghệ sử dụng

*   **Ngôn ngữ:** Java 25 (Phiên bản mới nhất - Bleeding Edge)
*   **Framework:** Spring Boot 3.5.7
*   **Cơ sở dữ liệu:** PostgreSQL 14+ (Schema: `program`)
*   **Quản lý Migration:** Flyway (Phiên bản hiện tại: V42)
*   **Build Tool:** Maven (Kèm Wrapper)
*   **Containerization:** Docker (Eclipse Temurin 25 Alpine)
*   **Tiện ích:** Lombok 1.18.42, Hibernate Types 60

---

## 🏗 Kiến trúc hệ thống

Dự án tuân theo **Kiến trúc phân lớp (Layered Architecture)** với sự phân tách trách nhiệm rõ ràng:

```
src/main/java/com/smokefree/program
├── auth/           # Bộ lọc bảo mật (HeaderUserContextFilter)
├── config/         # Cấu hình App (Security, CORS, Props)
├── domain/         
│   ├── model/      # JPA Entities (Program, QuizAssignment, Streak...)
│   ├── repo/       # Spring Data Repositories
│   └── service/    # Interfaces & Implementations của Business Logic
└── web/            
    ├── controller/ # Các REST Endpoints
    ├── dto/        # Data Transfer Objects
    └── error/      # Xử lý lỗi toàn cục (Global Exception Handling)
```

---

## 🧠 Logic Nghiệp vụ Cốt lõi

### 1. Tạo chương trình & Tự động gán (Auto-Assignment)
Khi người dùng bắt đầu (`ProgramServiceImpl`), hệ thống sẽ:
1.  Đọc `UserBaselineResult` để xác định mức độ nghiện.
2.  Chọn một `PlanTemplate` phù hợp (ví dụ: `L1_30D`).
3.  **Tự động gán Quiz:** Đọc từ `PlanQuizSchedule` và tạo hàng loạt bản ghi `QuizAssignment` cho người dùng. Đây được coi là các "cam kết tương lai" cho việc cung cấp nội dung.

### 2. Logic "Hard Stop" cho Dùng thử
Hệ thống giới hạn quyền truy cập một cách thụ động nhưng nghiêm ngặt.
*   **Logic:** Trong hàm `ProgramService.getActive()`, hệ thống kiểm tra `Instant.now() > trialEndExpected`.
*   **Kết quả:** Nếu đã hết hạn, một `SubscriptionRequiredException` sẽ được ném ra ngay lập tức, chặn request trước khi bất kỳ dữ liệu nào được trả về.

### 3. Khôi phục chuỗi (Streak Recovery - "Killer Feature")
Chúng tôi coi việc tái nghiện là một phần của hành trình, không phải là kết thúc.
*   **Luồng:** Người dùng làm gãy chuỗi -> Bản ghi `StreakBreak` được tạo ra.
*   **Khôi phục:** Người dùng hoàn thành một bài "Recovery Quiz" đặc biệt.
*   **Xử lý:** Khi nộp bài (`QuizFlowServiceImpl.submit`), hệ thống tìm `StreakBreak` gần nhất và "chữa lành" bản ghi `Streak` lịch sử bằng cách xóa dấu thời gian `endedAt` của nó.

### 4. Tối ưu hóa Quiz Engine
Để ngăn chặn lỗi **N+1 queries** khi liệt kê các bài quiz đến hạn:
*   Hệ thống tải toàn bộ Assignments, Results, và Templates theo các lô (batch) song song.
*   Ngày đến hạn (logic `ONCE` so với `RECURRING`) được tính toán trong bộ nhớ (in-memory), đảm bảo chỉ tốn rất ít round-trip xuống database.

---

## 🗄 Cơ sở dữ liệu (Database Schema)

Service sử dụng một schema riêng biệt tên là **`program`**. Các bảng chính bao gồm:

*   **`programs`**: Bảng gốc tổng hợp. Lưu trạng thái, ngày bắt đầu và các bộ đếm streak hiện tại.
*   **`quiz_templates`**: Dữ liệu gốc cho các câu hỏi và câu trả lời.
*   **`quiz_assignments`**: Liên kết User/Program với Template cùng các quy tắc lịch trình.
*   **`quiz_attempts`** & **`quiz_answers`**: Lưu bài làm của người dùng. Lưu ý: `quiz_answers` sử dụng khóa phức hợp `(attempt_id, question_no)`.
*   **`streaks`** & **`streak_breaks`**: Theo dõi các khoảng thời gian không hút thuốc liên tục.
*   **`smoke_events`**: Nhật ký các lần hút thuốc riêng lẻ.

*Các migration được quản lý qua Flyway tại `src/main/resources/db/migration`.*

---

## 🚀 Hướng dẫn cài đặt

### Yêu cầu
*   **JDK 25** (Bắt buộc để biên dịch)
*   Docker & Docker Compose (cho PostgreSQL)

### Chạy Local

1.  **Khởi động Database:**
    ```bash
    docker-compose up -d postgres
    ```

2.  **Build Dự án:**
    ```bash
    ./mvnw clean install
    ```

3.  **Chạy Ứng dụng:**
    ```bash
    ./mvnw spring-boot:run
    ```
    *Ứng dụng sẽ khởi động tại cổng `8080` (mặc định).*

---

## 🔐 Xác thực API

Service này được thiết kế để chạy sau một API Gateway. Nó hoạt động theo cơ chế **Stateless**.

*   **Cơ chế:** Tin tưởng gateway phía trước thực hiện việc xác thực.
*   **Định danh:** Dựa vào HTTP Header **`X-User-Id`**.
*   **Context:** `HeaderUserContextFilter` sẽ trích xuất ID này và tạo ra một `UserPrincipal` cho security context.

**Ví dụ Request:**
```http
GET /v1/programs/active
X-User-Id: a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11
X-User-Tier: PREMIUM  <-- Tùy chọn, dùng để kiểm tra quyền lợi (entitlement)
```

---
*© 2024 Smokefree Project. Tài liệu lưu hành nội bộ.*
