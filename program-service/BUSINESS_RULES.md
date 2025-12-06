# Smoking Cessation Program - Business Rules

## 🎯 Tổng quan

**Program Service** quản lý chương trình cai thuốc lá theo lộ trình có cấu trúc, với gamification và hỗ trợ hành vi.

---

## 📋 Core Business Flows

### FLOW 1: ONBOARDING (Đánh giá & Đăng ký)

#### 1.1. Baseline Assessment Quiz

**Mục đích:** Đánh giá mức độ nghiện thuốc lá của người dùng

**Process:**
```
1. User clicks "Bắt đầu cai thuốc"
2. System shows 10-question quiz (Fagerström Test)
3. User answers questions about smoking habits:
   - Thời gian hút điếu đầu tiên sau khi thức dậy
   - Số điếu thuốc/ngày
   - Khó khăn khi không được hút
   - ...
4. System tính điểm (0-10)
5. System phân loại mức độ nghiện:
   - 0-2: Very Low Dependence
   - 3-4: Low Dependence
   - 5-6: Medium Dependence
   - 7-8: High Dependence
   - 9-10: Very High Dependence
```

**Business Rules:**
- ✅ Quiz chỉ làm 1 lần khi onboarding
- ✅ Mỗi câu hỏi có trọng số khác nhau (0-3 points)
- ✅ Không thể skip quiz
- ✅ Không thể sửa đáp án sau khi submit

**Endpoint:** `GET /api/onboarding/baseline/quiz`

#### 1.2. Plan Recommendation

**Mục đích:** Gợi ý lộ trình phù hợp với mức độ nghiện

**Business Logic:**
```java
if (baselineScore <= 4) {
    recommendedPlan = "PLAN_30_DAYS"; // Giảm dần nhẹ
} else if (baselineScore <= 7) {
    recommendedPlan = "PLAN_45_DAYS"; // Giảm dần trung bình
} else {
    recommendedPlan = "PLAN_60_DAYS"; // Giảm dần mạnh
}
```

**Plan Templates:**
- **30 days:** Cho người ít nghiện, giảm 20-30% mỗi tuần
- **45 days:** Cho người nghiện trung bình, giảm 15-20% mỗi tuần
- **60 days:** Cho người nghiện nặng, giảm 10-15% mỗi tuần

**Endpoint:** `POST /api/onboarding/baseline`

**Response:**
```json
{
  "baselineScore": 8,
  "addictionLevel": "HIGH",
  "recommendedTemplateId": "uuid-plan-60",
  "recommendationReason": "Mức độ phụ thuộc cao, cần lộ trình giảm dần từ từ..."
}
```

---

### FLOW 2: ENROLLMENT (Tạo chương trình)

#### 2.1. Create Program

**Mục đích:** User chính thức đăng ký tham gia lộ trình

**Business Rules:**
- ✅ 1 user chỉ có 1 active program tại 1 thời điểm
- ✅ Nếu đã có active program → Reject (409 Conflict)
- ✅ Trial mode: Cho phép dùng thử 7 ngày miễn phí
- ✅ Sau trial → Phải upgrade hoặc program tự động pause

**Endpoint:** `POST /v1/programs`

**Request:**
```json
{
  "planTemplateId": "uuid-plan-60",
  "trial": true,
  "coachId": null
}
```

**Side Effects:**
1. Tạo Program với status = `ACTIVE`
2. Generate tất cả steps (daily tasks) cho 60 ngày
3. Tạo initial streak = 0
4. Unlock badge **"Khởi Hành"** (PROG_LV1)
5. Tính `trialEndDate` = today + 7 days

**Response:**
```json
{
  "id": "uuid-program",
  "userId": "uuid-user",
  "planTemplateId": "uuid-plan-60",
  "status": "ACTIVE",
  "startedAt": "2025-12-06",
  "trialEndDate": "2025-12-13",
  "isTrial": true
}
```

---

### FLOW 3: DASHBOARD (Home Screen)

#### 3.1. GET /api/me - Unified Dashboard Data

**Mục đích:** 1 API trả về TẤT CẢ dữ liệu cần thiết cho home screen

**Business Logic:**
```
IF user HAS active program:
  RETURN:
    - Current program info
    - Current streak
    - Today's tasks (PENDING/COMPLETED)
    - Due quizzes (chưa làm)
    - Earned badges
    - Program progress %
ELSE:
  RETURN:
    - badges: []
    - hasActiveProgram: false
    - redirectTo: "/onboarding"
```

