# Authorization Analysis: Frontend vs Backend

## 🎯 Overview

Phân tích phân quyền giữa Frontend (React) và Backend (Spring Boot) để đảm bảo security consistency.

---

## 🔐 Role Definitions

### Frontend Roles (lowercase)
```javascript
- 'customer' - Người dùng thường (cai thuốc)
- 'coach' - Huấn luyện viên
- 'admin' - Quản trị viên
```

### Backend Roles (uppercase với prefix)
```java
- "ROLE_CUSTOMER" - Map từ Cognito group "customer"
- "ROLE_COACH" - Map từ Cognito group "coach"
- "ROLE_ADMIN" - Map từ Cognito group "admin"
```

### Mapping Flow
```
Cognito User Pool Groups → JWT claim "cognito:groups" → Backend Spring Security
["customer"] → ROLE_CUSTOMER
["coach"] → ROLE_COACH
["admin"] → ROLE_ADMIN
```

---

## ✅ Frontend Authorization (MATCHED)

### Route-Level Protection

**File:** `src/App.jsx`

```javascript
// Customer-only routes
<ProtectedRoute allowedRoles={['customer']}>
  /dashboard
  /badges
  /tasks
  /quiz/:templateId
  /tracking
  /program/settings
</ProtectedRoute>

// Coach + Admin routes
<ProtectedRoute allowedRoles={['coach', 'admin']}>
  /coach/*
</ProtectedRoute>

// Admin-only routes
<ProtectedRoute allowedRoles={['admin']}>
  /admin/*
</ProtectedRoute>

// All authenticated users
<ProtectedRoute allowedRoles={['customer', 'coach', 'admin']}>
  /onboarding
  /profile
  /blogs/create
  /blogs/:id/edit
</ProtectedRoute>

// Public routes (no protection)
/
/login
/community
/blogs
/blogs/:id
```

**Implementation:** `src/components/ProtectedRoute.jsx`
```javascript
if (allowedRoles && !hasAnyRole(allowedRoles)) {
  return <Navigate to="/unauthorized" replace />;
}
```

---

## ⚠️ Backend Authorization (PARTIALLY MATCHED)

### SecurityConfig

**File:** `src/main/java/com/smokefree/program/config/SecurityConfig.java`

```java
@Configuration
@EnableMethodSecurity  // ✅ Enabled but underutilized
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/**", "/error").permitAll()
            .anyRequest().authenticated()  // ⚠️ TOO PERMISSIVE
        );
    }
}
```

**Issues:**
- ❌ NO role-based path matchers
- ❌ All endpoints just require `authenticated()`, not specific roles
- ⚠️ Relies on manual checks in service layer

---

### Controller-Level Security (MINIMAL)

**@PreAuthorize usage:** ONLY 1 instance found

**File:** `SmokeEventDetailController.java`
```java
@GetMapping("/stats")
@PreAuthorize("isAuthenticated()")  // ⚠️ Too permissive
public SmokeEventStatisticsRes getStatistics(...) {
```

**Problem:** Should be `@PreAuthorize("hasRole('CUSTOMER')")`

---

### Service-Layer Security (MANUAL CHECKS)

**File:** `SmokeEventServiceImpl.java`
```java
public SmokeEvent create(UUID userId, UUID programId, SmokeEventReq req) {
    // ✅ Manual authorization check
    if (SecurityUtil.hasRole("ADMIN")) {
        return; // Admin can access any program
    }

    // ✅ Owner check
    boolean isCoach = program.getCoachId() != null
        && program.getCoachId().equals(userId)
        && SecurityUtil.hasRole("COACH");

    if (!program.getUserId().equals(userId) && !isCoach) {
        throw new ForbiddenException("Not your program");
    }
}
```

**Pattern:** Most services use manual checks instead of annotations

---

## 🔍 Authorization Patterns Comparison

| Layer | Frontend | Backend | Status |
|-------|----------|---------|--------|
| **Route/Path** | ✅ ProtectedRoute | ❌ No path-based rules | ⚠️ Mismatch |
| **Controller** | N/A | ❌ Almost no @PreAuthorize | ⚠️ Weak |
| **Service** | N/A | ✅ Manual checks | ✅ Good |
| **Ownership** | ❌ No check | ✅ Service checks userId | ⚠️ Backend only |

---

## 🚨 Security Gaps

### Gap 1: Backend lacks path-based authorization

