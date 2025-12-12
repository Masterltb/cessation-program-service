# Program Service 🚀

> **Core Engine cho Nền tảng Cai thuốc lá Smokefree**
>
> Service này quản lý toàn bộ hành trình của người dùng: từ việc tạo lộ trình cá nhân hóa, lên lịch bài kiểm tra (quiz) cho đến theo dõi thói quen (streaks), cung cấp nội dung bài học và hệ thống gamification.

---

## 📋 Mục lục
- [Tổng quan](#-tổng-quan)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Logic Nghiệp vụ Cốt lõi](#-logic-nghiệp-vụ-cốt-lõi)
- [Hệ thống Gamification](#-hệ-thống-gamification-badges)
- [Cơ sở dữ liệu](#-cơ-sở-dữ-liệu)
- [Xác thực & Phân quyền](#-xác-thực--phân-quyền)
- [Xử lý lỗi](#-xử-lý-lỗi)
- [Hướng dẫn cài đặt (Local)](#-hướng-dẫn-cài-đặt-local)
- [Chi tiết Hạ tầng AWS (Production)](#-chi-tiết-hạ-tầng-aws-production)

---

## 📖 Tổng quan

**Program Service** (hay còn gọi là **Cessation Service** trong kiến trúc AWS) là một microservice quản lý trạng thái (state-managed), được thiết kế để hỗ trợ người dùng cai thuốc thông qua một lộ trình khoa học.

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
*   Tin tưởng Header: `X-User-Id` (được inject bởi API Gateway/Cognito).
*   Được xử lý bởi: `HeaderUserContextFilter`.

### 2. Vai trò (Roles - RBAC)
Hệ thống hỗ trợ các role:
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

## 🚀 Hướng dẫn cài đặt (Local)

### Yêu cầu
*   **JDK 25**
*   Docker & Docker Compose

### Chạy Local (Docker Compose)
Dùng cho phát triển cục bộ với DB PostgreSQL giả lập.

```bash
# 1. Khởi động DB
docker-compose up -d postgres

# 2. Build dự án
./mvnw clean install

# 3. Chạy Service
./mvnw spring-boot:run
```

---

## ☁️ Chi tiết Hạ tầng AWS 

Hệ thống được triển khai tại vùng **ap-southeast-1 (Singapore)** theo mô hình **Microservices kết hợp Serverless (Hybrid Architecture)**.

### 1. Phân vùng mạng & Bảo mật (Networking)
Toàn bộ hệ thống Backend nằm trong một **VPC (Virtual Private Cloud)** để đảm bảo an toàn.

*   **Backend Private Subnet (`192.0.0.0/18`):**
    *   Chứa các dịch vụ ứng dụng (EC2) và Bộ cân bằng tải nội bộ (NLB).
    *   **Không có** Public IP, không thể truy cập trực tiếp từ Internet.
    *   Chỉ nhận traffic từ API Gateway thông qua VPC Link.
*   **DB Private Subnet (`192.0.0.0/22`):**
    *   Chứa hệ thống Database.
    *   Được bảo vệ nghiêm ngặt nhất, chỉ nhận kết nối từ Backend Subnet.

### 2. Luồng truy cập (Access Flow)
Hệ thống sử dụng các dịch vụ quản lý (Managed Services) ở mép ngoài để xử lý traffic:

1.  **Frontend:** Người dùng truy cập qua **CloudFront** (CDN) lấy nội dung tĩnh từ **S3 Bucket**.
2.  **API Gateway:** Cổng giao tiếp duy nhất cho mọi request API.
3.  **Cognito:** Tích hợp với API Gateway để xác thực (AuthN) và phân quyền (AuthZ) trước khi request đi sâu vào hệ thống.

### 3. Kiến trúc Backend (Compute Layer)
Tại API Gateway, traffic được chia thành 2 nhánh:

*   **Nhánh 1: Serverless (Payment)**
    *   Sử dụng **AWS Lambda**.
    *   Mục đích: Xử lý thanh toán, tối ưu chi phí (chỉ trả tiền khi chạy) và khả năng scale đột biến.
*   **Nhánh 2: Microservices (Cessation Service)**
    *   **VPC Link & NLB:** API Gateway kết nối an toàn vào mạng riêng thông qua VPC Link, chuyển tiếp đến Network Load Balancer (NLB).
    *   **EC2 Instance:** Service này (`program-service`) chạy dưới dạng **Docker Container** trên các máy chủ ảo EC2 nằm trong Backend Subnet.

### 4. Tầng dữ liệu (Data Layer)
Hệ thống sử dụng mô hình **Self-managed Database** (Tự quản trị trên EC2) thay vì RDS để tối ưu kiểm soát.

*   **Cessation DB:** Chạy PostgreSQL trên EC2 Instance riêng biệt trong DB Subnet.
*   **User DB:** Chạy PostgreSQL trên EC2 khác.
*   **Social DB:** Chạy MongoDB (NoSQL) cho tính năng mạng xã hội.
*   **DB Backup:** Có service riêng chạy trên EC2 thực hiện sao lưu định kỳ.

### 5. Quản trị & DevOps
*   **CI/CD:** Sử dụng **GitLab** để quản lý mã nguồn và Pipeline tự động hóa.
*   **Container Registry:** Docker Image sau khi build được đẩy lên **AWS ECR**.
*   **Truy cập an toàn:** Quản trị viên (Operator) sử dụng **EC2 Instance Connect Endpoint** để SSH vào server trong mạng riêng mà không cần mở cổng 22 ra Internet công cộng.

---
*© 2024 Smokefree Project.*
