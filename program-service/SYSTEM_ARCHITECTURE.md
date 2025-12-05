# 📖 Kiến Trúc Hệ Thống Microservice - Smoke-Free Program

**Tài liệu này mô tả kiến trúc tổng thể, luồng dữ liệu và sự tương tác giữa các thành phần trong hệ thống microservice của dự án Smoke-Free, với trọng tâm là `Program Service`.**

## 1. Tổng Quan Kiến Trúc (High-Level Architecture)

Hệ thống được xây dựng theo kiến trúc microservice. Một **API Gateway** đóng vai trò là cửa ngõ duy nhất cho mọi yêu cầu từ client. Việc xác thực được xử lý bởi một **Authentication Service** (AWS Lambda), dịch vụ này sẽ làm giàu request bằng các header (`X-User-Id`, `X-User-Group`, `X-User-Tier`) trước khi chuyển tiếp đến các service nội bộ.

```mermaid
graph TD
    subgraph Client
        A[Browser / Mobile App]
    end

    subgraph "Hạ Tầng Backend"
        B(API Gateway)
        C(Authentication Service)
        D[<b>Program Service</b>]
        E(User Service)
        F(Payment Gateway)
        
        subgraph " "
            direction LR
            D1(Enrollment & Program)
            D2(Quiz & Onboarding)
            D3(Behavior Tracking: Smoke & Streak)
            D4(Content Management)
            D5(Coaching)
        end
        
        H[(PostgreSQL Database)]
    end

    A --> B
    B -- 1. Authenticate --> C
    B -- 2. Forward Enriched Request --> D
    
    D -- Calls for user/sub info --> E
    D -- Initiates payment flow --> F
    
    D -- Owns its schema --> H

    D1 -- internal --> D
    D2 -- internal --> D
    D3 -- internal --> D
    D4 -- internal --> D
    D5 -- internal --> D

    style D fill:#bbf,stroke:#333,stroke-width:2px
```

- **Authentication Service**: Xác thực JWT và tiêm thông tin người dùng vào request header.
- **Program Service**: **Dịch vụ cốt lõi và phức tạp nhất**, chịu trách nhiệm cho hầu hết các nghiệp vụ của ứng dụng. Nó được chia thành nhiều domain con logic:
    - `Enrollment & Program`: Quản lý vòng đời đăng ký và trạng thái chương trình.
    - `Quiz & Onboarding`: Quản lý các bài quiz, đánh giá đầu vào và đề xuất chương trình.
    - `Behavior Tracking`: Theo dõi các sự kiện hút thuốc (`SmokeEvent`) và chuỗi ngày thành công (`Streak`).
    - `Content Management`: Quản lý nội dung học tập với cơ chế phiên bản và đa ngôn ngữ.
    - `Coaching`: Quản lý việc gán ghép và tương tác giữa Coach và người dùng.
- **User Service**: (Dự kiến) Dịch vụ bên ngoài quản lý thông tin chi tiết của người dùng và trạng thái gói đăng ký (subscription). `Program Service` sẽ gọi đến đây khi cần.
- **Payment Gateway**: (Dự kiến) Dịch vụ bên ngoài xử lý thanh toán.

---

## 2. Phân Tích Sâu Các Domain trong Program Service

### 2.1. Domain: Enrollment & Program Management
- **Trách nhiệm**: Xử lý việc người dùng tham gia chương trình, quản lý trạng thái (active, paused), và tiến độ.
- **Controllers chính**:
    - `OnboardingFlowController`: Xử lý bài quiz đầu vào và đề xuất chương trình.
    - `ProgramJoinController`, `EnrollmentController`: Cung cấp các API để người dùng bắt đầu chương trình (trial/paid).
    - `ProgramManagementController`: Cung cấp các API để quản lý một chương trình đang diễn ra (pause, resume, end, upgrade).
- **Services chính**:
    - `OnboardingFlowService`: Điều phối luồng onboarding.
    - `EnrollmentService`: **Orchestrator Service**, điều phối việc tạo chương trình.
    - `ProgramCreationService`, `StepAssignmentService`: Các service chuyên biệt được `EnrollmentService` ủy quyền.
- **Kiến trúc đáng chú ý**:
    - Luồng onboarding và enrollment được thiết kế rất tốt, tuân thủ Single Responsibility Principle.
    - Tồn tại nhiều "thế hệ" API (`ProgramJoinController` vs `EnrollmentController`), cho thấy sự tiến hóa của hệ thống.
    - `ProgramManagementController` xử lý logic trực tiếp (Fat Controller), khác với các service khác.