**Problem:**
```java
// Current
.anyRequest().authenticated()  // Any authenticated user can access ANY endpoint

// Should be
.requestMatchers("/api/me/**").hasRole("CUSTOMER")
.requestMatchers("/v1/admin/**").hasRole("ADMIN")
.requestMatchers("/coach/**").hasAnyRole("COACH", "ADMIN")
```

**Impact:** Frontend blocks routes, but if someone calls API directly → Backend allows

---

### Gap 2: Missing @PreAuthorize on most endpoints

**Examples:**

**OnboardingFlowController.java**
```java
@GetMapping("/baseline/quiz")
public OpenAttemptRes getBaselineQuiz() {
    // ❌ NO authorization check
    // Anyone authenticated can access
}
```

**Should be:**
```java
@GetMapping("/baseline/quiz")
@PreAuthorize("hasRole('CUSTOMER')")  // Only customers need onboarding
public OpenAttemptRes getBaselineQuiz() {
```

**ProgramController.java**
```java
@PostMapping
public ProgramRes createProgram(@RequestBody CreateProgramReq req) {
    // ❌ NO role check
}
```

**Should be:**
```java
@PostMapping
@PreAuthorize("hasRole('CUSTOMER')")
public ProgramRes createProgram(...) {
```

---

### Gap 3: Inconsistent role naming

**Frontend:** lowercase ('customer', 'coach', 'admin')
**Backend SecurityUtil:** uppercase without prefix ('CUSTOMER', 'COACH', 'ADMIN')
**Spring Security:** uppercase with prefix ('ROLE_CUSTOMER', 'ROLE_COACH', 'ROLE_ADMIN')

**Impact:** Confusion, potential bugs

---

## ✅ What's Working Well

### 1. Service-Layer Ownership Checks

✅ Most services validate program ownership:
```java
UUID currentUserId = SecurityUtil.requireUserId();
if (!program.getUserId().equals(currentUserId)) {
    throw new ForbiddenException("Not your program");
}
```

### 2. Admin Override Pattern

✅ Admins can access any resource:
```java
if (SecurityUtil.hasRole("ADMIN")) {
    return; // Allow admin access
}
```

### 3. Coach Access Pattern

✅ Coaches can access assigned customers:
```java
boolean isCoach = program.getCoachId() != null
    && program.getCoachId().equals(userId)
    && SecurityUtil.hasRole("COACH");
```

---

## 📊 API Endpoint Authorization Matrix

| Endpoint | Required Role | Frontend Check | Backend Check | Status |
|----------|---------------|----------------|---------------|--------|
| `GET /api/me` | CUSTOMER | ✅ customer | ⚠️ authenticated only | WEAK |
| `GET /api/onboarding/baseline/quiz` | CUSTOMER | ✅ customer | ❌ none | WEAK |
| `POST /v1/programs` | CUSTOMER | ✅ customer | ⚠️ service check | OK |
| `GET /api/programs/{id}/steps/today` | CUSTOMER (owner) | ✅ customer | ✅ service check | GOOD |
| `POST /api/programs/{id}/smoke-events` | CUSTOMER (owner) | ✅ customer | ✅ service check | GOOD |
| `GET /v1/me/quizzes` | CUSTOMER | ✅ customer | ⚠️ authenticated only | WEAK |
| `GET /v1/admin/quizzes` | ADMIN | ✅ admin | ❌ none | WEAK |
| `GET /coach/*` | COACH or ADMIN | ✅ coach,admin | ❌ none | WEAK |

---

## 🔧 Recommendations

### Priority 1: Add Path-Based Authorization (CRITICAL)

**Update SecurityConfig.java:**
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http.authorizeHttpRequests(auth -> auth
        // Public endpoints
        .requestMatchers("/actuator/**", "/error").permitAll()

        // Admin endpoints
        .requestMatchers("/v1/admin/**").hasRole("ADMIN")
        .requestMatchers("/api/debug/**").hasRole("ADMIN")

        // Coach endpoints
        .requestMatchers("/coach/**").hasAnyRole("COACH", "ADMIN")

        // Customer endpoints
        .requestMatchers("/api/me/**").hasRole("CUSTOMER")
        .requestMatchers("/api/onboarding/**").hasRole("CUSTOMER")
        .requestMatchers("/v1/programs/**").hasRole("CUSTOMER")
        .requestMatchers("/v1/me/quizzes/**").hasRole("CUSTOMER")
        .requestMatchers("/api/programs/**").hasRole("CUSTOMER")

        // All other endpoints require authentication
        .anyRequest().authenticated()
    );
}
```

---

### Priority 2: Add @PreAuthorize to Controllers (HIGH)

**OnboardingFlowController.java:**
```java
@RestController
@RequestMapping("/api/onboarding")
@PreAuthorize("hasRole('CUSTOMER')")  // Apply to all methods
public class OnboardingFlowController {

