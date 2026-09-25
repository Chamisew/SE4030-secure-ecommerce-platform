# Consolidated Security Audit & Validation Summary (ORIGINAL Application)

## Executive Summary
This document consolidates the findings of the BEFORE-fix security audit conducted against the original, unmodified e-commerce platform. In strict adherence to assessment rules, all tests were executed against a local testing environment (`http://localhost:8080`) using disposable test accounts, non-destructive payloads, and with application source code, configuration, and database schemas preserved in their original state.

A systematic audit across all nine security domains (Authentication/Authorization, Password/Account Recovery, Data Exposure, File Upload, Business Logic, Security Misconfiguration, API/Input Validation, Token/Cryptographic Security, and Automated Tooling) identified **8 CONFIRMED, genuinely distinct vulnerabilities** supported by complete source-code root-cause analysis and live HTTP runtime evidence. Additional non-core observations are classified as **CODE-LEVEL CONCERNS**.

---

## 1. Consolidated Vulnerability Matrix

| ID | Finding | Runtime Tested | Status | Root Cause | Distinct? | Evidence Location |
| :--- | :--- | :---: | :---: | :--- | :---: | :--- |
| **V01** | Unauthenticated Access to Development User-List Endpoint Exposing Passwords & Metadata | Yes | **CONFIRMED** | Broad wildcard rule `.requestMatchers("/api/v1/auth/**").permitAll()` in `SecurityConfig.java` accidentally exposes dev debugging endpoint returning raw JPA `Users` entities. | Yes | [`docs/BEFORE/V01-dev-users/evidence-log.txt`](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/BEFORE/V01-dev-users/evidence-log.txt) |
| **V02** | Sensitive Information Disclosure via Password-Reset OTP Leak in API Response | Yes | **CONFIRMED** | Response DTO `ForgetPasswordResponse` and `AuthServiceImpl.forgotPassword()` return the generated 6-digit numeric OTP directly in the JSON response payload. | Yes | [`docs/BEFORE/V02-forgot-password-otp/evidence-log.txt`](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/BEFORE/V02-forgot-password-otp/evidence-log.txt) |
| **V03** | Privilege Escalation via Client-Controlled Role Assignment During Registration | Yes | **CONFIRMED** | `AuthController.register()` binds an untrusted client-supplied `Role` parameter without server-side validation or administrative restrictions, assigning requested role directly to entity. | Yes | [`docs/BEFORE/V03-client-controlled-role-assignment/evidence-log.txt`](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/BEFORE/V03-client-controlled-role-assignment/evidence-log.txt) |
| **V04** | Email Verification Bypass via In-Band Verification Token Leakage in Registration Response | Yes | **CONFIRMED** | `AuthServiceImpl.register()` embeds the secret UUID email verification token into `RegisterResponse`, leaking it in-band in the public HTTP registration response. | Yes | [`docs/BEFORE/V04-email-verification-token-disclosure/evidence-log.txt`](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/BEFORE/V04-email-verification-token-disclosure/evidence-log.txt) |
| **V05** | JWT Token-Type Confusion & Absence of Token Revocation / Rotation | Yes | **CONFIRMED** | `JWTserviceImpl` issues access and refresh tokens with identical HMAC keys and claims lacking a `typ`/`token_type` claim, allowing refresh tokens to be used as bearer access tokens on protected APIs. | Yes | [`docs/BEFORE/V05-jwt-token-type-confusion/evidence-log.txt`](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/BEFORE/V05-jwt-token-type-confusion/evidence-log.txt) |
| **V06** | Unauthenticated External Side-Effect via Security Matcher Path Bypass (`GET /test-email`) | Yes | **CONFIRMED** | Spring Security filter chain is constrained via `.securityMatcher("/api/**")`, leaving non-API endpoints like `TestEmailController` completely unauthenticated and triggering external SMTP email dispatch. | Yes | [`docs/BEFORE/V06-unauthenticated-test-email/evidence-log.txt`](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/BEFORE/V06-unauthenticated-test-email/evidence-log.txt) |
| **V07** | Unrestricted File Upload Without MIME, Content, or Extension Validation | Yes | **CONFIRMED** | `SellerController.requestSeller()` accepts arbitrary multipart files with zero extension, MIME, or magic-byte validation, uploading directly to external storage with `resource_type: auto`. | Yes | [`docs/BEFORE/V07-unrestricted-file-upload/evidence-log.txt`](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/BEFORE/V07-unrestricted-file-upload/evidence-log.txt) |
| **V08** | Business Logic Flaw: Arbitrary Inventory Manipulation / Stock Inflation via Order Cancellation | Yes | **CONFIRMED** | Asymmetric order lifecycle: `createOrderFromCart()` fails to decrement stock, while `cancelOrder()` unconditionally restores stock, allowing arbitrary inflation of product inventory. | Yes | [`docs/BEFORE/V08-business-logic-inventory-inflation/evidence-log.txt`](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/BEFORE/V08-business-logic-inventory-inflation/evidence-log.txt) |
| **C01** | User / Account Enumeration in Password Reset Endpoint | Yes | **CODE-LEVEL CONCERN** | `POST /api/v1/auth/forgot-password` returns HTTP `200 OK` for existing emails vs HTTP `401 Unauthorized` (`"User not found with email: ..."`) for non-existent emails. | Distinct mechanism | Tested at runtime; documented as secondary finding. |
| **C02** | Cryptographically Insecure PRNG (`java.util.Random`) for Password-Reset OTP | Yes | **CODE-LEVEL CONCERN** | `AuthServiceImpl.java:534` uses `new Random().nextInt(1_000_000)` (48-bit linear congruential generator) instead of `SecureRandom`. | Distinct mechanism | Code review confirmed; documented as secondary finding. |
| **C03** | Internal Exception Message Exposure in Global Error Handler | Yes | **CODE-LEVEL CONCERN** | `GlobalExceptionHandler.java:341` returns `"Something went wrong: " + ex.getMessage()` on unhandled exceptions, leaking internal driver and query details. | Distinct mechanism | Tested at runtime (e.g. 500 error messages); documented as secondary finding. |
| **C04** | Excessive JWT Access Token Lifetime (60 Days) | Yes | **CODE-LEVEL CONCERN** | `JWTserviceImpl.java:43` defines `ACCESS_TOKEN_EXPIRATION = 60L * 24 * 60 * 60 * 1000` (60 days), creating an excessively large window for stolen tokens. | Distinct mechanism | Code review confirmed. |
| **C05** | Missing Standard HTTP Security Headers (CSP, HSTS, Referrer-Policy) | Yes | **CODE-LEVEL CONCERN** | Response headers lack `Content-Security-Policy`, `Strict-Transport-Security`, and `Referrer-Policy`. | Distinct mechanism | Inspected via `curl -I`. |
| **P01** | Insecure Direct Object Reference (IDOR) on Order Retrieval | Yes | **NOT CONFIRMED** | `OrderServiceImpl.getOrderById()` explicitly verifies that `order.getUser().getEmail()` matches `authentication.getName()`, throwing `OrderNotFoundException` on mismatch. | N/A (Protected) | Runtime test returned `404 Not Found` when User B attempted User A's order ID. |
| **P02** | SQL / JPQL Injection in Derived Repository Queries | Yes | **NOT CONFIRMED** | All repositories strictly use Spring Data JPA derived queries and parameterized queries with Hibernate ORM. No dynamic string concatenation exists. | N/A (Protected) | Code review and probe tests verified. |
| **P03** | Overly Permissive Wildcard Cross-Origin Resource Sharing (CORS) | Yes | **FALSE POSITIVE** | Speculative concern that CORS was open to all origins (`*`). Actual controller configuration explicitly sets `@CrossOrigin(origins = "http://localhost:3000")`, blocking unauthorized origins. | N/A (Protected) | Untrusted origin probe (`Origin: http://evil.com`) returned HTTP `403 Forbidden`. |

