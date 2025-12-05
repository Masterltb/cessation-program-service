# Postman Test Guide - Program Service (v2.2 - with Badges)

**Last Updated:** 2025-12-04
**Focus:** End-to-End Flows, Optimization Verification, Badge System.

## 1. Environment Setup

Set these variables in your Postman Environment:

| Variable | Description | Value (Example) |
|----------|-------------|-----------------|
| `baseUrl` | Service URL | `http://localhost:8080` |
| `userId` | User UUID | `00000000-0000-0000-0000-000000000001` |
| `adminId` | Admin UUID | `00000000-0000-0000-0000-000000000000` |
| `programId` | Active Program ID | *(Auto-set by tests)* |
| `templateId` | Quiz Template ID | *(Auto-set by tests)* |
| `onboardingTemplateId` | Onboarding Quiz ID | *(Auto-set by tests)* |
| `weeklyQuizTemplateId` | Weekly Quiz ID | *(Auto-set by tests)* |
| `attemptId` | Quiz Attempt ID | *(Auto-set by tests)* |
| `planTemplateId` | Plan Template ID | *(Auto-set by tests)* |

### Authentication Headers (CRITICAL)
This service uses header-based authentication. **Every request** must include:

*   `X-User-Id`: `{{userId}}` (or `{{adminId}}` for admin routes)
*   `X-User-Group`: `CUSTOMER` (or `ADMIN`, `COACH`)
*   `X-User-Tier`: `BASIC` (or `PREMIUM`, `VIP`) - *Optional but recommended*

---

## 2. Core Flow: User Onboarding & Enrollment

### Step 1: Admin Setup - Create Quiz Templates & Plan (Pre-requisite)
*   **Purpose:** Prepare data for user onboarding.
*   **Actions:**
    1.  **Create Onboarding Quiz:** (If not already exists).
        *   **Method:** `POST`, **URL:** `{{baseUrl}}/v1/admin/quizzes`
        *   **Headers:** `X-User-Id: {{adminId}}`, `X-User-Group: ADMIN`
        *   **Body:** (Use a simple question set)
        *   **Save:** `id` from response as `{{onboardingTemplateId}}`.
    2.  **Create Weekly Check-in Quiz:** (For Quiz Badges)
        *   **Method:** `POST`, **URL:** `{{baseUrl}}/v1/admin/quizzes`
        *   **Headers:** `X-User-Id: {{adminId}}`, `X-User-Group: ADMIN`
        *   **Body:** (Use questions that can result in LOW, MODERATE, HIGH severity)
        *   **Save:** `id` from response as `{{weeklyQuizTemplateId}}`.
    3.  **Create Plan Template:** (If needed, or use existing one).
        *   **Method:** `POST`, **URL:** `{{baseUrl}}/v1/admin/plan-templates`
        *   **Headers:** `X-User-Id: {{adminId}}`, `X-User-Group: ADMIN`
        *   **Body:** (Define a 30-day plan with `planQuizSchedules` for `{{weeklyQuizTemplateId}}` at `startOffsetDay=7`, `everyDays=7`)
        *   **Save:** `id` from response as `{{planTemplateId}}`.

### Step 2: User Takes Baseline Quiz
*   **Method:** `POST`
*   **URL:** `{{baseUrl}}/api/onboarding/baseline`
*   **Headers:** `X-User-Id: {{userId}}`, `X-User-Group: CUSTOMER`
*   **Body:** (Use `{{onboardingTemplateId}}` and sample answers)
*   **Verify:** Response contains `recommendedTemplateId`. Save this as `{{planTemplateId}}`.

### Step 3: Start Program (Enrollment)
**Trigger Badge: Program Level 1 (Khởi Hành)**
*   **Method:** `POST`
*   **URL:** `{{baseUrl}}/v1/programs`
*   **Body:** (Use `{{planTemplateId}}` and `trial: true` or `false`)
*   **Verify:**
    *   Response Status: `200 OK`.
    *   Response Body: `status` is `ACTIVE`.
    *   **Badge Check:** Call `GET {{baseUrl}}/api/me/badges`. User should have `PROG_LV1` badge.

---

## 3. Core Flow: Quiz Execution & Quiz Badges

### Step 1: List Due Quizzes
*   **Method:** `GET`, **URL:** `{{baseUrl}}/v1/me/quizzes`
*   **Verify:** Ensure weekly quiz (template ID = `{{weeklyQuizTemplateId}}`) is due on appropriate days (e.g., day 7, 14, etc., of the program).

### Step 2: Open & Submit Weekly Quiz
**Trigger Badge: Quiz Level 1 (Tự Nhận Thức)**
*   **Method:** `POST`, **URL:** `{{baseUrl}}/v1/me/quizzes/{{weeklyQuizTemplateId}}/open`
*   **Save:** `attemptId` from response.
*   **Method:** `POST`, **URL:** `{{baseUrl}}/v1/me/quizzes/{{attemptId}}/submit`
*   **Verify:**
    *   Response shows `totalScore` and `severity`.
    *   **Badge Check:** Call `GET {{baseUrl}}/api/me/badges`. User should have `QUIZ_LV1` badge.

