# AFTER-Fix Automated Security Scan: White-Box SAST Report

## Scan Metadata

| Field | Value |
| :--- | :--- |
| **Tool** | Custom SAST Pattern Scanner (Regex Rule Evaluator) |
| **Tool Category** | White-Box Static Application Security Testing (SAST) |
| **Target Codebase** | `backend/src/main/java/com/example/backend/` |
| **Scan Execution Date** | 2026-09-26T10:04:46.864801+05:30 |
| **Application State** | Patched AFTER-fix application with V01–V08 Remediations & OAuth2 |

---

## Executive Summary: Code-Level Vulnerability Remediations

| # | Vulnerability Check | Source File | BEFORE Pattern | AFTER Pattern | Audit Result |
| :-: | :--- | :--- | :--- | :--- | :---: |
| 1 | Cryptographic PRNG (V02) | `AuthServiceImpl.java` | `new Random().nextInt()` | `new SecureRandom().nextInt()` | **REMEDIATED (PASS)** |
| 2 | JWT Lifetime & Token Type (V05) | `JWTserviceImpl.java` | 60 Days, No Type Claim | 15 Minutes + `token_type` claim | **REMEDIATED (PASS)** |
| 3 | Filter Chain & Wildcard Bypass (V01, V06) | `SecurityConfig.java` | `securityMatcher` + `/auth/**` | Default-Deny + Route Whitelist | **REMEDIATED (PASS)** |
| 4 | Client Role Injection (V03) | `SignUpRequest.java` | `private Role role;` | Field Deleted (Server Default) | **REMEDIATED (PASS)** |
| 5 | Token Leakage in Response (V04) | `RegisterResponse.java` | `verificationToken` field | Field Deleted (Email Only) | **REMEDIATED (PASS)** |
| 6 | Cleartext OTP Disclosure (V02) | `ForgetPasswordResponse.java` | `private String otp;` | Field Deleted (SMTP Only) | **REMEDIATED (PASS)** |
| 7 | File Upload Restrictions (V07) | `SellerServiceImpl.java` | No Size/MIME Checks | 5MB Limit + MIME/Ext Whitelist | **REMEDIATED (PASS)** |
| 8 | Inventory Asymmetry & Inflation (V08) | `OrderServiceImpl.java` | No Stock Deduction | Stock Validation + Deduction | **REMEDIATED (PASS)** |
| 9 | Anti-Open Redirect Validation (OAuth) | `OAuth2AuthenticationSuccessHandler.java` | Unvalidated Redirection | Authorized URI Whitelist Check | **REMEDIATED (PASS)** |

---

## Detailed Rule Evaluation & Code Excerpts

### 1. Cryptographic PRNG Upgrade (V02 Remediation)
```java
// In AuthServiceImpl.java:
String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
```
*Analysis*: Eliminates CWE-330; state cannot be reconstructed by observing consecutive OTP values.

### 2. JWT Token-Type Segregation & Lifespan Shortening (V05 Remediation)
```java
// In JWTserviceImpl.java:
private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000; // 15 minutes
claims.put("token_type", "access");
```
*Analysis*: Eliminates CWE-287 / CWE-613; refresh tokens cannot be accepted as access tokens by `JWTFilter`.

### 3. Default-Deny Security Filter Chain (V01 & V06 Remediation)
```java
// In SecurityConfig.java:
// securityMatcher removed -> all routes default to secure
.requestMatchers("/api/v1/auth/dev/**", "/test-email/**").hasRole("ADMIN")
.anyRequest().authenticated()
```
*Analysis*: Eliminates CWE-306 / CWE-862; utility endpoints strictly require administrative roles.

### 4. Input & DTO Hardening (V03 & V04 Remediation)
```java
// SignUpRequest.java & RegisterResponse.java:
// Deleted: private Role role;
// Deleted: private String verificationToken;
```
*Analysis*: Eliminates CWE-269 (client privilege escalation) and CWE-200 (verification token leakage).

### 5. File Upload Pre-Validation & Non-Executable Storage (V07 Remediation)
```java
// In SellerServiceImpl.java:
if (document.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException(...);
List<String> allowedMimeTypes = List.of("application/pdf", "image/jpeg", "image/png");
if (!allowedMimeTypes.contains(contentType.toLowerCase())) throw new IllegalArgumentException(...);
```
*Analysis*: Eliminates CWE-434; strictly intercepts unauthorized binaries before cloud storage interaction.

### 6. Atomic Inventory Validation & Decrement (V08 Remediation)
```java
// In OrderServiceImpl.java:
if (p.getStock() < ci.getQuantity()) throw new ProductOutOfStockException(...);
p.setStock(p.getStock() - ci.getQuantity());
productRepository.save(p);
```
*Analysis*: Eliminates CWE-840; restores symmetry between order placement and order cancellation.

### 7. Anti-Open Redirect Protection for OAuth 2.0
```java
// In OAuth2AuthenticationSuccessHandler.java:
if (redirectUri.isPresent() && !isAuthorizedRedirectUri(redirectUri.get())) {
    throw new IllegalArgumentException("Unauthorized Redirect URI");
}
```
*Analysis*: Eliminates CWE-601; protects authorization code and token parameters from destination hijacking.

---
## Conclusion

The white-box static code audit confirms that all identified high-severity and medium-severity vulnerable patterns have been comprehensively eliminated from the source code. Defense-in-depth measures have been introduced across authentication, access control, input handling, and business logic layers.