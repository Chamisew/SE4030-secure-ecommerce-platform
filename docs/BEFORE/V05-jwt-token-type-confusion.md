# V05: JWT Token-Type Confusion & Missing Token Revocation / Rotation

## 1. Vulnerability Overview
* **ID**: `V05`
* **Title**: JWT Token-Type Confusion and Absence of Refresh Token Revocation / Rotation
* **Severity Rationale**: **HIGH**. The application uses JSON Web Tokens (JWT) for authentication with separate access and refresh tokens. However, the token generation routine does not embed any `token_type` or `typ` claim distinguishing access tokens from refresh tokens. Furthermore, `JWTFilter` validates any signed token and extracts user roles without verifying token intent. Consequently, long-lived refresh tokens (7-day validity) can be submitted directly as bearer access tokens to invoke protected endpoints. In addition, refresh tokens are never rotated on token refresh (the exact same token is reissued), and logout performs no server-side invalidation.
* **Affected Endpoints**: Protected API routes (e.g., `GET /api/v1/auth/me`), `POST /api/v1/auth/refresh-token`, `POST /api/v1/auth/logout`
* **HTTP Method**: `GET`, `POST`
* **Authentication Requirement**: Bearer Token / Refresh Token payload

---

## 2. Source-Code Location & Implementation Analysis

