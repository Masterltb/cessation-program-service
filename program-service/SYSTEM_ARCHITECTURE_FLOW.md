# 🔄 SYSTEM ARCHITECTURE FLOW (Luồng Xử Lý Chi Tiết)

Tài liệu này mô tả chi tiết các luồng tương tác (Sequence Diagrams) cho 3 nghiệp vụ quan trọng và phức tạp nhất của hệ thống.

## 1. Luồng Khởi Tạo Chương Trình (Program Creation & Auto-Assign)
*Khi người dùng đăng ký, hệ thống không chỉ tạo một bản ghi `Program` mà còn phải sinh ra hàng loạt dữ liệu liên quan (Steps, Quizzes).*

```mermaid
sequenceDiagram
    actor User
    participant Ctl as ProgramController
    participant ES as EnrollmentService
    participant PS as ProgramService
    participant SAS as StepAssignmentService
    participant QAS as QuizAssignmentRepo
    participant DB as PostgreSQL

    User->>Ctl: POST /v1/programs (Create)
    Ctl->>ES: createProgram(userId, req)
    
    rect rgb(240, 248, 255)
        note right of ES: Transaction Start
        ES->>PS: Create & Save Base Program
        PS->>DB: INSERT INTO programs
        
        ES->>SAS: createForProgramFromTemplate()
        note right of SAS: Sinh bài học hàng ngày (Daily Steps)
        SAS->>DB: Bulk INSERT step_assignments
        
        ES->>PS: assignSystemQuizzes(program)
        note right of PS: Đọc lịch quiz và gán tự động
        PS->>DB: SELECT * FROM plan_quiz_schedules
        PS->>DB: Bulk INSERT quiz_assignments
        note right of ES: Transaction Commit
    end
    
    ES-->>Ctl: ProgramRes
    Ctl-->>User: 201 Created
```

---

## 2. Luồng "Cứu Chuỗi" (Streak Recovery Flow) - **Killer Feature**
*Đây là logic phức tạp nhất, nơi `Quiz Engine` tương tác trực tiếp với `Streak Engine` để sửa đổi lịch sử.*

```mermaid
sequenceDiagram
    actor User
    participant QC as MeQuizController
    participant QFS as QuizFlowService
    participant SS as StreakService
    participant DB as PostgreSQL

    User->>QC: POST /submit (Recovery Quiz)
    QC->>QFS: submit(attemptId)
    
    QFS->>QFS: Calculate Score & Severity
    QFS->>DB: Save QuizResult
    
    alt Quiz Origin is STREAK_RECOVERY
        QFS->>DB: Find Latest StreakBreak
        QFS->>SS: restoreStreak(breakId)
        
        rect rgb(255, 240, 240)
            note right of SS: Logic "Vá" Lỗi Lầm
            SS->>DB: Find Historical Streak (Ended)
            SS->>DB: UPDATE streaks SET ended_at = NULL
            SS->>DB: Recalculate Streak Length
            SS->>DB: Update Program Cache (streak_current)
        end
    end
    
    QFS-->>QC: SubmitRes (Success)
    QC-->>User: 200 OK (Streak Restored!)
```

---

## 3. Luồng Kiểm Tra & Chặn Dùng Thử (Trial Hard Stop)
*Cơ chế bảo vệ thụ động (Passive Protection) để ngăn người dùng xài chùa.*

```mermaid
sequenceDiagram
    actor User
    participant Ctl as AnyController
    participant PS as ProgramService
    participant Logic as BusinessLogic

    User->>Ctl: GET /api/me/quizzes (hoặc bất kỳ API nào)
    Ctl->>PS: getActive(userId)
    
    PS->>PS: Load Program from DB
    
    alt Trial Expired (Now > trialEndExpected)
        PS--XCtl: Throw SubscriptionRequiredException
        Ctl--XUser: 402 Payment Required
        note right of User: Bị chặn ngay lập tức. Không có dữ liệu trả về.
    else Active
        PS->>Logic: Return Program
        Logic->>Ctl: Process Request
        Ctl-->>User: 200 OK (Data)
    end
```