# V05: JWT Token-Type Confusion (AFTER FIX)

## 1. Vulnerability & Fix Overview

* **Vulnerability ID**: `V05`
* **Vulnerability Title**: JWT Token-Type Confusion Allowing Long-Lived Refresh Tokens to Act as Bearer Access Tokens
* **Assigned Member**: Member 2 — Token & Cryptography Specialist (`Sammani Wimalarathna <wimalarathnasammani@gmail.com>`)
* **Branch**: `fix/B-token-vulns`
* **CWE**: CWE-287 (Improper Authentication), CWE-613 (Insufficient Session Expiration)
* **OWASP Top 10**: A07:2021 – Identification and Authentication Failures
* **Fix Status**: **VERIFIED & REMEDIATED**

---

## 2. Root Cause & Code Remediation

### 2.1 The Flaw (BEFORE)
1. In `JWTserviceImpl.java`, access tokens had an excessive lifetime of **60 days**, and refresh tokens had a lifetime of **7 days**.
2. Both token types shared the same cryptographic signing key, structure, and claim sets with **no `token_type` claim**.
3. In `JWTFilter.java`, any syntactically valid and unexpired JWT was accepted as authentication for protected API resources. A client could use a refresh token indefinitely as a bearer token, defeating token lifecycle management.

### 2.2 The Remediation (AFTER)
1. **Reduced Access Token Validity**:
   In [JWTserviceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/service/Impl/JWTserviceImpl.java#L40), reduced `ACCESS_TOKEN_EXPIRATION` to **15 minutes** (`15 * 60 * 1000`).
2. **Introduced Explicit `token_type` Claim**:
   - `generateToken()` sets `claims.put("token_type", "access");`
   - `generateRefreshToken()` sets `claims.put("token_type", "refresh");`
3. **Enforced Type Validation in `JWTFilter`**:
   In [JWTFilter.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/jwt/JWTFilter.java#L47-L56), after signature validation:
   ```java
   String tokenType = jwtservice.extractTokenType(token);
   if (!"access".equalsIgnoreCase(tokenType)) {
       throw new InsufficientAuthenticationException(
           "Invalid token type: refresh tokens cannot be used to authenticate API requests"
       );
   }
   ```
4. **Enforced Type Validation in Refresh Endpoint**:
   In [AuthServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/service/Impl/AuthServiceImpl.java#L244-L248), `/refresh-token` ensures the submitted token has `token_type == "refresh"`.

---

## 3. Re-Testing & Verification Evidence

### 3.1 Token Claims Inspection
Decoded payload of issued access and refresh tokens:
* **Access Token**:
  ```json
  {"sub":"afterfix_member1_v03@test.com","token_type":"access","exp":1758807925}
  ```
* **Refresh Token**:
  ```json
  {"sub":"afterfix_member1_v03@test.com","token_type":"refresh","exp":1759411825}
  ```

### 3.2 Exploit Attempt with Refresh Token
```http
GET /api/v1/orders
Authorization: Bearer <refreshToken>
```
* **HTTP Status**: `401 Unauthorized`
* **Response Body**:
  ```json
  {
    "path": "/api/v1/orders",
    "error": "Unauthorized",
    "message": "Authentication failed: Invalid token type: refresh tokens cannot be used to authenticate API requests",
    "status": 401
  }
  ```

### 3.3 Legitimate Request with Access Token
```http
GET /api/v1/orders
Authorization: Bearer <accessToken>
```
* **HTTP Status**: `200 OK`
* **Response Body**: `[]`

---

## 4. Evidence File Link

Detailed execution log with token decode transcripts and HTTP responses:
* [evidence-log.txt](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/AFTER/V05-jwt-token-type-confusion/evidence-log.txt)