### 2.1 Token Generation Without Type Claim
In `JWTserviceImpl.java`:
* **File Link**: [JWTserviceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/service/Impl/JWTserviceImpl.java#L51-L85)
* **Code Snippet**:
  ```java
  // generating access token
  @Override
  public String generateToken(Users user) {
      Map<String, Object> claims = new HashMap<>();
      claims.put("role", user.getRole().name());
      Set<Role> roles = Set.of(user.getRole());
      return buildToken(claims, user.getEmail(), roles, ACCESS_TOKEN_EXPIRATION);
  }

  // generating refresh token
  @Override
  public String generateRefreshToken(Users user) {
      Map<String, Object> claims = new HashMap<>();
      claims.put("role", user.getRole().name());
      Set<Role> roles = Set.of(user.getRole());
      return buildToken(claims, user.getEmail(), roles, REFRESH_TOKEN_EXPIRATION);
  }
  ```

### 2.2 Security Filter Lacks Token Type Validation
In `JWTFilter.java`:
* **File Link**: [JWTFilter.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/jwt/JWTFilter.java#L47-L54)
* **Code Snippet**:
  ```java
  if (jwtservice.validateToken(token, userDetails)) {
      var claims = jwtservice.extractRoles(token);
      var authorities = claims.stream().map(SimpleGrantedAuthority::new).toList();
      var authToken = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authToken);
  }
  ```

### 2.3 Absence of Token Rotation and Revocation
In `AuthServiceImpl.java`:
* **File Link**: [AuthServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/service/Impl/AuthServiceImpl.java#L249-L274)
* **Code Snippet**:
  ```java
  // Generate a NEW access token
  String newAccessToken = jwtService.generateToken(user);

  return new LoginResponse(
          "Token refreshed successfully",
          newAccessToken,
          refreshTokenReq.getToken(), // reuse same refresh token (NO ROTATION)
          user.getEmail(),
          user.getFirstName(),
          user.getLastName(),
          user.getProfileImageUrl(),
          user.getRole()
  );

  // Logout implementation:
  // In a stateless JWT setup, logout is handled client-side by deleting tokens.
  // No server-side action is required.
  ```
* **Root Cause Analysis**: The application fails to enforce token segregation. Both access tokens and refresh tokens share the identical HMAC-SHA signing key, identical claims format, and lack a distinguishing claim (e.g. `token_type: "ACCESS"` vs `token_type: "REFRESH"`). `JWTFilter` evaluates only token signature and expiration, allowing refresh tokens to impersonate access tokens. Furthermore, the refresh token is never rotated upon use, and logout does not maintain a revocation blocklist.

---

## 3. Runtime Verification & Test Evidence

### 3.1 Step-by-Step Test Procedure
1. **User Setup & Login**:
   Registered and verified disposable account `audit_v05_user@test.com`, logging in via `POST /api/v1/auth/login` to obtain an `accessToken` and `refreshToken`.

2. **Test 1: Refresh Token as Bearer Access Token**:
   Submit the `refreshToken` directly in the `Authorization: Bearer` header on protected route `GET /api/v1/auth/me`:
   ```bash
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" \
     -H "Authorization: Bearer [REDACTED_REFRESH_TOKEN]" \
     http://localhost:8080/api/v1/auth/me
   ```

3. **Test 2: Access Token Submitted to Refresh Endpoint**:
   Submit the `accessToken` in the JSON body to `POST /api/v1/auth/refresh-token`:
   ```bash
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://localhost:8080/api/v1/auth/refresh-token \
     -H "Content-Type: application/json" \
     -d '{"token":"[REDACTED_ACCESS_TOKEN]"}'
   ```

4. **Test 3: Refresh Token Rotation Check**:
   Submit the `refreshToken` to `POST /api/v1/auth/refresh-token` and compare the returned `refreshToken` against the submitted token:
   ```bash
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://localhost:8080/api/v1/auth/refresh-token \
     -H "Content-Type: application/json" \
     -d '{"token":"[REDACTED_REFRESH_TOKEN]"}'
   ```

5. **Test 4: Post-Logout Token Validity Check**:
   Call `POST /api/v1/auth/logout` with `Authorization: Bearer [REDACTED_ACCESS_TOKEN]`.
   Subsequently issue `GET /api/v1/auth/me` with both the access token and refresh token:
   ```bash
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://localhost:8080/api/v1/auth/logout \
     -H "Authorization: Bearer [REDACTED_ACCESS_TOKEN]"
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" -H "Authorization: Bearer [REDACTED_ACCESS_TOKEN]" http://localhost:8080/api/v1/auth/me
   ```

### 3.2 Actual Response Summary
* **Test 1 (`GET /api/v1/auth/me` using Refresh Token)**:
  - HTTP Status: `200 OK`
  - Response Body:
    ```json
    {
      "email": "audit_v05_user@test.com",
      "firstName": "V05User",
      "lastName": "Audit",
      "profileImageUrl": null
    }
    ```
  - *Result*: Refresh token accepted as a valid access token.
* **Test 2 (`POST /api/v1/auth/refresh-token` using Access Token)**:
  - HTTP Status: `200 OK`
  - Response Body: New access token issued successfully.
  - *Result*: Access token accepted at refresh endpoint.
* **Test 3 (Rotation Check)**:
  - HTTP Status: `200 OK`
  - Response Body: Returns the exact same refresh token string character-for-character.
  - *Result*: Zero rotation implemented; token reuse is indefinitely allowed until expiry.
* **Test 4 (Logout Check)**:
  - `POST /api/v1/auth/logout`: Returns `200 OK` (`"Logged out successfully"`).
  - Subsequent `GET /api/v1/auth/me`: Returns `200 OK` with full profile data for both access token and refresh token.
  - *Result*: Tokens are not revoked or blacklisted on logout.

---

## 4. Security Assessment & Classification

### 4.1 Vulnerability Confirmation
* **Status**: **CONFIRMED VULNERABILITY**
* **Verification Detail**: Confirmed via runtime execution that refresh tokens function interchangeably as access tokens on protected endpoints, access tokens are accepted at the refresh endpoint, refresh tokens are never rotated upon refresh, and tokens remain fully valid after user logout.

### 4.2 Security Impact
1. **Compromise Window Expansion**: Refresh tokens typically have longer lifetimes (7 days). If intercepted (e.g. from local storage or logs), an attacker can directly use the refresh token to access protected APIs without ever needing to perform a token exchange.
2. **Session Invalidation Failure**: Users cannot terminate active sessions upon logout. Intercepted tokens remain valid until expiration regardless of user logout.

### 4.3 Classification Standards
* **OWASP Top 10 Category**: `A07:2021 – Identification and Authentication Failures` / `A02:2021 – Cryptographic Failures`
* **CWE Identifier**: `CWE-287` (Improper Authentication) / `CWE-613` (Insufficient Session Expiration)

### 4.4 Distinction from Other Findings
* **Distinctive Aspect**: `V05` resides within the stateless JWT token architecture and Spring Security request filter (`JWTFilter`), specifically involving token-type confusion and missing token lifecycle management. It is entirely distinct from API endpoint authorization flaws (`V01`, `V03`), secret disclosures (`V02`, `V04`), or business logic flaws (`V08`).

---

## 5. Remediation & Retest Guidance

### 5.1 Recommended Fix Direction
1. **Embed Token-Type Claim**: Add `claims.put("token_type", "ACCESS")` and `claims.put("token_type", "REFRESH")` in `JWTserviceImpl`.
2. **Enforce Token Type in Filter**: In `JWTFilter`, verify that `token_type` equals `"ACCESS"` before authenticating the request.
3. **Enforce Token Type in Refresh Service**: In `AuthServiceImpl.refreshToken()`, verify that `token_type` equals `"REFRESH"`.
4. **Implement Refresh Token Rotation**: Invalidate old refresh tokens upon exchange and store a token family or blacklist identifier.

### 5.2 Exact Retest Procedure
1. Submit refresh token to `GET /api/v1/auth/me`:
   ```bash
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" \
     -H "Authorization: Bearer <refresh_token>" \
     http://localhost:8080/api/v1/auth/me
   ```
2. **Expected Secure Result**:
   - HTTP Status: `401 Unauthorized`.
   - Error message indicates invalid token type or insufficient authentication.