    @GetMapping("/baseline/quiz")
    public OpenAttemptRes getBaselineQuiz() {
        // Now protected by class-level annotation
    }
}
```

**ProgramController.java:**
```java
@RestController
@RequestMapping("/v1/programs")
@PreAuthorize("hasRole('CUSTOMER')")
public class ProgramController {

    @PostMapping
    public ProgramRes createProgram(@RequestBody CreateProgramReq req) {
        // Protected
    }
}
```

**AdminQuizController.java:**
```java
@RestController
@RequestMapping("/v1/admin/quizzes")
@PreAuthorize("hasRole('ADMIN')")  // Admin only
public class AdminQuizController {
    // All methods require ADMIN role
}
```

---

### Priority 3: Standardize Role Names (MEDIUM)

**Create constants:**
```java
public class Roles {
    public static final String CUSTOMER = "CUSTOMER";
    public static final String COACH = "COACH";
    public static final String ADMIN = "ADMIN";

    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    public static final String ROLE_COACH = "ROLE_COACH";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
}
```

**Use in annotations:**
```java
@PreAuthorize("hasRole(T(com.smokefree.program.util.Roles).CUSTOMER)")
```

---

## 🧪 Testing Authorization

### Test 1: Customer accessing Admin endpoint

**Request:**
```bash
# Customer JWT token
TOKEN="eyJ..."

# Try to access admin endpoint
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/v1/admin/quizzes

# Expected: 403 Forbidden
# Current: 200 OK (BUG!)
```

### Test 2: Coach accessing other coach's customer

**Request:**
```bash
# Coach A token
TOKEN_COACH_A="..."

# Try to access Coach B's customer program
curl -H "Authorization: Bearer $TOKEN_COACH_A" \
     http://localhost:8080/api/programs/{coach_b_program_id}/steps/today

# Expected: 403 Forbidden
# Current: Service layer blocks (OK)
```

### Test 3: Direct API call bypassing frontend

**Request:**
```bash
# Customer token, but calling endpoint frontend doesn't show
curl -H "Authorization: Bearer $CUSTOMER_TOKEN" \
     http://localhost:8080/v1/admin/quizzes

# Expected: 403 Forbidden
# Current: Likely 200 OK (BUG!)
```

---

## 📋 Implementation Checklist

### Phase 1: Critical Security (DO NOW)
- [ ] Add path-based authorization in SecurityConfig
- [ ] Add @PreAuthorize to all admin endpoints
- [ ] Test admin endpoints reject non-admin users
- [ ] Test coach endpoints reject customers

### Phase 2: Enhanced Security (NEXT SPRINT)
- [ ] Add @PreAuthorize to all customer endpoints
- [ ] Create Roles constants class
- [ ] Refactor SecurityUtil to use constants
- [ ] Add integration tests for authorization

### Phase 3: Audit & Refactor (BACKLOG)
- [ ] Remove duplicate manual checks where @PreAuthorize exists
- [ ] Standardize error messages (403 vs 404)
- [ ] Add audit logging for authorization failures
- [ ] Document authorization patterns in wiki

---

## 🎯 Summary

### Current State
- ✅ **Frontend:** Strong route-level protection
- ⚠️ **Backend:** Weak controller-level, good service-level
- ❌ **Gap:** Direct API calls can bypass frontend protection

### Risk Level
- **Medium-High** - Backend endpoints mostly trust authentication but not authorization
- Relies on service-layer checks (good) but lacks defense-in-depth

### Quick Wins
1. Add path-based rules to SecurityConfig (15 minutes)
2. Add @PreAuthorize to admin/coach controllers (30 minutes)
3. Test with different role tokens (15 minutes)

**Total time to fix critical issues: ~1 hour**

---

**Date:** 2025-12-06
**Status:** ⚠️ Authorization gaps identified, recommendations provided
**Priority:** HIGH - Should fix before production deployment
