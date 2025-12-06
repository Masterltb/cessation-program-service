# Backend Fixes Applied - 2025-12-06

## ✅ Summary

Fixed **3 critical issues** to ensure frontend-backend compatibility and security:

1. ✅ **Role-based Authorization** - Added path-based security rules
2. ✅ **Smoke Events History** - Added missing @GetMapping annotation
3. ✅ **Step Status Update** - Changed HTTP method from PATCH to PUT

---

## 🔐 Fix 1: Role-Based Authorization (CRITICAL)

### File: `SecurityConfig.java`

### Before:
```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/**", "/error").permitAll()
    .anyRequest().authenticated()  // ❌ Too permissive
);
```

**Problem:** Any authenticated user could access ANY endpoint (including admin endpoints)

### After:
```java
http.authorizeHttpRequests(auth -> auth
    // Public endpoints
    .requestMatchers("/actuator/**", "/error").permitAll()

    // Admin-only endpoints
    .requestMatchers("/v1/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/debug/**").hasRole("ADMIN")

    // Coach or Admin endpoints
    .requestMatchers("/coach/**").hasAnyRole("COACH", "ADMIN")

    // Customer-only endpoints
    .requestMatchers("/api/me/**").hasRole("CUSTOMER")
    .requestMatchers("/api/onboarding/**").hasRole("CUSTOMER")
    .requestMatchers("/v1/programs/**").hasRole("CUSTOMER")
    .requestMatchers("/v1/me/**").hasRole("CUSTOMER")
    .requestMatchers("/api/programs/**").hasRole("CUSTOMER")
    .requestMatchers("/api/plan-templates/**").hasRole("CUSTOMER")
    .requestMatchers("/api/modules/**").hasRole("CUSTOMER")
    .requestMatchers("/api/subscriptions/**").hasRole("CUSTOMER")

    // All other endpoints require authentication
    .anyRequest().authenticated()
);
```

### Impact:
- ✅ Customer **CANNOT** access admin endpoints (403 Forbidden)
- ✅ Coach **CANNOT** access admin endpoints (403 Forbidden)
- ✅ Admin **CAN** access admin endpoints
- ✅ Each role can only access their designated endpoints

### Test:
```bash
# Customer trying to access admin endpoint
curl -H "Authorization: Bearer $CUSTOMER_TOKEN" \
     POST /v1/admin/quizzes

# Before: 200 OK ❌
# After: 403 Forbidden ✅
```

---

## 📝 Fix 2: Smoke Events History Endpoint

### File: `SmokeEventDetailController.java`

### Before:
```java
/**
 * Lịch sử các lần hút.
 */

public List<SmokeEventRes> getHistory(  // ❌ No @GetMapping
        @PathVariable UUID programId,
        @RequestParam(defaultValue = "20") int size) {
    ...
}
```

**Problem:** Method not exposed as HTTP endpoint → Frontend receives 404 Not Found

### After:
```java
/**
 * Lịch sử các lần hút.
 */
@GetMapping("/history")
@PreAuthorize("isAuthenticated()")
public List<SmokeEventRes> getHistory(
        @PathVariable UUID programId,
        @RequestParam(defaultValue = "20") int size) {
    ...
}
```

### Impact:
- ✅ Frontend can now call `GET /api/programs/{id}/smoke-events/history`
- ✅ Returns list of smoke events for tracking page
- ✅ Consistent with `/stats` endpoint

### Test:
```bash
curl -H "Authorization: Bearer $TOKEN" \
     "GET /api/programs/{programId}/smoke-events/history?size=20"

# Before: 404 Not Found ❌
# After: 200 OK with smoke events array ✅
```

---

## 🔄 Fix 3: Step Status Update HTTP Method

### File: `StepController.java`

### Before:
```java
@PatchMapping("/{id}/status")  // ❌ Frontend uses PUT
public void updateStatus(...) {
    ...
}
```

**Problem:** HTTP method mismatch - Frontend sends PUT, backend expects PATCH

### After:
```java
@PutMapping("/{id}/status")  // ✅ Matches frontend
public void updateStatus(...) {
    ...
}
```

### Impact:
- ✅ Frontend PUT request now works correctly
- ✅ Consistent HTTP semantics (PUT for updates)
- ✅ No frontend code change needed

### Test:
```bash
curl -X PUT \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"status":"COMPLETED","note":"Done"}' \
     "/api/programs/{programId}/steps/{stepId}/status"

# Before: 405 Method Not Allowed ❌
# After: 200 OK ✅
```

---

## 📊 Changes Summary

| File | Lines Changed | Type | Priority |
|------|---------------|------|----------|
| SecurityConfig.java | 34-57 (23 lines) | Security | CRITICAL |
| SmokeEventDetailController.java | 28-29 (2 lines) | Endpoint | HIGH |
| StepController.java | 88 (1 line) | HTTP Method | MEDIUM |

