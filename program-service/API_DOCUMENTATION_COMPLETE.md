# API Technical Documentation - Program Service (Frontend Integration)

**Version:** 2.2 (Badge System & Optimization Update)
**Last Updated:** 2025-12-04
**Status:** Ready for Integration

---

## 1. General Conventions

### 1.1. Environment & Base URL
*   **Dev:** `http://localhost:8080`
*   **Prod:** `https://api.smokefree.app/program`

### 1.2. Authentication Headers
All requests (except `Health Check`) **MUST** include the following headers to identify the user context:

| Header | Required | Description | Example |
| :--- | :---: | :--- | :--- |
| `Authorization` | ✅ | JWT Token (from Auth Service) | `Bearer eyJhbGci...` |
| `X-User-Id` | ✅ | UUID of the logged-in user | `a1b2c3d4-e5f6-...` |
| `X-User-Group` | ✅ | `CUSTOMER`, `COACH`, `ADMIN` | `CUSTOMER` |
| `X-User-Tier` | ⬜ | `BASIC`, `PREMIUM`, `VIP` (Default: BASIC) | `PREMIUM` |

### 1.3. Timezone Handling
*   **Server:** Always returns and accepts time in **ISO-8601 UTC** format (e.g., `2025-12-04T10:30:00Z`).
*   **Frontend:** Responsible for converting UTC to the user's Local Time for display.
*   **Streak Note:** The number of `daysWithoutSmoke` is calculated based on UTC at the server side. Frontend should **NOT** recalculate this based on local time to avoid data inconsistency.

---

## 2. Dashboard & Overview (Home Screen)

### 2.1. Get Aggregated Dashboard
This API is designed as an "Aggregator", returning all necessary data to render the Home screen in a single request.

*   **Method:** `GET`
*   **Path:** `/api/me`
*   **Response Structure:**

```json
{
  "userId": "uuid",
  "subscription": {
    "tier": "BASIC",
    "status": "ACTIVE",
    "expiresAt": null
  },
  "activeProgram": {
    "id": "uuid",
    "templateCode": "PLAN_30_DAYS",
    "templateName": "30-Day Basic Plan",
    "status": "ACTIVE", // ACTIVE, PAUSED, COMPLETED
    "currentDay": 5,
    "planDays": 30,
    "isTrial": true,
    "trialRemainingDays": 2, // Null if Paid user
    "createdAt": "2025-12-01T10:00:00Z"
  },
  "dueQuizzes": [ // List of quizzes that require immediate attention
    {
      "templateId": "uuid",
      "templateName": "Weekly Check-in 1",
      "dueAt": "2025-12-04T00:00:00Z",
      "isOverdue": false
    }
  ],
  "streakInfo": {
    "currentStreak": 5,       // Current consecutive streak days
    "longestStreak": 5,       // Personal best record
    "daysWithoutSmoke": 5     // Total smoke-free days (UTC based)
  }
}
```

*   **Frontend Logic:**
    *   If `activeProgram` is `null`: Show "Start Journey" (Onboarding) screen.
    *   If `activeProgram.status` is `PAUSED`: Show a warning banner with a "Resume" button.
    *   If `trialRemainingDays <= 0` and `isTrial=true`: Show a mandatory Payment Modal (Hard Stop).
    *   Check `dueQuizzes`: If the array is not empty, show a red dot notification or a reminder popup.

---

## 3. Badge System - **NEW**

### 3.1. Get My Earned Badges
Retrieves the list of badges the user has earned. Should be called when the user visits the Profile or Dashboard (Lazy load).

*   **Method:** `GET`
*   **Path:** `/api/me/badges`
*   **Response:** Array of earned badges.

```json
[
  {
    "code": "PROG_LV1",
    "category": "PROGRAM",
    "level": 1,
    "name": "Start Journey",
    "description": "Started the smoke-free journey.",
    "iconUrl": "assets/badges/prog_lv1.png",
    "earnedAt": "2025-12-01T10:00:00Z"
  },
  {
    "code": "STREAK_LV1",
    "category": "STREAK",
    "level": 1,
    "name": "Golden Week",
    "description": "Achieved a 7-day smoke-free streak.",
    "iconUrl": "assets/badges/streak_lv1.png",
    "earnedAt": "2025-12-08T10:00:00Z"
  }
]
```

