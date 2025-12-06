# Frontend-Backend API Mismatch Report

## 🔍 Kiểm tra ngày: 2025-12-06

---

## ❌ Vấn đề tìm thấy

### 1. ⚠️ CRITICAL: Smoke Events History - Missing @GetMapping

**File:** `SmokeEventDetailController.java` (line 29)

**Vấn đề:**
```java
// THIẾU @GetMapping("/history")
public List<SmokeEventRes> getHistory(
    @PathVariable UUID programId,
    @RequestParam(defaultValue = "20") int size) {
```

**Frontend expects:**
```javascript
GET /api/programs/{programId}/smoke-events/history
```

**Backend hiện tại:**
- Method `getHistory()` **KHÔNG có annotation**
- Endpoint **KHÔNG được expose**
- Frontend sẽ nhận **404 Not Found**

**Fix:**
```java
@GetMapping("/history")  // <-- THÊM DÒNG NÀY
@PreAuthorize("isAuthenticated()")
public List<SmokeEventRes> getHistory(
    @PathVariable UUID programId,
    @RequestParam(defaultValue = "20") int size) {
```

---

### 2. ⚠️ WARNING: Update Step Status - HTTP Method Mismatch

**File:** `StepController.java` (line 88)

**Vấn đề:**
```java
@PatchMapping("/{id}/status")  // Backend dùng PATCH
public void updateStatus(...) {
```

**Frontend expects:**
```javascript
PUT /api/programs/{programId}/steps/{stepId}/status
```

**Impact:**
- HTTP semantic: PUT = full replacement, PATCH = partial update
- Có thể gây lỗi nếu client chỉ accept PUT

**Recommendation:**
```java
@PutMapping("/{id}/status")  // Đổi từ PATCH → PUT
// hoặc support cả hai:
@RequestMapping(value = "/{id}/status", method = {RequestMethod.PUT, RequestMethod.PATCH})
public void updateStatus(...) {
```

---

## ✅ Các endpoint đã match đúng

### Flow 1: Onboarding
| Frontend | Backend | Status |
|----------|---------|--------|
| `GET /api/onboarding/baseline/quiz` | OnboardingFlowController | ✅ |
| `POST /api/onboarding/baseline` | OnboardingFlowController | ✅ |
| `GET /api/plan-templates/{id}` | PlanTemplateController | ✅ |

### Flow 2: Enrollment
| Frontend | Backend | Status |
|----------|---------|--------|
| `POST /v1/programs` | ProgramController | ✅ |

### Flow 3: Dashboard
| Frontend | Backend | Status |
|----------|---------|--------|
| `GET /api/me` | MeController | ✅ |
| `GET /api/me/badges` | BadgeController | ✅ |
| `GET /api/me/badges/all` | BadgeController | ✅ |

### Flow 4: Daily Tasks
| Frontend | Backend | Status |
|----------|---------|--------|
| `GET /api/programs/{id}/steps/today` | StepController | ✅ |
| `GET /api/modules/by-code/{code}` | ModuleController | ✅ |
| `PUT /api/programs/{id}/steps/{stepId}/status` | StepController | ⚠️ (PATCH) |
| `POST /api/programs/{id}/steps/{stepId}/skip` | StepController | ✅ |

### Flow 5: Quiz Engine
| Frontend | Backend | Status |
|----------|---------|--------|
| `GET /v1/me/quizzes` | MeQuizController | ✅ |
| `POST /v1/me/quizzes/{templateId}/open` | MeQuizController | ✅ |
| `PUT /v1/me/quizzes/{attemptId}/answer` | MeQuizController | ✅ |
| `POST /v1/me/quizzes/{attemptId}/submit` | MeQuizController | ✅ |

### Flow 6: Smoke Events & Streak
| Frontend | Backend | Status |
|----------|---------|--------|
| `POST /api/programs/{id}/smoke-events` | SmokeEventController | ✅ |
| `GET /api/programs/{id}/smoke-events/history` | SmokeEventDetailController | ❌ THIẾU |
| `GET /api/programs/{id}/smoke-events/stats` | SmokeEventDetailController | ✅ |
| `GET /api/programs/{id}/streak` | StreakController | ✅ |

### Flow 7: Program Management
| Frontend | Backend | Status |
|----------|---------|--------|
| `POST /api/programs/{id}/pause` | ProgramManagementController | ✅ |
| `POST /api/programs/{id}/resume` | ProgramManagementController | ✅ |
| `POST /api/programs/{id}/end` | ProgramManagementController | ✅ |
| `GET /api/programs/{id}/trial-status` | ProgramManagementController | ✅ |

### Flow 8: Subscription
| Frontend | Backend | Status |
|----------|---------|--------|
| `POST /api/subscriptions/upgrade` | SubscriptionController | ✅ |

---

## 📊 Summary

### Thống kê
- **Tổng endpoints:** 24
- **Match đúng:** 22 ✅
- **Thiếu hoàn toàn:** 1 ❌
- **HTTP method khác:** 1 ⚠️

### Mức độ nghiêm trọng
- **CRITICAL (P0):** 1 issue - Smoke events history không work
- **WARNING (P1):** 1 issue - HTTP method mismatch

---

## 🔧 Action Items

### Ưu tiên cao (P0) - Fix ngay

#### 1. Thêm @GetMapping cho smoke events history