### 2.2. Domain: Quiz
- **Trách nhiệm**: Quản lý toàn bộ vòng đời của các bài quiz, từ việc tạo, làm bài, chấm điểm đến xem lại lịch sử.
- **Controllers chính**:
    - `QuizDetailController`: Quản lý luồng làm quiz có trạng thái (lịch sử, xem chi tiết, làm lại).
    - `QuizController`: Cung cấp API stateless để tính điểm nhanh.
    - `CoachVipQuizController`: Xử lý nghiệp vụ đặc thù clone quiz cho user VIP.
- **Services chính**:
    - `QuizFlowService`: Quản lý luồng làm quiz **stateful** (có `QuizAttempt`), chống gian lận bằng cách chỉ cho phép 1 attempt `OPEN`.
    - `QuizService`: Cung cấp logic tính điểm **stateless**, không tương tác DB.
    - `SeverityRuleService`: Hoạt động như một "rule engine" để map điểm số ra mức độ và đề xuất.
    - `CoachVipQuizService`: Thực hiện logic "deep copy" phức tạp và được bảo vệ bởi `@Transactional`.
- **Kiến trúc đáng chú ý**:
    - Tách biệt rõ ràng giữa luồng stateful và stateless.
    - Logic phân quyền tùy chỉnh phức tạp (`@authz.isCoach`).
    - Tầng service (`QuizFlowService`) đã hoàn thiện nhưng chưa được kết nối đầy đủ từ `QuizDetailController`.

### 2.3. Domain: Behavior Tracking (Smoke & Streak)
- **Trách nhiệm**: Ghi nhận các hành vi liên quan đến việc hút thuốc và quản lý chuỗi ngày thành công. Đây là trái tim của việc theo dõi tiến trình cai thuốc.
- **Controllers chính**:
    - `SmokeEventController`: Ghi lại một sự kiện hút thuốc (Command).
    - `SmokeEventDetailController`: Lấy lịch sử và thống kê (Query).
    - `StreakController`: Quản lý toàn bộ vòng đời của `Streak` (start, break, history).
- **Services chính**:
    - `SmokeEventService`: Nhận sự kiện, cập nhật `Program`, và quan trọng nhất là **kích hoạt `StreakService`**.
    - `StreakService`: Chứa logic nghiệp vụ phức tạp để bắt đầu, phá vỡ, và tính toán chuỗi ngày.
- **Kiến trúc đáng chú ý**:
    - **Event-Driven Logic**: Một `SmokeEvent` được tạo ra sẽ kích hoạt các logic khác (cập nhật `Streak`).
    - **CQRS ở cấp Controller**: Tách biệt rõ `SmokeEventController` (ghi) và `SmokeEventDetailController` (đọc).
    - **Thiết kế dữ liệu sâu sắc**: Lưu lại cả `Streak` (thành công) và `StreakBreak` (thất bại) để phục vụ phân tích.

### 2.4. Domain: Content Management
- **Trách nhiệm**: Cung cấp và quản lý nội dung học tập.
- **Controllers chính**: `ModuleController`.
- **Services chính**: `ContentModuleService`.
- **Kiến trúc đáng chú ý**:
    - Hoạt động như một **CMS (Content Management System) mini**.
    - Hỗ trợ 2 tính năng cốt lõi: **Versioning** (tự động tăng phiên bản khi tạo mới) và **đa ngôn ngữ**.
    - API được thiết kế để tối ưu cho client với việc hỗ trợ ETag caching.

### 2.5. Domain: Coaching
- **Trách nhiệm**: Quản lý việc gán ghép và tương tác giữa Coach và người dùng.
- **Controllers chính**:
    - `CoachAssignmentController`: Quản lý việc gán/hủy gán Coach-Customer.
    - `ProgramCoachController`: Quản lý việc gán Coach vào một `Program` cụ thể.
- **Services chính**: `CoachAssignmentService` (hiện đang mock).
- **Kiến trúc đáng chú ý**:
    - Tồn tại **hai cách tiếp cận song song** cho cùng một nghiệp vụ, cho thấy sự thay đổi trong thiết kế.
    - `ProgramCoachController` thao tác trực tiếp trên `ProgramRepository`, trong khi `CoachAssignmentController` được thiết kế để dùng service riêng.
    - Luồng này chưa hoàn thiện và còn nhiều `//TODO`.

---

## 3. Phân Tích Các Luồng Tương Tác Quan Trọng

