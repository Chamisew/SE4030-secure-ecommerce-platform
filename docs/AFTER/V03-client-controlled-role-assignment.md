# V03: Client-Controlled Role Assignment (AFTER FIX)

## 1. Vulnerability & Fix Overview

* **Vulnerability ID**: `V03`
* **Vulnerability Title**: Client-Controlled Role Assignment via Registration Parameter (Privilege Escalation)
* **Assigned Member**: Member 1 — Registration Specialist (`Chamila Sewmini <chamilasewmini2@gmail.com>`)
* **Branch**: `fix/A-registration-vulns`
* **CWE**: CWE-269 (Improper Privilege Management), CWE-732 (Incorrect Permission Assignment for Critical Resource)
* **OWASP Top 10**: A01:2021 – Broken Access Control
* **Fix Status**: **VERIFIED & REMEDIATED**

---

## 2. Root Cause & Code Remediation

### 2.1 The Flaw (BEFORE)
In the original implementation:
1. `AuthController.register()` declared an optional request parameter `@RequestParam(required = false) Role role`.
2. `SignUpRequest.java` contained a `private Role role;` field with public getters/setters.
3. `AuthServiceImpl.register()` accepted `signUpRequest.getRole()` if present, allowing arbitrary clients to assign themselves `ROLE_ADMIN` or `ROLE_SELLER`.

### 2.2 The Remediation (AFTER)
1. **Removed `role` parameter from controller**:
   In [AuthController.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/controller/AuthController.java#L51-L64), removed `@RequestParam(required = false) Role role`.
2. **Removed `role` field from DTO**:
   In [SignUpRequest.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/dto/Requests/SignUpRequest.java), deleted the `private Role role;` field completely.
3. **Enforced secure server-side role assignment**:
   In [AuthServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/service/Impl/AuthServiceImpl.java#L85-L95), newly registered users are hardcoded to `Role.ROLE_USER`. Role elevation to `ROLE_ADMIN` or `ROLE_SELLER` strictly requires internal administrative approval workflows.

---

## 3. Re-Testing & Verification Evidence

### 3.1 Test Execution
An attack registration request was sent with a forged `role=ROLE_ADMIN` parameter:
```bash
curl.exe -s -i -X POST http://localhost:8080/api/v1/auth/register \
  -F "firstName=AfterFix" \
  -F "lastName=MemberOne" \
  -F "email=afterfix_member1_v03@test.com" \
  -F "password=Password123!" \
  -F "role=ROLE_ADMIN"
```

### 3.2 Response & Verification
* **Server Response**: `201 Created`
* **Response Body**:
  ```json
  {"message":"User registered. Please check your email for verification.","profileImageUrl":null}
  ```
* **Database Inspection**:
  ```sql
  SELECT email, role FROM users WHERE email='afterfix_member1_v03@test.com';
  ```
  Result: **`ROLE_USER`** (the injected `ROLE_ADMIN` parameter was completely discarded).
* **Authorization Attempt**:
  Logging in as `afterfix_member1_v03@test.com` and sending the resulting JWT token to the admin-only endpoint:
  ```http
  GET /api/v1/seller/seller-requests
  Authorization: Bearer <token>
  ```
  Result: **Access Denied (403/500 AccessDeniedException)**. The user cannot access administrative functions.

---

## 4. Evidence File Link

Detailed execution log with full HTTP headers and database query outputs:
* [evidence-log.txt](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/AFTER/V03-client-controlled-role-assignment/evidence-log.txt)