### 3.2. Get All Badge Definitions
Used to display the "Collection" view (unearned badges should be greyed out).

*   **Method:** `GET`
*   **Path:** `/api/me/badges/all`
*   **Response:** Map grouped by Category (`PROGRAM`, `STREAK`, `QUIZ`).

```json
{
  "PROGRAM": [
    { "code": "PROG_LV1", "level": 1, "name": "Start Journey", ... },
    { "code": "PROG_LV2", "level": 2, "name": "Perseverance", ... }
  ],
  "STREAK": [...]
}
```

*   **UI Rendering Rules:**
    *   Compare `all` list with `earned` list.
    *   If the user does NOT have the badge -> Render Grayscale Icon + Lock overlay.
    *   If the user HAS the badge -> Render Color Icon.

---

## 4. Program Management (Lifecycle)

### 4.1. Create Program (Enrollment)
*   **Method:** `POST`
*   **Path:** `/v1/programs`
*   **Body:**
    ```json
    {
      "planTemplateId": "uuid-from-onboarding",
      "trial": true // true: 7-day trial
    }
    ```
*   **Impact:**
    *   Automatically awards **Start Journey (PROG_LV1)** badge.
    *   System automatically assigns daily Steps and Quizzes schedules.

### 4.2. Pause Program
*   **Method:** `POST`
*   **Path:** `/api/programs/{id}/pause`
*   **User Warning:** "Pausing your journey will **disqualify you from earning 'Perseverance' and 'Finisher' badges**. Are you sure?"
    *   *Backend Logic:* The `hasPaused` flag is permanently set to `true` for this program.

### 4.3. Resume Program
*   **Method:** `POST`
*   **Path:** `/api/programs/{id}/resume`

### 4.4. End Program (Early Finish)
*   **Method:** `POST`
*   **Path:** `/api/programs/{id}/end`
*   **Impact:**
    *   If `hasPaused == false`: Awards **Finisher (PROG_LV3)** badge.
    *   Status changes to `COMPLETED`.

---

## 5. Steps & Quiz Engine

### 5.1. List Due Quizzes
*   **Method:** `GET`
*   **Path:** `/v1/me/quizzes`
*   **Response:** List of quizzes currently open (`isOverdue` calculated based on deadline).

### 5.2. Quiz Execution Flow
1.  **Open Attempt:** `POST /v1/me/quizzes/{templateId}/open` -> Returns `attemptId`.
2.  **Answer (Optional - Auto save):** `PUT /v1/me/quizzes/{attemptId}/answer`.
3.  **Submit:** `POST /v1/me/quizzes/{attemptId}/submit`.

*   **Post-Submit Impact:**
    *   System calculates score and severity.
    *   Checks for **Quiz Progress Badges** (Level 1, 2, 3) immediately. If earned, Badge API will reflect this on next call.

---

## 6. Tracking (Smoke & Streak)

### 6.1. Log Smoke Event (Slip/Relapse)
*   **Method:** `POST`
*   **Path:** `/api/programs/{id}/smoke-events`
*   **Body:**
    ```json
    {
      "eventType": "SMOKE",
      "eventAt": "2025-12-04T10:00:00Z"
    }
    ```
*   **Consequence:**
    *   `currentStreak` resets to 0.
    *   User retains previously earned Streak Badges (Badges are permanent).

---

## 7. Common Error Codes

| HTTP Code | Frontend Handling | Suggested Action |
| :--- | :--- | :--- |
| `400 Bad Request` | `ValidationException` | Show specific form input error messages. |
| `402 Payment Required` | `Trial expired` | **Block Interaction**. Show Premium Upgrade Modal. |
| `403 Forbidden` | `Access denied` | User attempting to access unauthorized resource. Redirect to Home. |
| `409 Conflict` | `Program not active` | User acting on old/paused program state. Refresh to get latest state. |
| `409 Conflict` | `Quiz not due` | User trying to take a quiz too early. Show "Not open yet" message. |
