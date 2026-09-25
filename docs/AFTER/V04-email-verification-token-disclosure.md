# V04: Email Verification Token Disclosed in Response Body (AFTER FIX)

## 1. Vulnerability & Fix Overview

* **Vulnerability ID**: `V04`
* **Vulnerability Title**: Email Verification Token Disclosed in HTTP Registration Response Body
* **Assigned Member**: Member 1 — Registration Specialist (`Chamila Sewmini <chamilasewmini2@gmail.com>`)
* **Branch**: `fix/A-registration-vulns`
* **CWE**: CWE-200 (Exposure of Sensitive Information to an Unauthorized Actor), CWE-640 (Weak Password Recovery / Verification Mechanism)
* **OWASP Top 10**: A07:2021 – Identification and Authentication Failures
* **Fix Status**: **VERIFIED & REMEDIATED**

---

## 2. Root Cause & Code Remediation

### 2.1 The Flaw (BEFORE)
In the original implementation:
1. `AuthServiceImpl.register()` generated an account verification token `UUID.randomUUID().toString()`.
2. Although it sent an email to the user, it also constructed:
   ```java
   return new RegisterResponse("User registered. Please check your email for verification.", token);
   ```
3. `RegisterResponse.java` exposed `private String verificationToken;`, which was serialized directly into the HTTP response.
4. Any caller (including an attacker registering with an email address they did not own) immediately received the secret token, bypassing email verification entirely.

### 2.2 The Remediation (AFTER)
1. **Removed token from response DTO**:
   In [RegisterResponse.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/dto/Responses/RegisterResponse.java), deleted `private String verificationToken;` and the constructor accepting the token.
2. **Updated service response**:
   In [AuthServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/service/Impl/AuthServiceImpl.java#L125-L135), modified the return statement:
   ```java
   // V04 Fix: Do not disclose verificationToken in the HTTP response body
   return new RegisterResponse("User registered. Please check your email for verification.");
   ```
3. The token is now only transmitted out-of-band to the target email inbox via SMTP.

---

## 3. Re-Testing & Verification Evidence

### 3.1 Test Execution
A registration request was submitted:
```bash
curl.exe -s -i -X POST http://localhost:8080/api/v1/auth/register \
  -F "firstName=AfterFix" \
  -F "lastName=MemberOne" \
  -F "email=afterfix_member1_v03@test.com" \
  -F "password=Password123!"
```

### 3.2 Response & Verification
* **HTTP Status**: `201 Created`
* **Response Body**:
  ```json
  {"message":"User registered. Please check your email for verification.","profileImageUrl":null}
  ```
* **Result**: The `verificationToken` property is completely eliminated from the client response. Account verification can only occur if the user possesses legitimate access to the recipient email mailbox.

---

## 4. Evidence File Link

Detailed execution log with full HTTP headers and response analysis:
* [evidence-log.txt](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/AFTER/V04-email-verification-token-disclosure/evidence-log.txt)
