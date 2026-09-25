# V02: Forgot-Password OTP Disclosed in API Response (AFTER FIX)

## 1. Vulnerability & Fix Overview

* **Vulnerability ID**: `V02`
* **Vulnerability Title**: Sensitive Information Disclosure via Password-Reset OTP Leak in API Response
* **Assigned Member**: Member 2 — Token & Cryptography Specialist (`Sammani Wimalarathna <wimalarathnasammani@gmail.com>`)
* **Branch**: `fix/B-token-vulns`
* **CWE**: CWE-200 (Exposure of Sensitive Information to an Unauthorized Actor), CWE-330 (Use of Insufficiently Random Values)
* **OWASP Top 10**: A07:2021 – Identification and Authentication Failures, A02:2021 – Cryptographic Failures
* **Fix Status**: **VERIFIED & REMEDIATED**

---

## 2. Root Cause & Code Remediation

### 2.1 The Flaw (BEFORE)
1. `AuthServiceImpl.forgotPassword()` generated a 6-digit OTP using `java.util.Random`, which is cryptographically insecure and predictable.
2. The service returned:
   ```java
   return new ForgetPasswordResponse("OTP sent successfully. Please check your inbox.", otp);
   ```
3. `ForgetPasswordResponse.java` defined `private String otp;` which was serialized into the JSON response body.
4. Any unauthenticated caller could query the endpoint with a victim's email address and intercept the valid OTP directly from the response.

### 2.2 The Remediation (AFTER)
1. **Upgraded to `SecureRandom`**:
   In [AuthServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/service/Impl/AuthServiceImpl.java#L534-L540), replaced `new Random()` with `java.security.SecureRandom`:
   ```java
   // V02 Fix: Use cryptographically strong SecureRandom instead of insecure Random (CWE-330)
   String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
   ```
2. **Removed OTP from Response DTO**:
   In [ForgetPasswordResponse.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/dto/Responses/ForgetPasswordResponse.java), deleted the `private String otp;` field.
3. **Updated Service Return**:
   In [AuthServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/service/Impl/AuthServiceImpl.java#L550-L555), modified the return to emit only the success message:
   ```java
   // V02 Fix: Do not disclose OTP in the HTTP response body
   return new ForgetPasswordResponse("OTP sent successfully. Please check your inbox.");
   ```

---

## 3. Re-Testing & Verification Evidence

### 3.1 Test Execution
A password reset request was triggered for an existing account:
```bash
POST /api/v1/auth/forgot-password
Content-Type: application/json

{"email": "afterfix_member1_v03@test.com"}
```

### 3.2 Response & Verification
* **HTTP Status**: `200 OK`
* **Response Body**:
  ```json
  {"message":"OTP sent successfully. Please check your inbox."}
  ```
* **Result**: The `otp` field is completely absent from the JSON response. The OTP can only be retrieved by accessing the user's registered email inbox.

---

## 4. Evidence File Link

Detailed execution log with full runtime output:
* [evidence-log.txt](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/AFTER/V02-missing-rate-limiting-otp/evidence-log.txt)
