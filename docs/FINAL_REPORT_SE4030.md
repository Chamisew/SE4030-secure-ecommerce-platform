# SE4030 – Secure Software Development
## Comprehensive Group Project Final Security Audit & Remediation Report

---

**Institution**: Sri Lanka Institute of Information Technology (SLIIT)  
**Module**: SE4030 – Secure Software Development  
**Academic Year**: 2026  
**Marks Allocated**: 25 Marks  
**Target Application**: Secure E-Commerce Platform (Full-Stack Spring Boot 3 & Angular 18)  
**Submission Repository**: `https://github.com/Chamisew/SE4030-secure-ecommerce-platform`

---

### Group Members & Contribution Breakdown

| Member Name | Student ID | Specialized Role | Branch Name | Primary Contributions |
| :--- | :--- | :--- | :--- | :--- |
| **Chamila Sewmini** | IT22176424 | Registration & User Identity Specialist | `fix/A-registration-vulns` | Remediated V03 (Role Injection), V04 (Token Leakage); implemented OAuth2 User Provisioning (`CustomOAuth2UserService`, `GoogleOAuth2UserInfo`). |
| **Sammani Wimalarathna** | IT22081834 | Token & Cryptography Specialist | `fix/B-token-vulns` | Remediated V02 (OTP Disclosure & Weak PRNG), V05 (JWT Token Type Confusion); implemented OAuth2 Token Issuance (`OAuth2AuthenticationSuccessHandler`, Anti-Open Redirect). |
| **H.M.D.K. Herath** | IT22106292 | Security Misconfiguration Specialist | `fix/C-secmisconfig-vulns` | Remediated V01 (Exposed Dev Endpoints), V06 (SecurityMatcher Bypass & Unauth Email); implemented OAuth2 Filter Chain & Stateless Cookie Repository. |
| **Sanjana Dinithi** | IT22082510 | File Upload & Order Logic Specialist | `fix/D-fileupload-order-vulns` | Remediated V07 (Unrestricted File Upload), V08 (Inventory Inflation Logic Flaw); implemented Frontend "Sign in with Google" UI & End-to-End OAuth Verification. |

---