**File:** `src/main/java/com/smokefree/program/web/controller/SmokeEventDetailController.java`

**Dòng 29:**
```java
@GetMapping("/history")  // <-- THÊM ANNOTATION NÀY
@PreAuthorize("isAuthenticated()")
public List<SmokeEventRes> getHistory(
    @PathVariable UUID programId,
    @RequestParam(defaultValue = "20") int size) {

    log.info("[SmokeEvent] Get history for program {} size {}", programId, size);
    return smokeEventService.getHistory(programId, size).stream()
            .map(SmokeEventRes::from)
            .toList();
}
```

**Test sau khi fix:**
```bash
curl -H "Authorization: Bearer $TOKEN" \
     "http://172.0.3.240:8080/api/programs/{programId}/smoke-events/history?size=20"
```

---

### Ưu tiên trung bình (P1) - Nên fix

#### 2. Chuẩn hóa HTTP method cho update step status

**File:** `src/main/java/com/smokefree/program/web/controller/StepController.java`

**Option 1: Đổi sang PUT (khuyến nghị)**
```java
@PutMapping("/{id}/status")  // Đổi từ PATCH → PUT
public void updateStatus(@PathVariable("programId") UUID programId,
                         @PathVariable("id") UUID assignmentId,
                         @RequestBody UpdateStepStatusReq req) {
    UUID userId = SecurityUtil.requireUserId();
    service.updateStatus(userId, programId, assignmentId, req.status(), req.note());
}
```

**Option 2: Support cả hai (backward compatible)**
```java
@RequestMapping(
    value = "/{id}/status",
    method = {RequestMethod.PUT, RequestMethod.PATCH}
)
public void updateStatus(...) {
```

**Option 3: Sửa frontend (không khuyến nghị)**
```javascript
// Đổi từ PUT → PATCH
await api.patch(BACKEND_API.program.dailyTasks.updateStatus(programId, stepId), data);
```

---

## 🧪 Testing Checklist

### Sau khi fix issue #1 (Smoke events history)

- [ ] Build backend: `mvn clean package`
- [ ] Deploy lên EC2
- [ ] Test endpoint:
  ```bash
  TOKEN="..."
  PROGRAM_ID="..."
  curl -H "Authorization: Bearer $TOKEN" \
       "http://172.0.3.240:8080/api/programs/$PROGRAM_ID/smoke-events/history"
  ```
- [ ] Verify response: `200 OK` with array of smoke events
- [ ] Test từ frontend: Navigate to Smoke Tracking page
- [ ] Check history tab loads correctly

### Sau khi fix issue #2 (Step status update)

- [ ] Build backend
- [ ] Deploy lên EC2
- [ ] Test với PUT:
  ```bash
  curl -X PUT \
       -H "Authorization: Bearer $TOKEN" \
       -H "Content-Type: application/json" \
       -d '{"status":"COMPLETED","note":"Done"}' \
       "http://172.0.3.240:8080/api/programs/$PROGRAM_ID/steps/$STEP_ID/status"
  ```
- [ ] Verify response: `200 OK`
- [ ] Test từ frontend: Complete a daily task
- [ ] Check status updates correctly

---

## 📝 Notes

### Tại sao vấn đề #1 nghiêm trọng?

**SmokeEventDetailController** có 2 methods:
- `getHistory()` - **THIẾU @GetMapping** → không work ❌
- `getStatistics()` - **CÓ @GetMapping("/stats")** → work ✅

Developer **quên thêm annotation** cho `getHistory()`, nên method này không được Spring MVC expose as endpoint.

### Tại sao PUT vs PATCH quan trọng?

**HTTP Semantic:**
- **PUT**: Replace toàn bộ resource
- **PATCH**: Update một phần resource

**Request body:**
```json
{
  "status": "COMPLETED",
  "note": "Đã hoàn thành"
}
```

→ Đây là **partial update** nên semantic đúng là **PATCH**

Nhưng frontend đang dùng **PUT**, nên backend nên support cả hai để tránh lỗi.

---

## ✅ Deployment Plan

### Bước 1: Fix code
```bash
cd d:\AWS\program_service\program-service
# Edit SmokeEventDetailController.java - Thêm @GetMapping("/history")
# Edit StepController.java - Đổi @PatchMapping → @PutMapping
```

### Bước 2: Build
```bash
mvn clean package -DskipTests
```

### Bước 3: Deploy
```bash
# Copy JAR to EC2
scp target/program-service-0.0.1-SNAPSHOT.jar ec2-user@EC2_IP:/opt/program-service/

# SSH to EC2
ssh ec2-user@EC2_IP

# Restart service
sudo systemctl restart program-service
```

### Bước 4: Verify
```bash
# Test history endpoint
curl -H "Authorization: Bearer $TOKEN" \
     "http://172.0.3.240:8080/api/programs/$PROGRAM_ID/smoke-events/history"

# Test update status endpoint
curl -X PUT \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"status":"COMPLETED"}' \
     "http://172.0.3.240:8080/api/programs/$PROGRAM_ID/steps/$STEP_ID/status"
```

---

**Status:** 🚨 2 issues found - 1 CRITICAL, 1 WARNING
**Next Action:** Fix SmokeEventDetailController.getHistory() annotation
**ETA:** 5 minutes to fix + 10 minutes to deploy