**Response Structure:**
```json
{
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "name": "Nguyễn Văn A"
  },
  "currentProgram": {
    "id": "uuid-program",
    "status": "ACTIVE",
    "currentDay": 15,
    "totalDays": 60,
    "progressPercent": 25.0,
    "targetCigarettes": 10,
    "actualCigarettes": 12,
    "isTrial": false
  },
  "currentStreak": {
    "currentStreak": 7,
    "longestStreak": 14,
    "lastSmokeDate": "2025-12-05"
  },
  "todayTasks": [
    {
      "id": "uuid-step1",
      "dayNumber": 15,
      "title": "Đọc bài học: Triggers",
      "status": "COMPLETED",
      "points": 10
    },
    {
      "id": "uuid-step2",
      "dayNumber": 15,
      "title": "Bài tập thở sâu",
      "status": "PENDING",
      "points": 5
    }
  ],
  "dueQuizzes": [
    {
      "templateId": "uuid-quiz1",
      "title": "Đánh giá tuần 2",
      "dueDate": "2025-12-07"
    }
  ],
  "badges": [
    {
      "badgeCode": "PROG_LV1",
      "badgeName": "Khởi Hành",
      "earnedAt": "2025-12-01"
    },
    {
      "badgeCode": "STREAK_7",
      "badgeName": "7 ngày không hút",
      "earnedAt": "2025-12-06"
    }
  ]
}
```

**Caching:** Redis cache 30s (refresh khi có thay đổi)

---

### FLOW 4: DAILY TASKS (Hoạt động hàng ngày)

#### 4.1. Get Today's Tasks

**Mục đích:** Hiển thị danh sách task của ngày hôm nay

**Business Rules:**
- ✅ Mỗi ngày có 2-4 tasks (step assignments)
- ✅ Tasks được gen sẵn khi tạo program
- ✅ Chỉ hiển thị tasks của ngày UTC hiện tại
- ✅ Tasks có deadline: 23:59:59 UTC của ngày đó

**Task Types:**
1. **READING**: Đọc bài học (Module nội dung)
2. **QUIZ**: Làm bài quiz (đánh giá kiến thức)
3. **EXERCISE**: Bài tập thực hành (thở sâu, meditation)
4. **REFLECTION**: Nhật ký suy ngẫm

**Endpoint:** `GET /api/programs/{id}/steps/today`

#### 4.2. Update Task Status

**Mục đích:** User đánh dấu task là hoàn thành

**Business Logic:**
```
IF task status changed to COMPLETED:
  1. Add points to user
  2. Check if unlocked new badge
  3. Update program progress
  4. Check if all today tasks completed → Earn daily badge
```

**Status Flow:**
```
PENDING → COMPLETED (user hoàn thành)
PENDING → SKIPPED (user bỏ qua)
PENDING → MISSED (quá deadline chưa làm)
```

**Endpoint:** `PUT /api/programs/{id}/steps/{stepId}/status`

**Points System:**
- READING: 10 points
- QUIZ: 20 points (nếu pass)
- EXERCISE: 5 points
- REFLECTION: 5 points

---

### FLOW 5: QUIZ ENGINE (Bài kiểm tra)

#### 5.1. Open Quiz

**Mục đích:** Bắt đầu làm quiz, tạo attempt

**Business Rules:**
- ✅ Mỗi quiz có thể làm lại nhiều lần (retry)
- ✅ Mỗi lần làm tạo 1 QuizAttempt mới
- ✅ Lưu best score (điểm cao nhất)
- ✅ Quiz có deadline (nếu là required quiz trong program)

**Endpoint:** `POST /v1/me/quizzes/{templateId}/open`

**Response:**
```json
{
  "attemptId": "uuid-attempt",
  "templateId": "uuid-template",
  "questions": [
    {
      "questionNo": 1,
      "questionText": "Khói thuốc lá chứa bao nhiêu chất độc hại?",
      "choices": {
        "A": "Hơn 70",
        "B": "Hơn 700",
        "C": "Hơn 7000"
      }
    }
  ]
}
```

#### 5.2. Save Answer

**Mục đích:** Lưu từng câu trả lời (không tính điểm ngay)

**Endpoint:** `PUT /v1/me/quizzes/{attemptId}/answer`

**Request:**
```json
{
  "questionNo": 1,
  "selectedChoice": "C",
  "score": 3
}
```

#### 5.3. Submit Quiz

**Mục đích:** Hoàn thành quiz, tính điểm cuối cùng

