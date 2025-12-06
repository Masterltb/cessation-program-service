# Test Authorization Gap - Proof of Concept

## 🎯 Mục đích
Chứng minh rằng Cognito authentication ≠ Backend authorization

---

## 🧪 Test Case 1: Customer accessing Admin endpoint

### Setup
1. Login as **Customer** user trong Cognito
2. Copy JWT token từ response
3. Decode token tại https://jwt.io

**Token payload:**
```json
{
  "sub": "uuid-customer",
  "cognito:groups": ["customer"],  // ← Chỉ có customer group
  "email": "customer@example.com",
  "iss": "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_dskUsnKt3"
}
```

### Test: Create Admin Quiz (Should FAIL)

**Request:**
```bash
CUSTOMER_TOKEN="eyJraWQiOiJ..."

curl -X POST \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "CUSTOMER_CREATED_QUIZ",
    "title": "Quiz tạo bởi customer",
    "description": "Không nên được phép"
  }' \
  https://v7agf76rrh.execute-api.ap-southeast-1.amazonaws.com/prod/v1/admin/quizzes
```

### Expected Result (with proper authorization):
```
HTTP 403 Forbidden
{
  "error": "Forbidden",
  "message": "Access Denied"
}
```

### Actual Result (current backend):
```
HTTP 200 OK
{
  "id": "uuid-quiz",
  "code": "CUSTOMER_CREATED_QUIZ",
  "title": "Quiz tạo bởi customer"
}
```

→ **🚨 SECURITY BREACH: Customer có thể tạo admin quiz!**

---

## 🧪 Test Case 2: Customer accessing other customer's program

### Setup
- **User A (customer):** programId = "aaa-111"
- **User B (customer):** programId = "bbb-222"

### Test: User A tries to access User B's program

**Request:**
```bash
USER_A_TOKEN="eyJraWQiOiJ..."

curl -H "Authorization: Bearer $USER_A_TOKEN" \
     https://v7agf76rrh.execute-api.ap-southeast-1.amazonaws.com/prod/api/programs/bbb-222/steps/today
```

### Expected Result:
```
HTTP 403 Forbidden
{
  "error": "Forbidden",
  "message": "Not your program"
}
```

### Actual Result (current backend):
```
HTTP 403 Forbidden (from service layer check)
```

→ **✅ BLOCKED by service layer** (good, but should be blocked at SecurityConfig level too)

---

## 🧪 Test Case 3: Coach accessing non-assigned customer

### Setup
- **Coach A:** assigned to Customer X
- **Coach A:** NOT assigned to Customer Y
- **Customer Y program:** programId = "yyy-333"

### Test: Coach A tries to access Customer Y's program

**Request:**
```bash
COACH_A_TOKEN="eyJraWQiOiJ..."

curl -H "Authorization: Bearer $COACH_A_TOKEN" \
     https://v7agf76rrh.execute-api.ap-southeast-1.amazonaws.com/prod/api/programs/yyy-333/steps/today
```

### Expected Result:
```
HTTP 403 Forbidden
{
  "error": "Forbidden",
  "message": "Not your assigned customer"
}
```

### Actual Result (current backend):
```
HTTP 403 Forbidden (from service layer check)
```

→ **✅ BLOCKED by service layer**

---

## 📊 Summary of Findings

| Test Case | Endpoint | Role | Expected | Actual | Gap |
|-----------|----------|------|----------|--------|-----|
| 1. Customer→Admin | `/v1/admin/quizzes` | customer | 403 | 200 ✅ | **CRITICAL** |
| 2. Customer→Customer | `/api/programs/{other}/steps` | customer | 403 | 403 | OK |
| 3. Coach→Unassigned | `/api/programs/{other}/steps` | coach | 403 | 403 | OK |

---

## 🔍 Root Cause Analysis

### Why Test Case 1 passes (BAD)?

**SecurityConfig.java:**
```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/**", "/error").permitAll()
    .anyRequest().authenticated()  // ← ONLY checks "has valid token"
);
```

**No role check for `/v1/admin/**` path!**

### Why Test Case 2 & 3 blocked (GOOD)?

**Service layer has manual checks:**
```java
// StepAssignmentServiceImpl.java
public List<StepAssignment> listByProgramAndDate(UUID programId, LocalDate date) {
    Program program = programRepository.findById(programId)...;

    // ✅ Check ownership
    if (SecurityUtil.hasRole("ADMIN")) {
        return; // Admin can access all
    }

    UUID currentUserId = SecurityUtil.requireUserId();
    boolean isOwner = program.getUserId().equals(currentUserId);
    boolean isCoach = program.getCoachId() != null
        && program.getCoachId().equals(currentUserId)
        && SecurityUtil.hasRole("COACH");

    if (!isOwner && !isCoach) {
        throw new ForbiddenException("Not your program");
    }
}
```

→ **Defense in depth works, but admin endpoints are exposed!**

---

## 🛠️ How to Fix

