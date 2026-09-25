# V04: Email Verification Token Disclosure in Registration API Response

## 1. Vulnerability Overview
* **ID**: `V04`
* **Title**: Sensitive Information Disclosure via In-Band Email Verification Token Leakage in Registration Response
* **Severity Rationale**: **HIGH**. The registration endpoint (`POST /api/v1/auth/register`) creates a pending user and generates a random UUID email verification token intended to be delivered exclusively out-of-band via email. However, the server includes this raw verification token string directly in the HTTP response JSON (`RegisterResponse.verificationToken`). An attacker who registers an account using an arbitrary victim's email address can intercept the verification token directly from the HTTP response and immediately activate the account via `GET /api/v1/auth/verify-email?token=...`, completely defeating the purpose of email ownership verification and enabling arbitrary account claiming.
* **Affected Endpoint**: `/api/v1/auth/register`
* **HTTP Method**: `POST`
* **Authentication Requirement**: **None** (Publicly accessible).

---

## 2. Source-Code Location & Implementation Analysis

### 2.1 Service Implementation
The vulnerability is located in `AuthServiceImpl.java`:
* **File Link**: [AuthServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/service/Impl/AuthServiceImpl.java#L116-L134)
* **Code Snippet**:
  ```java
  // generate and store token
  String token = UUID.randomUUID().toString();

  VerificationToken verificationToken = new VerificationToken();
  verificationToken.setToken(token);
  verificationToken.setUser(user);
  verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
  verificationTokenRepo.save(verificationToken);

  // send verification email
  String verificationLink = "http://localhost:8080/api/v1/auth/verify-email?token=" + token;
  String body = "Hello " + user.getFirstName() + ",\n\n" +
          "Click the link to verify your account:\n" + verificationLink +
          "\n\nIf you did not register, ignore this email.";
  emailService.sendEmail(user.getEmail(), "Verify your account", body);

  return new RegisterResponse("User registered. Please check your email for verification." ,token);
  ```

### 2.2 Response DTO Implementation
The DTO definition exposes `verificationToken`:
* **File**: `RegisterResponse.java`
* **Constructor**: `public RegisterResponse(String message, String verificationToken)`
* **Root Cause Analysis**: The response DTO `RegisterResponse` contains a `verificationToken` field, and `AuthServiceImpl.register()` passes the generated secret token directly into the constructor. Consequently, the secret authentication token is serialized into the public HTTP response payload.

---

## 3. Runtime Verification & Test Evidence

### 3.1 Exact Test Request
1. Registration request for arbitrary victim email:
   ```bash
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://localhost:8080/api/v1/auth/register \
     -F "firstName=Victim" \
     -F "lastName=Target" \
     -F "email=victim_account@test.com" \
     -F "password=[REDACTED_PASSWORD]"
   ```

2. Immediate verification using the leaked token:
   ```bash
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" \
     "http://localhost:8080/api/v1/auth/verify-email?token=[REDACTED_LEAKED_TOKEN]"
   ```

3. Immediate login of verified victim account:
   ```bash
   curl -s -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"victim_account@test.com","password":"[REDACTED_PASSWORD]"}'
   ```

### 3.2 Actual Response Summary
* **Registration HTTP Status**: `201 Created`
* **Registration Body**:
  ```json
  {
    "message": "User registered. Please check your email for verification.",
    "profileImageUrl": null,
    "verificationToken": "827e434f-472d-4e8a-bd41-bc5b3b4e1d61"
  }
  ```
* **Verification HTTP Status**: `200 OK`
* **Verification Body**: `{"message":"Email verified successfully!"}`
* **Login HTTP Status**: `200 OK`
* **Login Body**: `{"message":"Login successful", "accessToken":"...", ...}`

---

## 4. Security Assessment & Classification

### 4.1 Vulnerability Confirmation
* **Status**: **CONFIRMED VULNERABILITY**
* **Verification Detail**: Calling `POST /api/v1/auth/register` returned HTTP `201 Created` with the raw verification UUID token in the JSON body. Submitting this token to `GET /api/v1/auth/verify-email` activated the account immediately without interacting with any mailbox.

### 4.2 Security Impact
1. **Email Verification Bypass**: Completely nullifies the security guarantee of email verification. Anyone can register accounts under arbitrary corporate, academic, or personal email addresses and verify them instantaneously.
2. **Account Pre-hijacking & Denial of Service**: Malicious actors can pre-register and verify accounts for legitimate domain users before they sign up, locking legitimate users out or harvesting data.

### 4.3 Classification Standards
* **OWASP Top 10 Category**: `A07:2021 – Identification and Authentication Failures`
* **CWE Identifier**: `CWE-200` (Exposure of Sensitive Information to an Unauthorized Actor) / `CWE-306` (Missing Authentication for Critical Function)

### 4.4 Distinction from Other Findings
* **Distinctive Aspect**: `V04` represents an in-band information disclosure of an account activation token in `POST /api/v1/auth/register`. It is distinct from `V03` (which is a privilege escalation flaw via client-controlled role assignment in the same endpoint), and distinct from `V02` (which is a password-reset OTP disclosure in `POST /api/v1/auth/forgot-password`).

---

## 5. Remediation & Retest Guidance

### 5.1 Recommended Fix Direction
1. **Remove Token from DTO**: Delete the `verificationToken` field from `RegisterResponse` or set it to `null`.
2. **Ensure Exclusive Out-of-Band Delivery**: Deliver verification tokens solely via email to the registered address.

### 5.2 Exact Retest Procedure
1. Send registration request:
   ```bash
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://localhost:8080/api/v1/auth/register \
     -F "firstName=Audit" -F "lastName=User" -F "email=retest_user@test.com" -F "password=TestPass123!"
   ```
2. **Expected Secure Result**:
   - HTTP Status: `201 Created`.
   - Response body contains only a status message:
     ```json
     {
       "message": "User registered. Please check your email for verification."
     }
     ```
   - No `verificationToken` or sensitive token attributes appear in the JSON response.