**Business Logic:**
```
1. Validate tất cả câu hỏi đã được trả lời
2. Tính tổng điểm
3. Xác định PASS/FAIL (threshold 60%)
4. Cập nhật best score
5. Unlock badge nếu đạt milestone
6. Return kết quả
```

**Endpoint:** `POST /v1/me/quizzes/{attemptId}/submit`

**Response:**
```json
{
  "attemptId": "uuid-attempt",
  "totalScore": 85,
  "maxScore": 100,
  "passed": true,
  "correctAnswers": 17,
  "totalQuestions": 20,
  "timeTaken": "00:05:32",
  "rank": "EXCELLENT"
}
```

---

### FLOW 6: SMOKE EVENTS & STREAK (Theo dõi hút thuốc)

#### 6.1. Report Smoke Event

**Mục đích:** User báo cáo khi hút thuốc (relapse)

**Business Rules:**
- ✅ Khi report → Break streak (reset về 0)
- ✅ Lưu timestamp, số điếu, tâm trạng, triggers
- ✅ Cập nhật thống kê
- ✅ Gửi notification động viên

**Endpoint:** `POST /api/programs/{id}/smoke-events`

**Request:**
```json
{
  "occurredAt": "2025-12-06T14:30:00Z",
  "cigaretteCount": 2,
  "mood": "STRESSED",
  "triggers": ["WORK_PRESSURE", "COFFEE"],
  "note": "Áp lực công việc quá lớn"
}
```

**Side Effects:**
1. Break current streak → 0
2. Save to smoke_events table
3. Update program.actualCigarettes
4. Send encouragement notification
5. Calculate next target

#### 6.2. Get Smoke History

**Mục đích:** Xem lịch sử các lần hút thuốc

**Endpoint:** `GET /api/programs/{id}/smoke-events/history?size=20`

**Response:**
```json
[
  {
    "id": "uuid-event1",
    "occurredAt": "2025-12-06T14:30:00Z",
    "cigaretteCount": 2,
    "mood": "STRESSED",
    "triggers": ["WORK_PRESSURE"]
  }
]
```

#### 6.3. Get Statistics

**Mục đích:** Thống kê theo thời gian (ngày/tuần/tháng)

**Endpoint:** `GET /api/programs/{id}/smoke-events/stats?period=WEEK`

**Response:**
```json
{
  "period": "WEEK",
  "totalEvents": 3,
  "totalCigarettes": 8,
  "averagePerDay": 1.14,
  "comparedToTarget": "80% UNDER TARGET",
  "mostCommonTrigger": "STRESS",
  "mostCommonTime": "14:00-16:00"
}
```

#### 6.4. Get Current Streak

**Mục đích:** Lấy streak hiện tại (số ngày liên tục không hút)

**Endpoint:** `GET /api/programs/{id}/streak`

**Business Logic:**
```
currentStreak = days_since(lastSmokeEvent)

IF currentStreak >= 7:
  unlock STREAK_7 badge
IF currentStreak >= 30:
  unlock STREAK_30 badge
IF currentStreak >= 90:
  unlock STREAK_90 badge
```

---

### FLOW 7: PROGRAM MANAGEMENT (Quản lý chương trình)

#### 7.1. Pause Program

**Mục đích:** Tạm dừng chương trình (ví dụ: đi du lịch, bận việc)

**Business Rules:**
- ✅ Chỉ pause khi status = ACTIVE
- ✅ Khi pause → Không tính streak, không tạo tasks mới
- ✅ Có thể pause tối đa 14 ngày
- ✅ Sau 14 ngày tự động end program

**Endpoint:** `POST /api/programs/{id}/pause`

**Request:**
```json
{
  "reason": "Đi du lịch 1 tuần",
  "expectedResumeDate": "2025-12-15"
}
```

#### 7.2. Resume Program

**Mục đích:** Tiếp tục chương trình sau khi pause

**Business Rules:**
- ✅ Chỉ resume khi status = PAUSED
- ✅ Không tính ngày pause vào tổng tiến độ
- ✅ Tasks bị miss khi pause → Đánh dấu MISSED

**Endpoint:** `POST /api/programs/{id}/resume`

#### 7.3. End Program

**Mục đích:** Kết thúc chương trình sớm (user muốn dừng)

**Business Rules:**
- ✅ Có thể end bất cứ lúc nào
- ✅ Lưu progress đã đạt được
- ✅ Badges đã unlock vẫn giữ
- ✅ Có thể tạo program mới sau 7 ngày

**Endpoint:** `POST /api/programs/{id}/end`