---

## 🧪 Testing Checklist

### Security Tests
- [ ] Customer cannot access `/v1/admin/quizzes` (403)
- [ ] Customer can access `/api/me` (200)
- [ ] Admin can access `/v1/admin/quizzes` (200)
- [ ] Coach can access `/coach/customers` (200)
- [ ] Unauthenticated user cannot access any protected endpoint (401)

### Endpoint Tests
- [ ] GET `/api/programs/{id}/smoke-events/history` returns smoke events (200)
- [ ] GET `/api/programs/{id}/smoke-events/stats` returns statistics (200)

### HTTP Method Tests
- [ ] PUT `/api/programs/{id}/steps/{stepId}/status` updates status (200)
- [ ] POST `/api/programs/{id}/steps/{stepId}/skip` skips step (200)

---

## 🚀 Deployment Steps

### 1. Verify Changes
```bash
cd d:\AWS\program_service\program-service

# Review changes
git diff SecurityConfig.java
git diff SmokeEventDetailController.java
git diff StepController.java
```

### 2. Build
```bash
mvn clean package -DskipTests
```

### 3. Test Locally (Optional)
```bash
# Start with prod profile
java -jar target/program-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### 4. Deploy to EC2
```bash
# Copy JAR to EC2
scp target/program-service-0.0.1-SNAPSHOT.jar ec2-user@<EC2_IP>:/opt/program-service/

# SSH to EC2
ssh ec2-user@<EC2_IP>

# Backup old JAR
sudo cp /opt/program-service/program-service.jar /opt/program-service/program-service.jar.backup

# Replace with new JAR
sudo mv /opt/program-service/program-service-0.0.1-SNAPSHOT.jar /opt/program-service/program-service.jar

# Set prod profile
sudo systemctl set-environment SPRING_PROFILES_ACTIVE=prod

# Restart service
sudo systemctl restart program-service

# Check logs
sudo journalctl -u program-service -f
```

### 5. Verify Deployment
```bash
# Test baseline quiz endpoint (should work now)
curl -H "Authorization: Bearer $CUSTOMER_TOKEN" \
     https://v7agf76rrh.execute-api.ap-southeast-1.amazonaws.com/prod/api/onboarding/baseline/quiz

# Test smoke history endpoint (should work now)
curl -H "Authorization: Bearer $CUSTOMER_TOKEN" \
     "https://v7agf76rrh.execute-api.ap-southeast-1.amazonaws.com/prod/api/programs/$PROGRAM_ID/smoke-events/history"

# Test admin endpoint with customer token (should be blocked)
curl -H "Authorization: Bearer $CUSTOMER_TOKEN" \
     https://v7agf76rrh.execute-api.ap-southeast-1.amazonaws.com/prod/v1/admin/quizzes
# Expected: 403 Forbidden
```

---

## 🎯 Expected Behavior After Deployment

### Baseline Quiz Flow (Now Works)
```
1. User completes signup
2. Navigate to /onboarding
3. Frontend calls GET /api/onboarding/baseline/quiz
4. Backend validates JWT → extracts userId from 'sub' claim
5. Returns quiz questions
6. User answers quiz
7. Frontend calls POST /api/onboarding/baseline
8. Backend recommends plan
```

### Smoke Tracking Flow (Now Works)
```
1. User navigates to /tracking
2. Frontend calls GET /api/programs/{id}/smoke-events/history
3. Backend returns list of smoke events
4. Frontend displays chart and history
```

### Security Flow (Now Enforced)
```
1. Customer tries to access /v1/admin/quizzes
2. API Gateway validates JWT ✅
3. Backend checks hasRole("ADMIN") ❌
4. Returns 403 Forbidden
5. Customer cannot access admin features
```

---

## 📋 Related Documents

- [BUSINESS_RULES.md](BUSINESS_RULES.md) - Business logic overview
- [FRONTEND_BACKEND_MISMATCH.md](FRONTEND_BACKEND_MISMATCH.md) - Issue analysis
- [AUTHORIZATION_ANALYSIS.md](AUTHORIZATION_ANALYSIS.md) - Security analysis
- [TEST_AUTHORIZATION_GAP.md](TEST_AUTHORIZATION_GAP.md) - Test cases
- [DEPLOY_WITH_JWT.md](DEPLOY_WITH_JWT.md) - JWT setup guide

---

## ✅ Status

- **Code Changes:** ✅ Complete
- **Build:** ⏳ Pending
- **Tests:** ⏳ Pending
- **Deployment:** ⏳ Pending
- **Verification:** ⏳ Pending

---

**Date:** 2025-12-06
**Author:** Claude Code
**Build Time:** ~5 minutes
**Deploy Time:** ~10 minutes
**Total Time:** ~15 minutes