### 3.1. Luồng: Bắt đầu chương trình dùng thử (Start Trial)
*Mô tả: Luồng này hoàn toàn nằm trong `Program Service`. Nó kiểm tra điều kiện, tạo các bản ghi cần thiết trong database và trả về kết quả.*
```mermaid
sequenceDiagram
    participant Client
    participant API Gateway
    participant Program Service
    participant PostgreSQL DB

    Client->>API Gateway: POST /api/enrollments/start-trial
    API Gateway->>Program Service: Forward request
    
    Program Service->>Program Service: EnrollmentController.startTrial()
    Program Service->>Program Service: EnrollmentService.startTrialOrPaid()
    
    Program Service->>PostgreSQL DB: SELECT * FROM programs WHERE user_id = ? AND status = 'ACTIVE'
    alt User đã có chương trình ACTIVE
        PostgreSQL DB-->>Program Service: Trả về 1 program
        Program Service-->>API Gateway: 409 Conflict
        API Gateway-->>Client: 409 Conflict
    else User chưa có chương trình ACTIVE
        PostgreSQL DB-->>Program Service: Trả về rỗng
        Program Service->>Program Service: programCreationService.createTrialProgram()
        Program Service->>PostgreSQL DB: INSERT INTO programs (...)
        Program Service->>Program Service: stepAssignmentService.createForProgramFromTemplate()
        Program Service->>PostgreSQL DB: INSERT INTO step_assignments (...)
        
        Program Service-->>API Gateway: 201 Created (EnrollmentRes)
        API Gateway-->>Client: 201 Created
    end
```

### 3.2. Luồng: Người dùng làm một bài Quiz (Stateful Quiz Flow)
*Mô tả: Luồng này thể hiện quá trình làm một bài quiz có trạng thái, được quản lý bởi `QuizFlowService` để đảm bảo tính toàn vẹn và chống gian lận.*
```mermaid
sequenceDiagram
    participant Client
    participant API Gateway
    participant Program Service
    participant PostgreSQL DB

    Client->>API Gateway: POST /me/quiz/{templateId}/retry
    API Gateway->>Program Service: Forward request (đã xác thực)
    
    Program Service->>Program Service: QuizDetailController (gọi openAttempt)
    Program Service->>Program Service: QuizFlowService.openAttempt()
    Program Service->>PostgreSQL DB: SELECT 1 FROM quiz_attempts WHERE ... AND status = 'OPEN'
    alt Đã có attempt đang mở
        Program Service-->>API Gateway: 409 Conflict
        API Gateway-->>Client: 409 Conflict
    else Chưa có attempt đang mở
        Program Service->>PostgreSQL DB: INSERT INTO quiz_attempts (status='OPEN', ...)
        Program Service-->>API Gateway: 200 OK (OpenAttemptRes với câu hỏi)
        API Gateway-->>Client: 200 OK
    end

    Note over Client, PostgreSQL DB: Người dùng trả lời các câu hỏi...

    Client->>API Gateway: POST /me/quiz/{templateId}/attempts/{attemptId}/answers
    API Gateway->>Program Service: Forward request
    Program Service->>Program Service: QuizFlowService.saveAnswer()
    Program Service->>PostgreSQL DB: UPSERT vào quiz_answers

    Note over Client, PostgreSQL DB: Người dùng nộp bài...

    Client->>API Gateway: POST /me/quiz/{templateId}/attempts/{attemptId}/submit
    API Gateway->>Program Service: Forward request
    Program Service->>ProgramService: QuizFlowService.submit()
    Program Service->>PostgreSQL DB: UPDATE quiz_attempts SET status='SUBMITTED'
    Program Service->>PostgreSQL DB: INSERT INTO quiz_results (...)
    Program Service-->>API Gateway: 200 OK (SubmitRes với điểm số)
    API Gateway-->>Client: 200 OK
```

### 3.3. Luồng: Ghi nhận Sự kiện Hút thuốc (Event-Driven Streak Update)
*Mô tả: Luồng này thể hiện cách một hành động (ghi lại `SmokeEvent`) kích hoạt một logic nghiệp vụ quan trọng khác (cập nhật `Streak`).*
```mermaid
sequenceDiagram
    participant Client
    participant API Gateway
    participant Program Service
    participant PostgreSQL DB

    Client->>API Gateway: POST /api/programs/{id}/smoke-events (kind='SLIP')
    API Gateway->>Program Service: Forward request
    
    Program Service->>Program Service: SmokeEventController.create()
    Program Service->>Program Service: SmokeEventService.create()
    
    Program Service->>PostgreSQL DB: INSERT INTO smoke_events (...)
    Program Service->>PostgreSQL DB: UPDATE programs SET last_smoke_at = now()
    
    Note over Program Service: Logic xử lý Streak được kích hoạt
    Program Service->>Program Service: StreakService.breakStreak()
    
    Program Service->>PostgreSQL DB: SELECT * FROM streaks WHERE ended_at IS NULL
    Program Service->>PostgreSQL DB: UPDATE streaks SET ended_at = now(), length_days = ...
    Program Service->>PostgreSQL DB: INSERT INTO streak_breaks (...)
    
    Program Service-->>API Gateway: 201 Created (SmokeEventRes)
    API Gateway-->>Client: 201 Created
```