## Table of Contents
1. [Executive Summary & Target Architecture](#1-executive-summary--target-architecture)
2. [Security Testing Methodology & Tooling](#2-security-testing-methodology--tooling)
   * 2.1 Static Application Security Testing (SAST)
   * 2.2 Dynamic Application Security Testing (DAST - OWASP ZAP)
   * 2.3 Software Composition Analysis (SCA - OWASP Dependency-Check)
3. [Deep-Dive Security Vulnerability Analysis (V01 – V08)](#3-deep-dive-security-vulnerability-analysis-v01--v08)
   * V01: Exposed Development Endpoints & Password Hash Exposure
   * V02: Cleartext OTP Exposure in HTTP Response & Weak PRNG
   * V03: Client-Controlled Role Assignment via Registration Parameter
   * V04: Email Verification Token Disclosed in HTTP Registration Response
   * V05: JWT Token-Type Confusion Permitting Refresh Tokens as Access Tokens
   * V06: Unauthenticated Arbitrary Email Trigger via Security Matcher Bypass
   * V07: Unrestricted File Upload Without MIME, Content, or Extension Validation
   * V08: Business Logic Flaw in Inventory Management (Arbitrary Stock Inflation)
4. [Unfixed Vulnerabilities & Technical Rationale](#4-unfixed-vulnerabilities--technical-rationale)
5. [Secure Software Engineering Best Practices (SSDLC)](#5-secure-software-engineering-best-practices-ssdlc)
6. [OAuth 2.0 & OpenID Connect (OIDC) Implementation](#6-oauth-20--openid-connect-oidc-implementation)
7. [Conclusion & Deliverables Checklist](#7-conclusion--deliverables-checklist)

---

## 1. Executive Summary & Target Architecture

Modern web applications operate in highly hostile network environments. Deficiencies in architectural design, access control enforcement, cryptographic operations, and input validation routinely expose sensitive enterprise assets to compromise.

This project conducts a comprehensive, multi-phase security evaluation and remediation of a full-stack, enterprise-grade e-commerce application. The platform encompasses a **Spring Boot 3 (Java 21)** REST API backend, **PostgreSQL 18** relational database, and an **Angular 18** Single Page Application frontend. The architecture includes complex multi-role workflows (`ROLE_USER`, `ROLE_SELLER`, `ROLE_ADMIN`), shopping carts, Stripe payment integration, Cloudinary KYC document storage, and order fulfillment pipelines.

### Summary of Audit Outcomes:
* **8 Distinct Vulnerabilities Identified & Confirmed**: Across authentication, session management, access control, file storage, and business logic.
* **100% Remediated with Defense-in-Depth**: All 8 flaws patched at the source level and verified with before-and-after live runtime HTTP exploit traces.
* **OAuth 2.0 & OpenID Connect Implemented**: Designed a stateless, secure Google OAuth 2.0 Authorization Code grant with PKCE (`S256`), automatic user provisioning, and anti-open-redirect controls.
* **Individual Accountability**: Work systematically divided across 4 student team members with distinct Git author/committer identities and branches.

---

## 2. Security Testing Methodology & Tooling

To ensure scientific rigor, both white-box (static/dependency) and black-box (dynamic/runtime) testing methodologies were applied **before and after** code remediation. This two-phase approach provides empirical evidence that the identified vulnerabilities were successfully eliminated.

### 2.1 Static Application Security Testing (SAST) — White-Box
* **Tool Used**: Pattern-based Regex SAST and Semgrep / Sonar-aligned static rule evaluation.
* **Target**: `backend/src/main/java/` (145+ Java classes).
* **BEFORE-Fix Findings**:
  - Identified usage of weak pseudo-random number generators (`new Random()`) in `AuthServiceImpl.java:493`.
  - Identified hardcoded credentials and wildcards in `SecurityConfig.java:38`.
  - Flagged unrestricted `MultipartFile` consumption without size or extension checks in `SellerServiceImpl.java:75`.
* **AFTER-Fix Results**: All critical SAST findings resolved. See `docs/AFTER/tools/whitebox-sast-report.md`.

### 2.2 Dynamic Application Security Testing (DAST) — Black-Box

#### 2.2.1 Custom Automated DAST Scanner
* **Tool Used**: Custom Python-based automated DAST scanner targeting known API attack surfaces.
* **Target**: `http://localhost:8080/` (REST API).
* **BEFORE-Fix Key Findings**:
  - `High`: Unauthenticated administrative user enumeration via `GET /api/v1/auth/dev/users`.
  - `High`: Denial-of-Service and mail flooding via unauthenticated `GET /test-email`.
  - `Medium`: Sensitive OTP and token disclosure in HTTP response bodies (`/forgot-password` and `/register`).
  - `Low`: Missing security headers (`X-Frame-Options`, `Content-Security-Policy`).
* **AFTER-Fix Results**: All High and Medium findings eliminated. See `docs/AFTER/tools/blackbox-dast-report.md`.

#### 2.2.2 OWASP ZAP (Zed Attack Proxy) — Industry-Standard DAST
* **Tool Used**: OWASP ZAP 2.17.0 (by Checkmarx) — the world's most widely-used open-source DAST tool.
* **Scan Type**: Automated Scan (Traditional Spider + Passive Scan + Active Scan).
* **Target**: `http://localhost:8080/` — 6 public API endpoints seeded, 12 URLs discovered.
* **AFTER-Fix Results**:

  | Severity        | Count | Status   |
  |-----------------|-------|----------|
  | High            | **0** | ✅ Clean  |
  | Medium          | **0** | ✅ Clean  |
  | Low             | **12**| Accepted |
  | Informational   | **144**| Noted   |

  - **Zero** SQL Injection, XSS, RCE, Path Traversal, or SSRF vulnerabilities detected.
  - **12 Low-risk** alerts: Spring Boot default error message disclosure (cosmetic, addressed via `@ControllerAdvice` best practice).
  - **144 Informational**: User-Agent fuzzing — application responded consistently (no differential behavior).
* **Report Artifacts**: `docs/AFTER/tools/zap-after-report.html`, `docs/AFTER/tools/zap-scan-report.md`.

### 2.3 Software Composition Analysis (SCA)
* **Tool Used**: OWASP Dependency-Check (v12.1.0).
* **Target**: `backend/pom.xml` dependencies (PostgreSQL driver, Nimbus JOSE JWT, JJWT, Cloudinary HTTP client).
* **Report Artifact**: Saved under `docs/BEFORE/tools/owasp-dependency-check-report.html`.

---

## 3. Deep-Dive Security Vulnerability Analysis (V01 – V08)

---

### V01: Exposed Development Endpoints & Password Hash Exposure
* **CWE**: CWE-200 (Exposure of Sensitive Information), CWE-215 (Insertion of Sensitive Information Into Debug Code)
* **OWASP Top 10**: A01:2021 – Broken Access Control / A05:2021 – Security Misconfiguration
* **Assigned Member**: Member 3 — Security Misconfiguration Specialist (`It22106292`)
* **Branch**: `fix/C-secmisconfig-vulns`

#### Root Cause (BEFORE):
`SecurityConfig.java` contained a permissive wildcard `.requestMatchers("/api/v1/auth/**").permitAll()`. Consequently, `AuthController.java:273` exposed `/api/v1/auth/dev/users` without authentication. Crucially, the endpoint returned complete `Users` database entities directly into JSON, exposing real BCrypt password hashes, email verification tokens, and password reset tokens to anonymous internet users.

#### Code Remediation (AFTER):
1. In `SecurityConfig.java`, replaced wildcard matching with an explicit whitelist of public auth routes (`/login`, `/register`, etc.).
2. Explicitly restricted `/api/v1/auth/dev/**` to `.hasRole("ADMIN")`.
3. In `AuthController.java`, added `@PreAuthorize("hasRole('ADMIN')")` and explicitly masked all passwords with `"[REDACTED]"` and nullified verification tokens prior to serialization.

#### Runtime Verification:
* **Anonymous Request**: `GET /api/v1/auth/dev/users` -> **HTTP 401 Unauthorized**.
* **Regular User Request**: `GET /api/v1/auth/dev/users` (Bearer User JWT) -> **HTTP 403 Forbidden**.
* **Admin Request**: `GET /api/v1/auth/dev/users` (Bearer Admin JWT) -> **HTTP 200 OK** with all passwords redacted.

---

### V02: Cleartext OTP Exposure in HTTP Response & Weak PRNG
* **CWE**: CWE-319 (Cleartext Transmission of Sensitive Information), CWE-330 (Use of Insufficiently Random Values)
* **OWASP Top 10**: A07:2021 – Identification and Authentication Failures / A02:2021 – Cryptographic Failures
* **Assigned Member**: Member 2 — Token & Cryptography Specialist (`Sammani Wimalarathna`)
* **Branch**: `fix/B-token-vulns`

#### Root Cause (BEFORE):
In `AuthServiceImpl.java:493`, the password reset OTP was generated using `new Random().nextInt(...)`, which relies on an insecure linear congruential PRNG with predictable state. Furthermore, `ForgetPasswordResponse.java` contained an `otp` field, causing the server to return the generated OTP directly in the HTTP JSON response body of `POST /api/v1/auth/forgot-password`, completely bypassing email delivery.

#### Code Remediation (AFTER):
1. Upgraded PRNG from `java.util.Random` to cryptographically secure `java.security.SecureRandom`.
2. Removed the `otp` field completely from `ForgetPasswordResponse.java`.
3. The generated OTP is strictly sent via outbound SMTP email to the registered address.

#### Runtime Verification:
* Sending `POST /api/v1/auth/forgot-password` returned:
  ```json
  {"message": "OTP sent to your email successfully"}
  ```
  The OTP is entirely omitted from headers and payload.

---

### V03: Client-Controlled Role Assignment via Registration Parameter
* **CWE**: CWE-269 (Improper Privilege Management), CWE-732 (Incorrect Permission Assignment)
* **OWASP Top 10**: A01:2021 – Broken Access Control
* **Assigned Member**: Member 1 — Registration Specialist (`Chamila Sewmini`)
* **Branch**: `fix/A-registration-vulns`

#### Root Cause (BEFORE):
`AuthController.register()` declared an optional request parameter `@RequestParam(required = false) Role role`. Furthermore, `SignUpRequest.java` exposed a `private Role role` field with getters and setters. If an attacker supplied `role=ROLE_ADMIN` in the multipart registration payload, `AuthServiceImpl.register()` honored the client parameter and elevated the account to full administrative rights.

#### Code Remediation (AFTER):
1. Removed `@RequestParam Role role` from `AuthController.java`.
2. Deleted `private Role role` from `SignUpRequest.java`.
3. In `AuthServiceImpl.java`, newly registered accounts are hardcoded to `Role.ROLE_USER`. Role elevation strictly requires internal administrative approval workflows.

#### Runtime Verification:
* An attacker sending `POST /api/v1/auth/register` with `role=ROLE_ADMIN` was provisioned as `ROLE_USER`.
* Accessing admin-only endpoints (`/api/v1/seller/seller-requests`) resulted in **HTTP 403 Forbidden**.

---

### V04: Email Verification Token Disclosed in HTTP Registration Response
* **CWE**: CWE-200 (Exposure of Sensitive Information)
* **OWASP Top 10**: A01:2021 – Broken Access Control
* **Assigned Member**: Member 1 — Registration Specialist (`Chamila Sewmini`)
* **Branch**: `fix/A-registration-vulns`

#### Root Cause (BEFORE):
`RegisterResponse.java` contained a `verificationToken` field. When a new user registered, the backend returned the raw UUID verification token in the HTTP 201 response body. An attacker could register arbitrary victims' email addresses and immediately verify them without possessing access to the victim's email inbox.

#### Code Remediation (AFTER):
1. Removed `verificationToken` field from `RegisterResponse.java`.
2. The verification token is exclusively sent via asynchronous email with a secure, single-use activation link.

#### Runtime Verification:
* `POST /api/v1/auth/register` response body now contains only:
  ```json
  {"message": "User registered. Please check your email for verification.", "profileImageUrl": null}
  ```

---

### V05: JWT Token-Type Confusion Permitting Refresh Tokens as Access Tokens
* **CWE**: CWE-287 (Improper Authentication), CWE-613 (Insufficient Session Expiration)
* **OWASP Top 10**: A07:2021 – Identification and Authentication Failures
* **Assigned Member**: Member 2 — Token & Cryptography Specialist (`Sammani Wimalarathna`)
* **Branch**: `fix/B-token-vulns`

#### Root Cause (BEFORE):
In `JWTserviceImpl.java`, access tokens had an excessive lifetime of 60 days, while refresh tokens lasted 7 days. Both tokens were signed using the same HMAC secret key without any identifying `token_type` claim. In `JWTFilter.java`, any syntactically valid JWT was accepted as authentication. An attacker could use a refresh token as a bearer token to invoke protected APIs indefinitely.

#### Code Remediation (AFTER):
1. Shortened access token validity to **15 minutes** (`15 * 60 * 1000`).
2. Added explicit token typing: `claims.put("token_type", "access")` for access tokens, and `claims.put("token_type", "refresh")` for refresh tokens.
3. In `JWTFilter.java`, added strict claim validation:
   ```java
   String tokenType = jwtService.extractTokenType(token);
   if (!"access".equalsIgnoreCase(tokenType)) {
       throw new InsufficientAuthenticationException("Invalid token type for resource access");
   }
   ```

#### Runtime Verification:
* Supplying a refresh token to `GET /api/v1/orders` returned **HTTP 401 Unauthorized** (`"Invalid token type for resource access"`).
* Supplying a genuine access token returned **HTTP 200 OK**.

---

### V06: Unauthenticated Arbitrary Email Trigger via Security Matcher Bypass
* **CWE**: CWE-306 (Missing Authentication for Critical Function), CWE-862 (Missing Authorization)
* **OWASP Top 10**: A01:2021 – Broken Access Control / A05:2021 – Security Misconfiguration
* **Assigned Member**: Member 3 — Security Misconfiguration Specialist (`It22106292`)
* **Branch**: `fix/C-secmisconfig-vulns`

#### Root Cause (BEFORE):
`SecurityConfig.java` declared `.securityMatcher("/api/**")`. As a result, routes outside `/api/**`—such as the utility route `/test-email` defined in `TestEmailController.java`—were completely excluded from the Spring Security filter chain. Any unauthenticated attacker could trigger mass outbound emails, resulting in SMTP reputation damage and resource exhaustion.

#### Code Remediation (AFTER):
1. Removed `.securityMatcher("/api/**")` to enforce an application-wide **Default-Deny** security posture.
2. In `SecurityConfig.java`, explicitly restricted `/test-email/**` to `.hasRole("ADMIN")`.
3. In `TestEmailController.java`, added `@PreAuthorize("hasRole('ADMIN')")`.

#### Runtime Verification:
* Anonymous request `GET /test-email?to=target@test.com` returned **HTTP 401 Unauthorized**.

---

### V07: Unrestricted File Upload Without MIME, Content, or Extension Validation
* **CWE**: CWE-434 (Unrestricted Upload of File with Dangerous Type)
* **OWASP Top 10**: A04:2021 – Insecure Design / A03:2021 – Injection
* **Assigned Member**: Member 4 — File Upload Specialist (`Sanjana Dinithi`)
* **Branch**: `fix/D-fileupload-order-vulns`

#### Root Cause (BEFORE):
`SellerServiceImpl.sendSellerRequest()` accepted a `MultipartFile document` for KYC seller verification without inspecting size, Content-Type, or extension. The raw bytes were forwarded to `cloudinary.uploader().upload()` with `"resource_type": "auto"`. Attackers could upload arbitrary HTML scripts, malicious executables, or phishing documents to the corporate CDN.

#### Code Remediation (AFTER):
1. Enforced a 5MB maximum file size ceiling.
2. Implemented strict MIME type whitelisting (`application/pdf`, `image/jpeg`, `image/png`).
3. Implemented filename extension validation (`.pdf`, `.jpg`, `.jpeg`, `.png`).
4. Replaced `"resource_type": "auto"` with specific `"raw"` or `"image"`.

#### Runtime Verification:
* Attempted upload of `test_kyc.html` returned **HTTP 400 Bad Request** (`"Invalid file type. Only PDF, JPEG, and PNG documents are allowed"`).
* Attempted upload of `malicious.exe` (with spoofed `image/png` header) returned **HTTP 400 Bad Request** (`"Invalid file extension"`).

---

### V08: Business Logic Flaw in Inventory Management (Arbitrary Stock Inflation)
* **CWE**: CWE-840 (Business Logic Errors), CWE-799 (Improper Control of Generation of Resource)
* **OWASP Top 10**: A04:2021 – Insecure Design
* **Assigned Member**: Member 4 — File Upload & Order Logic Specialist (`Sanjana Dinithi`)
* **Branch**: `fix/D-fileupload-order-vulns`

#### Root Cause (BEFORE):
While direct order checkout decremented stock, `createOrderFromCart()` in `OrderServiceImpl.java` created orders without validating available inventory and without deducting stock from the database. Conversely, `cancelOrder()` unconditionally restored stock for all order items (`product.setStock(product.getStock() + quantity)`). An attacker could repeatedly add items to their cart, check out, and cancel to inflate warehouse inventory indefinitely.

#### Code Remediation (AFTER):
1. In `createOrderFromCart()`, added stock availability verification (`ProductOutOfStockException`) and atomic stock decrementing (`productRepository.save(product)`).
2. In `cancelOrder()`, enforced strict idempotency (`if (order.getStatus() == OrderStatus.CANCELLED) return;`) preventing double-restoration.

#### Runtime Verification:
* Initial Stock = 62 units.
* Cart Order placed for 2 units -> Stock immediately decremented to **60 units**.
* Order cancelled -> Stock restored to **62 units**.
* Duplicate cancellation attempted -> Stock remained constant at **62 units**.

---

## 4. Unfixed Vulnerabilities & Technical Rationale

In accordance with assignment guidelines, two architectural limitations were identified but consciously left unfixed, with technical justifications:

### 1. Lack of Distributed Redis Token Revocation Blacklist for Active JWTs
* **Description**: When a user logs out (`/api/v1/auth/logout`), the refresh token is deleted from the client, but previously issued JWT access tokens remain cryptographically valid until their expiration.
* **Reason for Not Fixing in Current Phase**:
  1. *Mitigation via Ephemeral Lifetimes*: The access token expiration was already reduced from 60 days to 15 minutes (V05 fix), bounding the vulnerability window to an acceptable threshold for standard e-commerce operations.
  2. *Architectural Trade-Off*: Introducing a server-side Redis token blacklist introduces network latency and transforms a stateless microservice architecture into a stateful distributed dependency, which was deemed disproportionate for the current deployment tier.

### 2. Distributed Rate-Limiting on External Payment Webhooks
* **Description**: Stripe webhook callbacks (`/api/v1/payments/webhook`) lack distributed rate-limiting against high-concurrency replay bursts.
* **Reason for Not Fixing in Current Phase**:
  1. *Cryptographic Signature Verification*: Webhooks are already validated via Stripe cryptographic signature verification (`Stripe-Signature` HMAC-SHA256 headers), ensuring only legitimate events from Stripe's servers are processed.
  2. *Database Idempotency*: Payment status transitions in PostgreSQL are protected by unique session constraint checks, preventing duplicate ledger credits.

---

## 5. Secure Software Engineering Best Practices (SSDLC)

The vulnerabilities identified in this project highlight common failure modes in software engineering lifecycles. Incorporating the following Secure Software Development Lifecycle (SSDLC) practices would have prevented their introduction:

1. **Threat Modeling at Design Phase (STRIDE Methodology)**:
   * Identifying that cart checkout and cancellation operate asynchronously would have flagged the inventory inflation asymmetry (V08) before code was written.
2. **Automated Security Gating in CI/CD Pipelines**:
   * Integrating SAST tools (Semgrep, SonarQube) and SCA tools (OWASP Dependency-Check) into GitHub Actions pull request workflows blocks merges that introduce `new Random()` (V02) or overly permissive wildcards (V01).
3. **Principle of Least Privilege & Default-Deny**:
   * Security filter chains must always default to denying all unauthenticated requests (`anyRequest().authenticated()`), preventing route omissions like `/test-email` (V06).
4. **Defense-in-Depth for File Ingestion**:
   * Treating all client-supplied files as hostile, enforcing size limits, MIME whitelists, and extension verification at controller and service boundaries (V07).

---

## 6. OAuth 2.0 & OpenID Connect (OIDC) Implementation

### 6.1 Architectural Design
To modernise customer onboarding and eliminate password handling risks, the application implements the **OAuth 2.0 Authorization Code Grant with OpenID Connect (OIDC)** and **PKCE** (`code_challenge_method=S256`) using Google Identity Services.

### 6.2 Implementation Details:
* **Stateless Cookie Repository** (`HttpCookieOAuth2AuthorizationRequestRepository`): Eliminates server-side session stickiness by storing OAuth2 `state` and PKCE verifiers in short-lived (180s), encrypted, `HttpOnly` cookies.
* **Automated User Provisioning** (`CustomOAuth2UserService`): Intercepts OIDC user claims (`sub`, `email`, `name`, `picture`), provisions accounts in PostgreSQL with verified emails, and assigns `Role.ROLE_USER` by default.
* **Anti-Open Redirect Handler** (`OAuth2AuthenticationSuccessHandler`): Validates redirect URIs against a strict whitelist before redirecting the browser with minted 15-minute access tokens and 7-day refresh tokens.
* **Frontend SPA Integration**: Adds "Sign in with Google" buttons in Angular templates and handles token capture seamlessly via `OAuth2RedirectComponent`.

---

## 7. Conclusion & Deliverables Checklist

The security posture of the e-commerce platform has been comprehensively transformed:
* 8 distinct vulnerabilities across OWASP Top 10 categories thoroughly remediated.
* Full-featured Google OAuth 2.0 / OpenID Connect login deployed.
* Complete before-and-after audit logs preserved under `docs/BEFORE/` and `docs/AFTER/`.
* Git repository commit history structured member-wise with granular commits.
* Video presentation structure and `README.txt` prepared for final university submission.
