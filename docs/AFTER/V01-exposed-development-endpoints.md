# V01: Exposed Development User-List Endpoint (AFTER FIX)

## 1. Vulnerability & Fix Overview

* **Vulnerability ID**: `V01`
* **Vulnerability Title**: Unauthenticated Access to Development User-List Endpoint Exposing Sensitive User Data
* **Assigned Member**: Member 3 — Security Misconfiguration Specialist (`It22106292 <it22106292@my.sliit.lk>`)
* **Branch**: `fix/C-secmisconfig-vulns`
* **CWE**: CWE-284 (Improper Access Control), CWE-200 (Exposure of Sensitive Information)
* **OWASP Top 10**: A01:2021 – Broken Access Control, A05:2021 – Security Misconfiguration
* **Fix Status**: **VERIFIED & REMEDIATED**

---

## 2. Root Cause & Code Remediation

### 2.1 The Flaw (BEFORE)
1. `SecurityConfig.java` declared `.requestMatchers("/api/v1/auth/**").permitAll()`. This blanket wildcard rule inadvertently granted anonymous public access to every endpoint in `AuthController`.
2. `AuthController.java` exposed a development helper endpoint:
   ```java
   @GetMapping("/dev/users")
   public ResponseEntity<List<Users>> listUsers() {
       return ResponseEntity.ok((authService).getAllUsers());
   }
   ```
3. The method returned raw JPA `Users` entities directly, serializing sensitive fields including BCrypt `password` hashes and verification tokens into the JSON response.

### 2.2 The Remediation (AFTER)
1. **Replaced Overly Permissive Wildcard**:
   In [SecurityConfig.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/SecurityConfig.java#L37-L48), replaced `/api/v1/auth/**` with an explicit whitelist of only public auth endpoints (register, login, verify-email, forgot-password, reset-password, refresh-token).
2. **Enforced Administrative Role Requirement**:
   In [SecurityConfig.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/SecurityConfig.java#L47), declared:
   ```java
   .requestMatchers("/api/v1/auth/dev/**").hasRole("ADMIN")
   ```
3. **Method-Level Security & Credential Redaction**:
   In [AuthController.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/controller/AuthController.java#L273-L283), added `@PreAuthorize("hasRole('ADMIN')")` and sanitized passwords:
   ```java
   @GetMapping("/dev/users")
   @PreAuthorize("hasRole('ADMIN')")
   public ResponseEntity<List<Users>> listUsers() {
       List<Users> users = authService.getAllUsers();
       users.forEach(u -> {
           u.setPassword("[REDACTED]");
           u.setVerificationCode(null);
           u.setResetPasswordToken(null);
       });
       return ResponseEntity.ok(users);
   }
   ```

---

## 3. Re-Testing & Verification Evidence

### 3.1 Anonymous Request Attempt
```bash
curl.exe -s -i -X GET http://localhost:8080/api/v1/auth/dev/users
```
* **HTTP Status**: `401 Unauthorized`
* **Response Body**:
  ```json
  {
    "message": "Full authentication is required to access this resource",
    "error": "Unauthorized",
    "path": "/api/v1/auth/dev/users",
    "status": 401
  }
  ```

### 3.2 Authenticated Non-Admin Request Attempt
```http
GET /api/v1/auth/dev/users
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
* [evidence-log.txt](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/AFTER/V01-exposed-development-endpoints/evidence-log.txt)