---

## 2. Detailed Breakdown of the 8 Confirmed Vulnerabilities

### V01: Unauthenticated Development User-List Endpoint Exposing Passwords & Metadata
* **Affected Endpoint**: `GET /api/v1/auth/dev/users`
* **HTTP Method**: `GET`
* **Authentication**: None (Public)
* **Source Location**: `AuthController.java:274-277`, `SecurityConfig.java:37-41`
* **Root Cause**: The wildcard matcher `/api/v1/auth/**` in Spring Security grants public access (`permitAll()`) to all controller methods under `AuthController`. The `/dev/users` endpoint returns the raw JPA entity collection directly, serializing BCrypt password hashes, emails, user UUIDs, and role mappings to anonymous callers.
* **Security Impact**: Complete account and credential hash harvesting; enables targeted offline password cracking and mapping of privileged administrative accounts.
* **OWASP / CWE**: `A01:2021 – Broken Access Control` / `CWE-200`, `CWE-306`.
* **Distinctness**: Focuses on unauthenticated exposure of a development debugging endpoint dumping stored database credentials.

### V02: Sensitive Information Disclosure via Password-Reset OTP Leak in API Response
* **Affected Endpoint**: `POST /api/v1/auth/forgot-password`
* **HTTP Method**: `POST`
* **Authentication**: None (Public)
* **Source Location**: `AuthController.java:248-251`, `AuthServiceImpl.java:513-553`, `ForgetPasswordResponse.java`
* **Root Cause**: The password reset service returns the generated numeric OTP string directly inside the `ForgetPasswordResponse` DTO returned to the HTTP client.
* **Security Impact**: Direct Account Takeover (ATO). Any unauthenticated attacker knowing a victim's email can initiate a password reset, extract the active OTP from the HTTP response, and reset the password via `/api/v1/auth/reset-password`, completely bypassing email verification.
* **OWASP / CWE**: `A07:2021 – Identification and Authentication Failures` / `CWE-640`, `CWE-200`.
* **Distinctness**: Focuses on secret token disclosure inside the legitimate password recovery flow.