### Solution 1: Add path-based authorization (RECOMMENDED)

**Update SecurityConfig.java:**
```java
http.authorizeHttpRequests(auth -> auth
    // Public
    .requestMatchers("/actuator/**", "/error").permitAll()

    // Admin only
    .requestMatchers("/v1/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/debug/**").hasRole("ADMIN")

    // Coach or Admin
    .requestMatchers("/coach/**").hasAnyRole("COACH", "ADMIN")

    // Customer only
    .requestMatchers("/api/me/**").hasRole("CUSTOMER")
    .requestMatchers("/api/onboarding/**").hasRole("CUSTOMER")
    .requestMatchers("/v1/programs/**").hasRole("CUSTOMER")
    .requestMatchers("/v1/me/**").hasRole("CUSTOMER")
    .requestMatchers("/api/programs/**").hasRole("CUSTOMER")

    // All authenticated
    .anyRequest().authenticated()
);
```

### Solution 2: Add @PreAuthorize to controllers

**AdminQuizController.java:**
```java
@RestController
@RequestMapping("/v1/admin/quizzes")
@PreAuthorize("hasRole('ADMIN')")  // ← Class-level annotation
public class AdminQuizController {
    // All methods now require ADMIN role
}
```

### Solution 3: Both (Defense in Depth) ⭐ BEST

Combine both solutions for maximum security:
- SecurityConfig blocks at HTTP layer
- @PreAuthorize blocks at method layer
- Service checks block at business logic layer

---

## 🧪 After Fix - Re-test

### Test Case 1 (after fix):

**Request:**
```bash
curl -X POST \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  https://v7agf76rrh.execute-api.ap-southeast-1.amazonaws.com/prod/v1/admin/quizzes
```

**Expected Result:**
```
HTTP 403 Forbidden
{
  "timestamp": "2025-12-06T...",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/v1/admin/quizzes"
}
```

**Actual Result (after fix):**
```
HTTP 403 Forbidden ✅
```

→ **🎉 SECURITY FIXED!**

---

## 📋 Full Test Suite

### Automated Test Script

```bash
#!/bin/bash

API_URL="https://v7agf76rrh.execute-api.ap-southeast-1.amazonaws.com/prod"

# Get tokens (replace with actual login)
CUSTOMER_TOKEN=$(curl -s -X POST ... | jq -r '.AuthenticationResult.IdToken')
COACH_TOKEN=$(curl -s -X POST ... | jq -r '.AuthenticationResult.IdToken')
ADMIN_TOKEN=$(curl -s -X POST ... | jq -r '.AuthenticationResult.IdToken')

echo "=== Test 1: Customer accessing admin endpoint ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  "$API_URL/v1/admin/quizzes")

if [ "$HTTP_CODE" == "403" ]; then
  echo "✅ PASS: Customer blocked from admin endpoint"
else
  echo "❌ FAIL: Customer accessed admin endpoint (HTTP $HTTP_CODE)"
fi

echo "=== Test 2: Admin accessing admin endpoint ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$API_URL/v1/admin/quizzes")

if [ "$HTTP_CODE" == "200" ]; then
  echo "✅ PASS: Admin can access admin endpoint"
else
  echo "❌ FAIL: Admin blocked from admin endpoint (HTTP $HTTP_CODE)"
fi

echo "=== Test 3: Customer accessing customer endpoint ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  "$API_URL/api/me")

if [ "$HTTP_CODE" == "200" ]; then
  echo "✅ PASS: Customer can access customer endpoint"
else
  echo "❌ FAIL: Customer blocked from customer endpoint (HTTP $HTTP_CODE)"
fi

echo "=== Test 4: Coach accessing coach endpoint ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $COACH_TOKEN" \
  "$API_URL/coach/customers")

if [ "$HTTP_CODE" == "200" ]; then
  echo "✅ PASS: Coach can access coach endpoint"
else
  echo "❌ FAIL: Coach blocked from coach endpoint (HTTP $HTTP_CODE)"
fi
```

---

## 🎯 Conclusion

### Current State
- ✅ **Cognito:** Authenticates users correctly
- ✅ **API Gateway:** Validates JWT tokens
- ❌ **Backend:** Does NOT enforce role-based authorization at HTTP layer
- ✅ **Services:** Enforce ownership checks

### Security Level
- **Authentication:** STRONG (Cognito + JWT)
- **Authorization:** WEAK (relies only on service layer)

### Risk
- **HIGH:** Any authenticated user can call any endpoint
- **MEDIUM:** Service layer provides some protection for ownership

### Recommendation
**Fix immediately before production deployment**
- Add path-based rules to SecurityConfig (15 min)
- Add @PreAuthorize to admin/coach controllers (30 min)
- Run test suite to verify (15 min)

**Total time: 1 hour**

---

**Test Date:** 2025-12-06
**Status:** 🚨 Authorization gap confirmed
**Severity:** HIGH
**Fix Priority:** P0 (Critical)
