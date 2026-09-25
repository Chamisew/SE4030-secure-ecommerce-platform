# V01: Unauthenticated Access to Development User-List Endpoint

## 1. Vulnerability Overview
* **ID**: `V01`
* **Title**: Unauthenticated Access to Development User-List Endpoint Exposing Sensitive User Data
* **Severity Rationale**: **HIGH** / **CRITICAL**. The endpoint `/api/v1/auth/dev/users` is accessible to any anonymous, unauthenticated user on the internet. It dumps the entire user database, including user IDs, email addresses, assigned roles, and raw BCrypt password hashes. This allows attackers to perform account discovery, map out administrative accounts, and extract password hashes for offline dictionary/brute-force attacks.
* **Affected Endpoint**: `/api/v1/auth/dev/users`
* **HTTP Method**: `GET`
* **Authentication Requirement**: **None** (Publicly accessible due to wildcard `permitAll()` in Spring Security configuration).

---

## 2. Source-Code Location & Security Configuration

### 2.1 Controller Implementation
The development endpoint is declared in `AuthController.java`:
* **File Link**: [AuthController.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/controller/AuthController.java#L274-L277)
* **Code Snippet**:
  ```java
  @GetMapping("/dev/users")
  public ResponseEntity<List<Users>> listUsers() {
      return ResponseEntity.ok((authService).getAllUsers());
  }
  ```

### 2.2 Security Configuration
Spring Security is configured in `SecurityConfig.java`:
* **File Link**: [SecurityConfig.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/SecurityConfig.java#L37-L40)
* **Code Snippet**:
  ```java
  .authorizeHttpRequests(auth -> auth
          // public auth endpoints
          .requestMatchers("/api/v1/auth/**").permitAll()
          .anyRequest().authenticated()
  )
  ```
* **Root Cause Analysis**: The security matcher `/api/v1/auth/**` uses a wildcard matching rule that marks all endpoints under `/api/v1/auth/` as public (`permitAll()`). Because `/dev/users` resides under `AuthController` (`@RequestMapping("/api/v1/auth")`), it inherits public access. In addition, the controller returns the raw JPA `Users` entity directly, causing Jackson to serialize sensitive entity fields (including `password`) into the JSON response.

---

## 3. Runtime Verification & Test Evidence

### 3.1 Exact Test Request
```bash
curl -s -w "\nHTTP_STATUS:%{http_code}\n" http://localhost:8080/api/v1/auth/dev/users
```
*(No `Authorization` header provided)*

### 3.2 Actual Response Summary
* **HTTP Status Code**: `200 OK`
* **Response Body Structure**: JSON Array containing full `Users` entity representations for all registered users in the database.
* **Number of User Records Returned**: All registered user accounts (e.g. 5 records during functional baseline testing).

### 3.3 Sensitive Fields Exposed
The response body explicitly returns the following sensitive attributes for every account:
1. `password` — The hashed password string (e.g., `$2a$10$...` BCrypt hash).
2. `email` — Full email address (enables account enumeration).
3. `role` — System role (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_SELLER`).
4. `id` — Database UUID primary key.
5. `verificationCode` / `resetPasswordToken` — Security tokens.
6. `authorities` — Spring Security granted authority arrays.

---

## 4. Security Assessment & Classification

### 4.1 Vulnerability Confirmation
* **Status**: **CONFIRMED VULNERABILITY**
* **Verification Detail**: Executing an unauthenticated HTTP GET request against the endpoint on a running instance returned HTTP `200 OK` and dumped all user records, complete with BCrypt password hashes.

### 4.2 Security Impact
1. **User Database Leakage**: Anonymous attackers can scrape all registered email addresses, user UUIDs, and account roles.
2. **Targeted Administrative Attacks**: Attackers can instantly identify all high-privilege `ROLE_ADMIN` accounts for targeted attacks.
3. **Password Hash Exposure**: Access to BCrypt password hashes permits offline cracking attempts using rainbow tables or dictionary tools (e.g., Hashcat/John the Ripper) without triggering rate limits or lockouts on the server.

### 4.3 Classification Standards
* **OWASP Top 10 Category**: `A01:2021 – Broken Access Control` / `A05:2021 – Security Misconfiguration`
* **CWE Identifier**: `CWE-200` (Exposure of Sensitive Information to an Unauthorized Actor) / `CWE-306` (Missing Authentication for Critical Function)

### 4.4 Distinction from Other Vulnerabilities
* **Distinctive Aspect**: `V01` specifically targets an unauthenticated development/debug endpoint (`/api/v1/auth/dev/users`) that leaks full entity records including password hashes. It differs from registration role manipulation (which elevates privileges during signup) or IDOR vulnerabilities (which involve authenticated parameter manipulation on user-owned resource IDs).

---

## 5. Remediation & Retest Guidance

### 5.1 Recommended Fix Direction
1. **Remove or Restrict Development Endpoint**: Remove `/dev/users` from production builds, or restrict access exclusively to authenticated `ROLE_ADMIN` users (`@PreAuthorize("hasRole('ADMIN')")`).
2. **Tighten Security Matching Rules**: Exclude `/dev/**` routes from `permitAll()` matchers in `SecurityConfig.java`.
3. **DTO Abstraction**: Do not expose raw JPA `Users` entities in REST responses. Use a dedicated `UserResponse` DTO that excludes sensitive fields like `password`, `verificationCode`, and `resetPasswordToken`.

### 5.2 Exact Retest Procedure
1. Send an unauthenticated GET request:
   ```bash
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" http://localhost:8080/api/v1/auth/dev/users
   ```
2. **Expected Secure Result**:
   - HTTP Status: `401 Unauthorized` or `403 Forbidden` (or `404 Not Found` if endpoint is removed).
   - No user records, emails, or password hashes returned in the response body.