### V03: Privilege Escalation via Client-Controlled Role Assignment During Registration
* **Affected Endpoint**: `POST /api/v1/auth/register`
* **HTTP Method**: `POST`
* **Authentication**: None (Public)
* **Source Location**: `AuthController.java:51-64`, `AuthServiceImpl.java:70-75`
* **Root Cause**: The registration endpoint accepts an optional `role` parameter (`@RequestParam(required = false) Role role`) and assigns it directly to the new entity. No validation or authorization checks prevent anonymous registrants from requesting `ROLE_ADMIN` or `ROLE_SELLER`.
* **Security Impact**: Unauthorized administrative access (`ROLE_ADMIN`) and unauthorized merchant catalog access (`ROLE_SELLER`), bypassing all administrative review workflows.
* **OWASP / CWE**: `A01:2021 – Broken Access Control` / `CWE-269`, `CWE-915`.
* **Distinctness**: Focuses on authorization boundary violation and mass assignment during account creation.

### V04: Email Verification Bypass via In-Band Verification Token Leakage in Registration Response
* **Affected Endpoint**: `POST /api/v1/auth/register`
* **HTTP Method**: `POST`
* **Authentication**: None (Public)
* **Source Location**: `AuthServiceImpl.java:116-134`, `RegisterResponse.java`
* **Root Cause**: The registration response DTO (`RegisterResponse`) includes the raw UUID email verification token string generated by the server.
* **Security Impact**: Allows anyone to register accounts using arbitrary third-party email addresses and activate them immediately without ever possessing or accessing the mailbox. Enables account squatting, pre-hijacking, and spam registration.
* **OWASP / CWE**: `A07:2021 – Identification and Authentication Failures` / `CWE-200`, `CWE-306`.
* **Distinctness**: Distinct from V03 (which is mass assignment role escalation in the same endpoint) and distinct from V02 (which is recovery OTP leakage). V04 specifically compromises the email ownership verification mechanism through in-band token leakage.

### V05: JWT Token-Type Confusion & Absence of Token Revocation / Rotation
* **Affected Endpoints**: Protected API endpoints (e.g. `GET /api/v1/auth/me`), `POST /api/v1/auth/refresh-token`, `POST /api/v1/auth/logout`
* **HTTP Method**: `GET`, `POST`
* **Authentication**: Bearer Token / Refresh Token
* **Source Location**: `JWTFilter.java:34-55`, `JWTserviceImpl.java:51-73`, `AuthServiceImpl.java:231-270`
* **Root Cause**: The JWT utility fails to include a `typ` claim separating access tokens from refresh tokens. `JWTFilter` validates any signed token and populates `SecurityContextHolder`. Long-lived refresh tokens (7 days) can be used as access tokens, access tokens can be refreshed, refresh tokens are never rotated, and logout performs zero server-side invalidation.
* **Security Impact**: Long-lived persistence of compromised refresh tokens, inability to invalidate sessions upon logout, and breakdown of access vs refresh token isolation.
* **OWASP / CWE**: `A07:2021 – Identification and Authentication Failures` / `A02:2021 – Cryptographic Failures` / `CWE-287`, `CWE-613`.
* **Distinctness**: Resides in the cryptographic token verification filter and token lifecycle logic.