### Step 3: Submit 2nd & 3rd Weekly Quiz
**Trigger Badge: Quiz Level 2 (Tiến Triển Tốt) & Level 3 (Làm Chủ)**
*   **Scenario A: Improve/Maintain Performance (for QUIZ_LV2)**
    1.  Wait 7 days (simulate by manipulating program `currentDay` or `program.startDate` in DB for quick test).
    2.  List Due Quizzes, Open, Submit the 2nd weekly quiz. Ensure score is *lower or equal* than the 1st quiz.
    3.  **Badge Check:** Call `GET {{baseUrl}}/api/me/badges`. User should have `QUIZ_LV2` badge.
*   **Scenario B: Master Performance (for QUIZ_LV3)**
    1.  Continue taking quizzes until the "last quiz" (e.g., in the last week of the program).
    2.  Ensure this last quiz results in `Severity: LOW`.
    3.  **Badge Check:** Call `GET {{baseUrl}}/api/me/badges`. User should have `QUIZ_LV3` badge.

---

## 4. Core Flow: Program Milestone Badges

### Scenario 1: Earn Program Level 2 (Kiên Trì - Halfway)
*   **Trigger:** User viewing dashboard (`GET /api/me`) or checking progress (`GET /api/programs/{{programId}}/progress`).
*   **Action:** Simulate program progress.
    1.  Get `programId` from enrollment. Get `planDays` from program details.
    2.  Manually update `currentDay` in DB: `UPDATE program.programs SET current_day = <planDays/2> WHERE id = '{{programId}}';`
    3.  Call `GET {{baseUrl}}/api/me` or `GET {{baseUrl}}/api/programs/{{programId}}/progress`.
*   **Verify:** User has `PROG_LV2` badge.

### Scenario 2: Earn Program Level 3 (Về Đích - Completed)
*   **Trigger:** Program status becomes `COMPLETED`.
*   **Action:**
    1.  Manually update `currentDay` in DB to `program.planDays`.
    2.  Call `POST {{baseUrl}}/api/programs/{{programId}}/end` to explicitly complete the program.
*   **Verify:** User has `PROG_LV3` badge.

### Edge Case: Paused Program (NO Badges)
1.  Start a program. Verify `PROG_LV1` is awarded.
2.  Call `POST {{baseUrl}}/api/programs/{{programId}}/pause`.
3.  Manually update `currentDay` in DB to past `planDays/2`.
4.  Call `GET {{baseUrl}}/api/me`.
5.  **Verify:** User should *NOT* have `PROG_LV2` badge because `program.has_paused` is true.

---

## 5. Core Flow: Streak Badges

### Step 1: Initial Streak (Lazy Check)
*   **Action:** Ensure program has 0 `last_smoke_at` (new program).
*   **Action:** Call `GET {{baseUrl}}/api/me`.
*   **Verify:** In `streakInfo`, `daysWithoutSmoke` should be > 0. User should NOT have any Streak badges yet.

### Step 2: Earn Streak Level 1 (Tuần Lễ Vàng - 7 days)
*   **Action:** Simulate 7 days without smoking.
    1.  Manually update `program.start_date` in DB to 7 days ago.
    2.  Manually ensure `program.last_smoke_at` is NULL or very old.
    3.  Call `GET {{baseUrl}}/api/me`.
*   **Verify:** User has `STREAK_LV1` badge.

### Step 3: Earn Streak Level 2 (Thói Quen Mới - Half Program Days)
*   **Action:** Simulate `planDays / 2` days without smoking.
    1.  Manually update `program.start_date` in DB to `planDays/2` days ago.
    2.  Manually ensure `program.last_smoke_at` is NULL or very old.
    3.  Call `GET {{baseUrl}}/api/me`.
*   **Verify:** User has `STREAK_LV2` badge.

### Step 4: Earn Streak Level 3 (Chiến Binh Tự Do - Full Program Days)
*   **Action:** Simulate `planDays` days without smoking.
    1.  Manually update `program.start_date` in DB to `planDays` days ago.
    2.  Manually ensure `program.last_smoke_at` is NULL or very old.
    3.  Call `GET {{baseUrl}}/api/me`.
*   **Verify:** User has `STREAK_LV3` badge.

### Edge Case: Smoke Event (Break Streak)
1.  Earn `STREAK_LV1`.
2.  Log a smoke event: `POST {{baseUrl}}/api/programs/{{programId}}/smoke-events`.
3.  Call `GET {{baseUrl}}/api/me`.
4.  **Verify:** `daysWithoutSmoke` resets to 0. User *still has* `STREAK_LV1` (badges are permanent, not revoked).

---

## 6. Badge APIs

### Step 1: Get All Badge Definitions
*   **Method:** `GET`, **URL:** `{{baseUrl}}/api/me/badges/all`
*   **Verify:**
    *   Returns a Map grouped by category (PROGRAM, STREAK, QUIZ).
    *   Contains all 9 badge definitions.

### Step 2: Get My Earned Badges
*   **Method:** `GET`, **URL:** `{{baseUrl}}/api/me/badges`
*   **Verify:**
    *   Returns only the badges earned by `{{userId}}`.
    *   Each badge includes `code`, `name`, `iconUrl`, `earnedAt`.

---