---

## 4. Mô Hình Dữ Liệu (PostgreSQL Schema)

Sơ đồ quan hệ thực thể (ERD) chi tiết của `Program Service`, bao gồm các domain chính.

```mermaid
erDiagram
    PROGRAM {
        UUID id PK
        UUID user_id "FK to User"
        UUID plan_template_id FK
        UUID coach_id FK "Nullable"
        varchar status
        date start_date
        int current_day
        int plan_days
        timestamp trial_end_expected
        timestamp last_smoke_at
        int streak_current
        int streak_best
    }

    PLAN_TEMPLATE {
        UUID id PK
        varchar code UK
        varchar name
        int total_days
    }

    PLAN_STEP {
        UUID template_id PK, FK
        int day_no PK
        time slot PK
        varchar title
        varchar module_code "FK to MODULE"
    }

    STEP_ASSIGNMENT {
        UUID id PK
        UUID program_id FK
        int step_no
        int planned_day
        varchar status "PENDING, COMPLETED, SKIPPED"
        timestamp scheduled_at
        timestamp completed_at
    }

    MODULE {
        UUID id PK
        varchar code UK
        varchar lang UK
        int version UK
        varchar type
        jsonb payload
    }

    COACH_ASSIGNMENT {
        UUID id PK
        UUID coach_id "FK to User"
        UUID customer_id "FK to User"
        timestamp assigned_at
    }

    %% --- Quiz Sub-domain ---
    QUIZ_TEMPLATE {
        UUID id PK
        varchar name
        varchar scope "GLOBAL, COACH"
        UUID owner_id "FK to User (coach)"
    }

    QUIZ_TEMPLATE_QUESTION {
        UUID template_id PK, FK
        int question_no PK
        varchar question_text
    }

    QUIZ_ASSIGNMENT {
        UUID id PK
        UUID program_id FK
        UUID template_id FK
        varchar origin "COACH_CUSTOM, SYSTEM"
        timestamp expires_at
    }

    QUIZ_ATTEMPT {
        UUID id PK
        UUID user_id FK
        UUID template_id FK
        varchar status "OPEN, SUBMITTED"
    }

    QUIZ_ANSWER {
        UUID attempt_id PK, FK
        int question_no PK
        int answer
    }

    QUIZ_RESULT {
        UUID id PK
        UUID attempt_id FK
        int total_score
        varchar severity
    }

    %% --- Behavior Tracking Sub-domain ---
    SMOKE_EVENT {
        UUID id PK
        UUID program_id FK
        varchar kind "SLIP, RELAPSE, NO_SMOKE"
        timestamp occurred_at
    }

    STREAK {
        UUID id PK
        UUID program_id FK
        timestamp started_at
        timestamp ended_at "NULL nếu đang mở"
        int length_days
    }

    STREAK_BREAK {
        UUID id PK
        UUID streak_id FK
        UUID smoke_event_id FK "Nullable"
        timestamp broken_at
        int prev_streak_days
    }

    %% --- Relationships ---
    PROGRAM }o--|| PLAN_TEMPLATE : "dựa trên"
    PROGRAM ||--o{ STEP_ASSIGNMENT : "có"
    PROGRAM ||--o{ QUIZ_ASSIGNMENT : "có"
    PROGRAM ||--o{ SMOKE_EVENT : "có"
    PROGRAM ||--o{ STREAK : "có"
    
    PLAN_TEMPLATE ||--o{ PLAN_STEP : "định nghĩa"
    STEP_ASSIGNMENT }o..|| PLAN_STEP : "sao chép từ"
    
    QUIZ_ASSIGNMENT }o--|| QUIZ_TEMPLATE : "gán"
    QUIZ_ATTEMPT }o--|| QUIZ_TEMPLATE : "của"
    QUIZ_ATTEMPT ||--o{ QUIZ_ANSWER : "có"
    QUIZ_ATTEMPT ||--o{ QUIZ_RESULT : "có"

    STREAK ||--o{ STREAK_BREAK : "bị phá vỡ bởi"
    STREAK_BREAK }o..|| SMOKE_EVENT : "gây ra bởi"
```