### V06: Unauthenticated External Side-Effect via Security Matcher Path Bypass (`GET /test-email`)
* **Affected Endpoint**: `GET /test-email`
* **HTTP Method**: `GET`
* **Authentication**: None (Unauthenticated)
* **Source Location**: `SecurityConfig.java:33-41`, `TestEmailController.java:9-21`
* **Root Cause**: Spring Security is configured with `.securityMatcher("/api/**")`, leaving endpoints outside the `/api/**` path completely unmonitored by security filters. `TestEmailController` exposes `GET /test-email`, which invokes `emailService.sendEmail(...)` unconditionally.
* **Security Impact**: Anonymous remote users can trigger outbound SMTP email dispatches, consuming email service quotas, causing mailbox flooding, and risking sender reputation blacklisting.
* **OWASP / CWE**: `A05:2021 – Security Misconfiguration` / `CWE-306`, `CWE-284`.
* **Distinctness**: Stems from architectural security matcher omission on non-API routes.

### V07: Unrestricted File Upload Without MIME or Extension Validation
* **Affected Endpoint**: `POST /api/v1/seller/request`
* **HTTP Method**: `POST`
* **Authentication**: Authenticated (`ROLE_USER`)
* **Source Location**: `SellerController.java:49-64`, `SellerServiceImpl.java:73-85`
* **Root Cause**: The seller request onboarding endpoint accepts arbitrary `MultipartFile document` uploads without checking file extensions, MIME types, or magic numbers. Files are passed directly to cloud storage with `"resource_type": "auto"`.
* **Security Impact**: Stored Cross-Site Scripting (XSS) via HTML/SVG files viewed by administrators, malware/phishing hosting on trusted URLs.
* **OWASP / CWE**: `A04:2021 – Insecure Design` / `CWE-434`.
* **Distinctness**: Pertains specifically to unrestricted file ingestion and lack of content-type filtering in the multipart pipeline.

### V08: Business Logic Flaw: Arbitrary Inventory Manipulation / Stock Inflation via Order Cancellation
* **Affected Endpoints**: `POST /api/v1/orders` and `PATCH /api/v1/orders/{id}/cancel`
* **HTTP Method**: `POST`, `PATCH`
* **Authentication**: Authenticated (`ROLE_USER`)
* **Source Location**: `OrderServiceImpl.java:47-90` (createOrderFromCart) and `OrderServiceImpl.java:195-223` (cancelOrder)
* **Root Cause**: `createOrderFromCart()` creates an order without decrementing the product stock in the database. When the user calls `cancelOrder()`, the method unconditionally restores stock by adding each item's quantity back to `product.getStock()`.
* **Security Impact**: Any buyer can arbitrarily inflate product inventory to arbitrary levels by placing and immediately cancelling cart orders. Leads to inventory corruption, phantom stock listings, order fulfillment failures, and merchant financial disruption.
* **OWASP / CWE**: `A04:2021 – Insecure Design` / `CWE-840`, `CWE-670`.
* **Distinctness**: Pertains purely to business logic transaction state and inventory lifecycle asymmetry in the order domain.

---

## 3. Summary of Testing and Validation Evidence

All 8 vulnerabilities have complete BEFORE-fix documentation and raw runtime logs captured in:
* `docs/BEFORE/V01-dev-users/`
* `docs/BEFORE/V02-forgot-password-otp/`
* `docs/BEFORE/V03-client-controlled-role-assignment/`
* `docs/BEFORE/V04-email-verification-token-disclosure/`
* `docs/BEFORE/V05-jwt-token-type-confusion/`
* `docs/BEFORE/V06-unauthenticated-test-email/`
* `docs/BEFORE/V07-unrestricted-file-upload/`
* `docs/BEFORE/V08-business-logic-inventory-inflation/`

Every evidence file contains redacted credentials, exact request commands, raw HTTP responses, status codes, and line-level root-cause citations. No modifications or fixes have been applied to the codebase.