**Request:**
```json
{
  "reason": "COMPLETED_GOAL",
  "feedback": "Đã cai được 30 ngày, cảm thấy đủ"
}
```

#### 7.4. Check Trial Status

**Mục đích:** Kiểm tra tình trạng trial, còn bao nhiêu ngày

**Endpoint:** `GET /api/programs/{id}/trial-status`

**Response:**
```json
{
  "isTrial": true,
  "trialStartDate": "2025-12-01",
  "trialEndDate": "2025-12-08",
  "daysRemaining": 2,
  "canUpgrade": true,
  "upgradeCTA": "Nâng cấp ngay để tiếp tục"
}
```

---

## 🎮 Gamification System

### Badges (Huy hiệu)

**Badge Types:**
1. **Progress Badges:** PROG_LV1, PROG_LV2, PROG_LV3
   - Unlock khi hoàn thành % chương trình
2. **Streak Badges:** STREAK_7, STREAK_30, STREAK_90
   - Unlock khi đạt streak liên tục
3. **Quiz Badges:** QUIZ_MASTER, QUIZ_PERFECT
   - Unlock khi đạt điểm cao quiz
4. **Milestone Badges:** HALF_WAY, FINISH_LINE
   - Unlock khi đạt mốc đặc biệt

**Rarity Levels:**
- COMMON: Dễ đạt (80% users)
- RARE: Trung bình (50% users)
- EPIC: Khó (20% users)
- LEGENDARY: Rất khó (5% users)

### Points System

**How to earn points:**
- Complete daily task: 5-20 points
- Pass quiz: 20-50 points
- Maintain streak 7 days: 100 points
- Complete program: 500 points

**Points usage:**
- Unlock premium content
- Customize avatar
- Compete in leaderboard

---

## 🔒 Authorization Rules

### Role-Based Access

```
CUSTOMER (người dùng thường):
  - Tạo program cho chính mình
  - Xem/cập nhật tasks của chính mình
  - Làm quiz
  - Report smoke events

COACH (huấn luyện viên):
  - Xem progress của customers được assign
  - Gửi encouragement messages
  - Customize tasks cho customer

ADMIN (quản trị viên):
  - CRUD quiz templates
  - CRUD plan templates
  - Xem thống kê toàn hệ thống
  - Manage coaches
```

### Program Ownership

```java
@PreAuthorize("@programSecurity.isOwner(#programId)")
public void updateProgram(UUID programId) {
  // Chỉ owner mới được update
}
```

---

## 📊 Data Retention

### Active Programs
- Giữ vô thời hạn trong khi `status = ACTIVE`

### Ended Programs
- Giữ 1 năm để thống kê
- Sau 1 năm → Archive (soft delete)

### Smoke Events
- Giữ vô thời hạn (quan trọng cho phân tích)

### Quiz Attempts
- Giữ best attempt vô thời hạn
- Old attempts → Delete sau 6 tháng

---

## 🔔 Notification Rules

### Daily Reminders
- 08:00 UTC: "Bắt đầu ngày mới, hãy kiểm tra tasks!"
- 20:00 UTC: "Đừng quên hoàn thành tasks hôm nay"

### Streak Alerts
- Lost streak: "Không sao, hãy bắt đầu lại!"
- 7-day streak: "Tuyệt vời! Bạn đã 7 ngày không hút"

### Trial Expiring
- 3 days before: "Dùng thử còn 3 ngày, nâng cấp ngay!"
- 1 day before: "Chương trình sẽ tạm dừng ngày mai, nâng cấp để tiếp tục"

---

## ⚠️ Edge Cases

### 1. User tạo program trong khi đã có active program
**Rule:** Reject với 409 Conflict
**Message:** "Bạn đã có chương trình đang chạy. Hãy kết thúc trước khi tạo mới."

### 2. User report smoke event sau khi program ended
**Rule:** Reject với 400 Bad Request
**Message:** "Chương trình đã kết thúc, không thể báo cáo smoke event"

### 3. Trial expired nhưng user chưa upgrade
**Rule:** Auto pause program
**Message:** "Dùng thử đã hết, vui lòng nâng cấp để tiếp tục"

### 4. User làm quiz nhiều lần
**Rule:** Allow, lưu best score
**Logic:** Encourage learning, không penalize retry

### 5. Timezone issues
**Rule:** Tất cả timestamp lưu UTC
**Logic:** Frontend convert sang local timezone

---

**Version:** 1.0
**Last Updated:** 2025-12-06
**Author:** Program Service Team
