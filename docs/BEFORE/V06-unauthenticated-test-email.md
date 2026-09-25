# V06: Unauthenticated External Side-Effect via Security Matcher Path Bypass

## 1. Vulnerability Overview
* **ID**: `V06`
* **Title**: Unauthenticated External Side-Effect via Security Matcher Path Bypass (`GET /test-email`)
* **Severity Rationale**: **MEDIUM / HIGH**. Spring Security's filter chain is narrowly configured to match only the `/api/**` URL namespace via `.securityMatcher("/api/**")`. Consequently, any controller routes mounted outside `/api/**` bypass Spring Security authentication and authorization filters entirely. The endpoint `GET /test-email` is mapped under `TestEmailController` (`@RequestMapping("/test-email")`) and immediately triggers an outbound SMTP email dispatch to a hardcoded recipient without any authentication or rate limiting. Any unauthenticated anonymous attacker can repeatedly issue GET requests to trigger external email delivery, flood recipient mailboxes, exhaust server SMTP quotas, and cause server resource exhaustion.
* **Affected Endpoint**: `/test-email`
* **HTTP Method**: `GET`
* **Authentication Requirement**: **None** (Completely unauthenticated due to Spring Security matcher omission).

---

## 2. Source-Code Location & Implementation Analysis

### 2.1 Security Matcher Configuration
In `SecurityConfig.java`:
* **File Link**: [SecurityConfig.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/SecurityConfig.java#L33-L41)
* **Code Snippet**:
  ```java
  @Bean
  public SecurityFilterChain apiFilterChain(HttpSecurity http) {
     return http
              .securityMatcher("/api/**") // ONLY PROTECTS /api/**
              .csrf(csrf -> csrf.disable())
              .authorizeHttpRequests(auth -> auth
                      .requestMatchers("/api/v1/auth/**").permitAll()
                      .anyRequest().authenticated()
              )
              ...
  }
  ```

### 2.2 Controller Implementation
In `TestEmailController.java`:
* **File Link**: [TestEmailController.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/controller/TestEmailController.java#L9-L21)
* **Code Snippet**:
  ```java
  @RestController
  @RequestMapping("/test-email")
  @RequiredArgsConstructor
  public class TestEmailController {

      private final EmailService emailService;

      @GetMapping
      public String testEmail() {
          emailService.sendEmail("faresayman5453@gmail.com", "Test Email", "This is a test email.");
          return "Email sent!";
      }
  }
  ```
* **Root Cause Analysis**: The security filter chain applies exclusively to `/api/**`. Non-API routes like `/test-email` are not covered by any security filter chain and fall through with no authentication or authorization checks. The controller executes active side-effects (SMTP email dispatch) unconditionally on every GET request.

---

## 3. Runtime Verification & Test Evidence

### 3.1 Exact Test Request
```bash
curl -s -w "\nHTTP_STATUS:%{http_code}\n" http://localhost:8080/test-email
```
*(No `Authorization` header provided)*

### 3.2 Actual Response Summary
* **HTTP Status Code**: `200 OK`
* **Response Content-Type**: `text/plain;charset=UTF-8`
* **Response Body**:
  ```
  Email sent!
  ```
* **Server Log Confirmation**:
  Tomcat `DispatcherServlet` successfully mapped `GET /test-email` to `TestEmailController#testEmail()`, and an SMTP message transaction was dispatched to the local mail server (`localhost:1025`).

---

## 4. Security Assessment & Classification

### 4.1 Vulnerability Confirmation
* **Status**: **CONFIRMED VULNERABILITY**
* **Verification Detail**: Unauthenticated HTTP GET request executed against `/test-email` returned HTTP `200 OK`, printed `"Email sent!"`, and triggered an outbound SMTP connection confirmed in server and mail sink logs.

### 4.2 Security Impact
1. **Unrestricted External Side Effects**: Remote anonymous users can trigger outbound emails repeatedly without credentials or throttle limits.
2. **Mail Flooding & Reputation Damage**: Attackers can flood the hardcoded email address, risking domain blacklisting for the sending server and exhaustion of email service quotas.
3. **Architectural Security Bypass**: Demonstrates a fundamental flaw in Spring Security path matching where endpoints outside `/api/**` are left completely exposed to the public internet.

### 4.3 Classification Standards
* **OWASP Top 10 Category**: `A05:2021 – Security Misconfiguration` / `A01:2021 – Broken Access Control`
* **CWE Identifier**: `CWE-306` (Missing Authentication for Critical Function) / `CWE-284` (Improper Access Control)

### 4.4 Distinction from Other Findings
* **Distinctive Aspect**: `V06` is caused by an architectural Spring Security configuration flaw (`.securityMatcher("/api/**")`) that leaves endpoints outside the `/api` namespace completely unguarded, combined with an active debug controller executing external side-effects. It is distinct from `V01` (which is inside `/api/v1/auth/**` and leaks database records).

---

## 5. Remediation & Retest Guidance

### 5.1 Recommended Fix Direction
1. **Remove Test Controller**: Delete `TestEmailController.java` completely from production builds.
2. **Comprehensive Security Filter Chain**: Apply Spring Security to all application routes, or configure a fallback filter chain requiring authentication for any non-API endpoints (`auth.anyRequest().authenticated()`).
3. **Restrict Matcher**: If test utilities are required in development, gate them behind Spring profiles (e.g. `@Profile("dev")`) and enforce `ROLE_ADMIN` authentication.

### 5.2 Exact Retest Procedure
1. Send unauthenticated request:
   ```bash
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" http://localhost:8080/test-email
   ```
2. **Expected Secure Result**:
   - HTTP Status: `404 Not Found` (if deleted) or `401 Unauthorized` / `403 Forbidden`.
   - No email dispatched to the SMTP server.
