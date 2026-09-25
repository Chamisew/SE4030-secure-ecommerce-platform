# V06: Unauthenticated Mail Dispatch & Security Filter Bypass (AFTER FIX)

## 1. Vulnerability & Fix Overview

* **Vulnerability ID**: `V06`
* **Vulnerability Title**: Unauthenticated Mail Dispatch via Root Endpoint Bypassing Spring Security Filter Chain
* **Assigned Member**: Member 3 — Security Misconfiguration Specialist (`It22106292 <it22106292@my.sliit.lk>`)
* **Branch**: `fix/C-secmisconfig-vulns`
* **CWE**: CWE-284 (Improper Access Control), CWE-16 (Security Misconfiguration)
* **OWASP Top 10**: A01:2021 – Broken Access Control, A05:2021 – Security Misconfiguration
* **Fix Status**: **VERIFIED & REMEDIATED**

---

## 2. Root Cause & Code Remediation

### 2.1 The Flaw (BEFORE)
1. `SecurityConfig.java` contained:
   ```java
   http.securityMatcher("/api/**")
   ```
   This scoped the entire Spring Security filter chain strictly to URL paths prefixed with `/api/`.
2. `TestEmailController.java` mapped to `/test-email`, which resides outside `/api/**`.
3. Consequently, requests to `/test-email` bypassed all security filters, authentication entry points, authorization checks, and HTTP security response headers. An anonymous user could repeatedly trigger outbound email dispatch to a hardcoded email address.

### 2.2 The Remediation (AFTER)
1. **Removed Narrow Path Matcher (Default-Deny)**:
   In [SecurityConfig.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/SecurityConfig.java#L32-L48), removed `.securityMatcher("/api/**")`. Spring Security now protects all endpoints across the entire application by default.
2. **Explicit Authorization Rules**:
   In [SecurityConfig.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/SecurityConfig.java#L47), restricted `/test-email` to administrative users:
   ```java
   .requestMatchers("/api/v1/auth/dev/**", "/test-email", "/test-email/**").hasRole("ADMIN")
   ```
3. **Method-Level Pre-Authorization**:
   In [TestEmailController.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/controller/TestEmailController.java#L16), annotated the handler with:
   ```java
   @PreAuthorize("hasRole('ADMIN')")
   ```

---

## 3. Re-Testing & Verification Evidence

### 3.1 Anonymous Request Attempt
```bash
curl.exe -s -i -X GET http://localhost:8080/test-email
```
* **HTTP Status**: `401 Unauthorized`
* **Response Body**:
  ```json
  {
    "message": "Full authentication is required to access this resource",
    "error": "Unauthorized",
    "path": "/test-email",
    "status": 401
  }
  ```
* **Security Headers Present**: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-cache`.

### 3.2 Authenticated Non-Admin Request Attempt
```http
GET /test-email
Authorization: Bearer <valid_ROLE_USER_token>
```
* **HTTP Status**: `403 Forbidden`
* **Response Body**:
  ```json
  {"message":"Access Denied","error":"Forbidden","status":403}
  ```

---

## 4. Evidence File Link

Detailed execution log with full HTTP transcripts:
* [evidence-log.txt](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/AFTER/V06-unauthenticated-email-trigger/evidence-log.txt)
