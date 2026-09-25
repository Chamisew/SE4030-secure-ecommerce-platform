# V02: Forgot-Password OTP Disclosure in API Response

## 1. Vulnerability Overview
* **ID**: `V02`
* **Title**: Sensitive Information Disclosure via Password-Reset OTP Leak in API Response
* **Severity Rationale**: **HIGH**. The endpoint `/api/v1/auth/forgot-password` generates a 6-digit numeric One-Time Password (OTP) for password resets and returns the raw `otp` value directly in the HTTP JSON response body (`ForgetPasswordResponse`). An unauthenticated attacker who knows or guesses a target user's email address can initiate a password reset request and intercept the valid OTP from the server response, completely bypassing the secondary out-of-band email verification channel.
* **Affected Endpoint**: `/api/v1/auth/forgot-password`
* **HTTP Method**: `POST`
* **Authentication Requirement**: **None** (Publicly accessible endpoint matching `/api/v1/auth/**`).

---

## 2. Source-Code Location & Implementation Analysis

### 2.1 Controller Implementation
The endpoint is defined in `AuthController.java`:
* **File Link**: [AuthController.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/controller/AuthController.java#L248-L251)
* **Code Snippet**:
  ```java
  @PostMapping("/forgot-password")
  public ResponseEntity<ForgetPasswordResponse> forgotPassword(@RequestBody Map<String,String> body) {
      return ResponseEntity.ok(authService.forgotPassword(body.get("email")));
  }
  ```

### 2.2 Service Implementation
The business logic is implemented in `AuthServiceImpl.java`:
* **File Link**: [AuthServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/service/Impl/AuthServiceImpl.java#L513-L553)
* **Code Snippet**:
  ```java
  // generate 6-digit OTP
  String otp = String.format("%06d", new Random().nextInt(1_000_000));
  ...
  // Save OTP in the database
  OTP otpEntity = new OTP();
  otpEntity.setUser(user);
  otpEntity.setOtp(otp);
  otpEntity.setExpiryDate(LocalDateTime.now().plusMinutes(15));
  otpRepository.save(otpEntity);

  // send email
  String body = "Hello " + user.getFirstName() + ",\n\n" +
          "Your password reset code (OTP) is: " + otp + "\n\n" +
          "This code expires in 15 minutes.";
  emailService.sendEmail(user.getEmail(), "Password Reset OTP", body);

  return new ForgetPasswordResponse("OTP sent successfully. Please check your inbox." , otp);
  ```

### 2.3 Response DTO
The DTO definition exposes `otp` as a constructor parameter and public field:
* **DTO File**: `ForgetPasswordResponse.java`
* **Constructor**: `public ForgetPasswordResponse(String message, String otp)`

---

## 3. Runtime Verification & Test Evidence

### 3.1 Exact Test Request
```bash
curl -s -w "\nHTTP_STATUS:%{http_code}\n" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"func_user@example.com\"}" \
  http://localhost:8080/api/v1/auth/forgot-password
```

### 3.2 Actual Response Summary
* **HTTP Status Code**: `200 OK`
* **Response Body Structure**:
  ```json
  {
    "message": "OTP sent successfully. Please check your inbox.",
    "otp": "[REDACTED_OTP]"
  }
  ```

### 3.3 Email Delivery Status
* **Attempted**: Yes (sent to `func_user@example.com`).
* **Delivery Status**: Succeeded via local SMTP server (`localhost:1025`).
* **Note**: The email delivery succeeded, but the vulnerability arises because the server *also* includes the generated OTP directly in the API response JSON.

### 3.4 Sensitive Information Exposed
* The secret 6-digit `otp` string required to reset the account password.

---

## 4. Security Assessment & Classification

### 4.1 Vulnerability Confirmation
* **Status**: **CONFIRMED VULNERABILITY**
* **Verification Detail**: Runtime test against the live backend returned HTTP `200 OK` with a JSON payload explicitly containing the `"otp"` field alongside the confirmation message.

### 4.2 Security Impact
1. **Account Takeover (ATO)**: Any unauthenticated actor can issue a `POST /api/v1/auth/forgot-password` request for any target user's email address, read the valid OTP from the API response payload, and immediately call `POST /api/v1/auth/reset-password` to overwrite the target account's password.
2. **Bypass of Out-of-Band Channel**: The multi-factor / email verification control is completely negated because the secret code is exposed in band in the response.

### 4.3 Classification Standards
* **OWASP Top 10 Category**: `A07:2021 – Identification and Authentication Failures` / `A01:2021 – Broken Access Control`
* **CWE Identifier**: `CWE-640` (Weak Password Recovery Mechanism for Forgotten Password) / `CWE-200` (Exposure of Sensitive Information to an Unauthorized Actor)

### 4.4 Distinction from V01
* **V01 (Dev Users Endpoint)**: Involves unauthenticated access to a bulk user-dump development route (`/api/v1/auth/dev/users`) exposing stored BCrypt password hashes and account metadata.
* **V02 (Forgot-Password OTP Disclosure)**: Involves an architectural defect in the legitimate password reset workflow (`/api/v1/auth/forgot-password`), where an active verification secret (OTP) is leaked in the HTTP response body during a reset request.

---

## 5. Remediation & Retest Guidance

### 5.1 Recommended Fix Direction
1. **Remove OTP from DTO / Service Return**: Update `AuthServiceImpl.forgotPassword()` to return a response containing only a generic success message (`new ForgetPasswordResponse("OTP sent successfully. Please check your inbox.")` or a `MessageResponse`).
2. **Refactor Response DTO**: Remove the `otp` field from `ForgetPasswordResponse` or eliminate the DTO if redundant.
3. **Ensure Email-Only Delivery**: Ensure the OTP secret is delivered exclusively through out-of-band email notifications.

### 5.2 Exact Retest Procedure
1. Issue the test request:
   ```bash
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" \
     -H "Content-Type: application/json" \
     -d "{\"email\":\"func_user@example.com\"}" \
     http://localhost:8080/api/v1/auth/forgot-password
   ```
2. **Expected Secure Result**:
   - HTTP Status: `200 OK`
   - Response JSON body:
     ```json
     {
       "message": "OTP sent successfully. Please check your inbox."
     }
     ```
   - No `"otp"` field or secret reset code present in the JSON body.
