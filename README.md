# Program Service 🚀

> **Core Engine cho Nền tảng Cai thuốc lá Smokefree**
>
> Service này quản lý toàn bộ hành trình của người dùng: từ việc tạo lộ trình cá nhân hóa, lên lịch bài kiểm tra (quiz) cho đến theo dõi thói quen (streaks), cung cấp nội dung bài học và hệ thống gamification.

---

## 📋 Mục lục
- [Tổng quan](#-tổng-quan)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Logic Nghiệp vụ Cốt lõi](#-logic-nghiệp-vụ-cốt-lõi)
- [Hệ thống Gamification](#-hệ-thống-gamification-badges)
- [Cơ sở dữ liệu](#-cơ-sở-dữ-liệu)
- [Xác thực & Phân quyền](#-xác-thực--phân-quyền)
- [Xử lý lỗi](#-xử-lý-lỗi)
- [Hướng dẫn cài đặt](#-hướng-dẫn-cài-đặt)

---

## 📖 Tổng quan

**Program Service** là một microservice quản lý trạng thái (state-managed), được thiết kế để hỗ trợ người dùng cai thuốc thông qua một lộ trình khoa học.

### Tính năng chính
*   **Lộ trình cá nhân hóa:** Tự động chỉ định lộ trình 30/45/60 ngày dựa trên mức độ nghiện.
*   **Quiz Engine thông minh:** Lên lịch Quiz Hàng ngày/Hàng tuần với cơ chế tính toán ngày đến hạn (Lazy Loading).
*   **Gamification:** Hệ thống Huy hiệu (Badges) và Chuỗi (Streak) để khuyến khích người dùng.
*   **Quản lý nội dung:** Cung cấp tài liệu giáo dục (`ContentModule`) với cơ chế quản lý phiên bản (Versioning).
*   **Cưỡng chế dùng thử (Trial Hard Stop):** Chặn truy cập ngay lập tức khi hết hạn dùng thử.

---

## 🛠 Công nghệ sử dụng

*   **Ngôn ngữ:** Java 25 (Eclipse Temurin)
*   **Framework:** Spring Boot 3.5.7
*   **Cơ sở dữ liệu:** PostgreSQL 14+ (Schema: `program`)
*   **Migration:** Flyway (Phiên bản: V42)
*   **Build Tool:** Maven (Wrapper included)
*   **Container:** Docker (Alpine Linux)

---

## 🏗 Kiến trúc hệ thống

```
src/main/java/com/smokefree/program
├── auth/           # Security Filters (HeaderUserContextFilter)
├── config/         # App Config (Security, CORS, Props)
├── domain/         
│   ├── model/      # JPA Entities (Program, QuizAssignment, Badge...)
│   ├── repo/       # Spring Data Repositories
│   └── service/    # Business Logic (ProgramService, BadgeService...)
└── web/            
    ├── controller/ # REST Endpoints
    │   ├── quiz/   # Quiz Controllers (Admin, Me)
    │   └── ...
    ├── dto/        # Data Transfer Objects
    └── error/      # Global Exception Handling
```

---

## 🧠 Logic Nghiệp vụ Cốt lõi

### 1. Tạo chương trình & Tự động gán (Auto-Assignment)
*   **Đầu vào:** Kết quả bài test `UserBaselineResult`.
*   **Xử lý:**
    1.  Chọn Template (30/45/60 ngày).
    2.  Tạo `Program` với trạng thái `ACTIVE`.
    3.  **Sinh dữ liệu:** Tự động tạo hàng loạt `StepAssignment` (bài học) và `QuizAssignment` (bài kiểm tra) cho tương lai.

### 2. Logic "Hard Stop" cho Dùng thử
*   **Cơ chế:** Kiểm tra thụ động tại thời điểm gọi API.
*   **Logic:** `IF (trialEndExpected < NOW) THEN Throw SubscriptionRequiredException`.
*   **Kết quả:** API trả về lỗi 402/403, chặn toàn bộ truy cập dữ liệu.

### 3. Khôi phục chuỗi (Streak Recovery)
*   **Vấn đề:** Người dùng hút thuốc -> Gãy chuỗi -> Nản lòng.
*   **Giải pháp:**
    1.  Gán bài Quiz phục hồi tâm lý (`STREAK_RECOVERY`).
    2.  Khi hoàn thành Quiz, hệ thống tìm điểm gãy gần nhất (`StreakBreak`).
    3.  **"Vá" lỗi:** Xóa ngày kết thúc của chuỗi cũ, nối liền mạch lại như chưa từng gãy.

---

## 🏆 Hệ thống Gamification (Badges)

Hệ thống tự động trao thưởng huy hiệu để giữ chân người dùng.

### Các loại huy hiệu:
1.  **Tiến độ (Milestone):** Trao khi hoàn thành 50%, 100% lộ trình.
2.  **Chuỗi (Streak):** Trao khi đạt 7, 30, 60 ngày không hút thuốc.
3.  **Học tập (Quiz):** Trao khi hoàn thành xuất sắc các bài kiểm tra.

### Cơ chế hoạt động:
*   **Trigger:** Huy hiệu được kiểm tra (`checkBadge`) bất đồng bộ (Async) sau mỗi hành động quan trọng (nộp bài, check-in hàng ngày).
*   **Lưu trữ:** Bảng `user_badges` lưu lịch sử nhận.
*   **API:** `GET /v1/me/badges` để xem bộ sưu tập huy hiệu.

---

## 🔐 Xác thực & Phân quyền

Service hoạt động **Stateless** sau API Gateway.

### 1. Định danh (Identity)
*   Tin tưởng Header: `X-User-Id`.
*   Được xử lý bởi: `HeaderUserContextFilter`.

### 2. Vai trò (Roles - RBAC)
Hệ thống hỗ trợ các role (truyền qua Header hoặc JWT):
*   **CUSTOMER:** Người dùng cuối. Chỉ truy cập dữ liệu của chính mình.
*   **COACH:** Huấn luyện viên. Được phép xem/sửa dữ liệu của học viên được gán.
*   **ADMIN:** Quản trị viên. Toàn quyền quản lý Template và Nội dung.

### 3. Gói dịch vụ (Tiers)
*   **BASIC:** Tính năng cơ bản, có quảng cáo/giới hạn.
*   **PREMIUM:** Full tính năng, không giới hạn.
*   **VIP:** Có thêm đặc quyền Coach 1-1.

---

## ⚠️ Xử lý lỗi (Error Handling)

API trả về lỗi theo định dạng chuẩn JSON (`GlobalExceptionHandler`).

**Ví dụ Response:**
```json
{
  "timestamp": "2024-12-12T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Không tìm thấy chương trình đang hoạt động",
  "path": "/v1/programs/active"
}
```

**Các mã lỗi phổ biến:**
*   `402 Payment Required`: Hết hạn dùng thử.
*   `409 Conflict`: Dữ liệu xung đột (ví dụ: đã có chương trình rồi mà tạo tiếp).
*   `403 Forbidden`: Không có quyền truy cập (ví dụ: xem bài của người khác).

---

## 🗄 Cơ sở dữ liệu

Schema `program` trên PostgreSQL:
*   `programs`: Bảng lõi.
*   `streaks`, `streak_breaks`: Lịch sử cai thuốc.
*   `quiz_templates`, `quiz_assignments`, `quiz_attempts`, `quiz_results`: Hệ thống bài kiểm tra.
*   `badges`, `user_badges`: Gamification.
*   `content_modules`: Nội dung bài học (Versioning).

---

## 🚀 Hướng dẫn cài đặt

### Yêu cầu
*   **JDK 25**
*   Docker

### Chạy Local
```bash
# 1. Khởi động DB
docker-compose up -d postgres

# 2. Build
./mvnw clean install

# 3. Run
./mvnw spring-boot:run
```

---
*© 2024 Smokefree Project.*
