# SECURITY_TEST_PLAN.md: Baseline Security Test Plan & Architecture Inspection

## 1. Application Architecture

### 1.1 Overview
The application is a full-stack E-Commerce platform consisting of a Java Spring Boot RESTful API backend and an Angular single-page application (SPA) frontend.

```
+-----------------------------------+       HTTP/REST       +------------------------------------+
|         Angular Frontend          | <-------------------> |         Spring Boot Backend        |
|  (http://localhost:4200 / 3000)   |    Bearer JWT Auth    |       (http://localhost:8080)       |
+-----------------------------------+                       +------------------------------------+
                                                              |            |            |
                                                              v            v            v
                                                       PostgreSQL/H2   Cloudinary     Stripe API
                                                        (Database)     (File Store)   (Payments)
```

### 1.2 Tech Stack & Component Versions
* **Backend Framework**: Spring Boot `4.0.0` (Spring Security, Spring Data JPA, Spring Web MVC, Spring Validation, Spring Mail)
* **Java Version**: JDK `21`
* **Database**: PostgreSQL (`jdbc:postgresql://localhost:5432/E_Commerence_DB`) with H2 database (`com.h2database:h2`) configured in dependencies. ORM managed via Hibernate JPA (`spring.jpa.hibernate.ddl-auto=update`).
* **Frontend Framework**: Angular `21.2.0` (Angular CLI `21.2.7`, RxJS `7.8.0`, TypeScript `5.9.2`)
* **Authentication Mechanism**: Stateless JWT (JSON Web Token) authentication using `io.jsonwebtoken:jjwt-api:0.12.6`, `jjwt-impl:0.12.6`, `jjwt-jackson:0.12.6` paired with Spring Security `DaoAuthenticationProvider` and `BCryptPasswordEncoder`.
* **External Integrations**:
  * **Cloudinary**: `com.cloudinary:cloudinary-http44:1.32.0` for image upload & hosting.
  * **Stripe**: `com.stripe:stripe-java:31.2.0-alpha.1` for payment processing and refunds.
  * **Mail**: `spring-boot-starter-mail` targeting local SMTP mock/trap server on `localhost:1025`.

### 1.3 Service URLs & Ports
* **Backend Base URL**: `http://localhost:8080`
* **API Base URL**: `http://localhost:8080/api/v1`
* **Frontend Local URL**: `http://localhost:4200` (Angular CLI default dev server; AuthController CORS allows `http://localhost:3000`)
* **Local SMTP Trap**: `localhost:1025`

---

## 2. Frontend vs Backend Functionality Mapping

### 2.1 Available Frontend Functionality (Angular UI)
The current Angular frontend application (located in `frontend/src/app`) implements only a subset of the authentication user experience:
1. **User Login Page (`/auth`)**:
   * Form capturing `email` and `password`.
   * Sends `POST /api/v1/auth/login`.
   * Stores `accessToken`, `refreshToken`, and `email` in browser `localStorage`.
2. **User Registration Page (`/auth`)**:
   * Form capturing `firstName`, `lastName`, `email`, `password`, and optional profile photo upload.
   * Sends `POST /api/v1/auth/register` as `multipart/form-data`.
3. **Protected Home Route (`/home`)**:
   * Guarded by `AuthGuard` (checks existence of `accessToken` in `localStorage`).
   * Displays basic placeholder view.

### 2.2 Backend-Only Functionality (API-Only Testing Scope)
The backend exposes extensive features that are not yet wired to Angular UI components and must be tested directly via Postman, cURL, or OWASP ZAP:
* **Account Verification & Token Refresh**: Email verification (`GET /api/v1/auth/verify-email`), Refresh Token exchange (`POST /api/v1/auth/refresh-token`), Logout (`POST /api/v1/auth/logout`).
* **Profile & Identity Management**: Profile update (`PUT /api/v1/auth/update-profile`), View current user (`GET /api/v1/auth/me`), Delete user (`DELETE /api/v1/auth/me`), Email change request & verification (`POST /api/v1/auth/update-email`, `GET /api/v1/auth/update-email/verify`), Password change (`PUT /api/v1/auth/update-password`).
* **Password Reset Flow**: Forgot password OTP generation (`POST /api/v1/auth/forgot-password`), Reset password via OTP (`POST /api/v1/auth/reset-password`).
* **Development/Admin Endpoints**: Listing all users (`GET /api/v1/auth/dev/users`), Test email endpoint (`GET /test-email`).
* **Catalog Management**: Product CRUD (`POST`, `GET`, `PUT`, `DELETE` on `/api/v1/products`), Category CRUD (`POST`, `GET`, `PUT`, `DELETE` on `/api/v1/categories`).
* **Shopping Cart & Wishlist**: Cart management (`/api/v1/cart/*`), Wishlist management (`/api/v1/wishlist/*`).
* **Order Processing**: Create cart order, create direct order, list user orders, view order details, cancel order (`/api/v1/orders/*`).
* **Payments & Refunds**: Stripe checkout session creation, payment confirmation, refund processing (`/api/v1/payments/*`).
* **Seller Onboarding & Admin Workflow**: Seller request submission with verification document upload (`POST /api/v1/seller/request`), List pending seller requests (`GET /api/v1/seller/seller-requests`), Approve seller request (`POST /api/v1/seller/approve/{requestId}`), Reject seller request (`POST /api/v1/seller/reject/{requestId}`).

---

## 3. Authentication & Authorization Flow

### 3.1 Authentication Flow
1. **Registration Flow**:
   * Client issues `POST /api/v1/auth/register` (`multipart/form-data`).
   * Server validates parameters, hashes password using `BCryptPasswordEncoder`, creates user in DB with `enabled=false` and `emailVerified=false`.
   * Server generates a random UUID `VerificationToken` expiring in 24 hours.
   * Server dispatches verification email to user containing `http://localhost:8080/api/v1/auth/verify-email?token=<UUID>`.
   * *Note*: The registration endpoint response body returns the registration message and includes the raw verification token string.
2. **Email Verification Flow**:
   * Client calls `GET /api/v1/auth/verify-email?token=<token>`.
   * Server verifies token validity, sets `emailVerified=true` and `enabled=true`, and marks token as used.
3. **Login Flow**:
   * Client issues `POST /api/v1/auth/login` (`application/json`).
   * `AuthenticationManager` verifies credentials against `UsersServices`.
   * Server checks if `user.isEnabled()` (`emailVerified`).
   * Upon verification, `JWTserviceImpl` constructs:
     * **Access Token**: Signed HMAC SHA key (`jwt.secret`), 60 days validity (`60L * 24 * 60 * 60 * 1000`), claims containing `sub` (email), `role`, and `roles`.
     * **Refresh Token**: Signed HMAC SHA key (`jwt.secret`), 7 days validity (`7 * 24 * 60 * 60 * 1000`).
   * Server returns `LoginResponse` with tokens and user details.
4. **Token Handling & Filter Chain**:
   * Spring Security filter chain matches `/api/**`.
   * `JWTFilter` interceptor extracts `Authorization: Bearer <token>` header, parses claims, resolves `UserDetails`, extracts roles as `SimpleGrantedAuthority`, and populates `SecurityContextHolder`.
5. **Token Refresh Flow**:
   * Client sends `POST /api/v1/auth/refresh-token` with `{ "token": "<refresh_token>" }`.
   * Server extracts username, validates token expiration, issues a new access token, and reissues the submitted refresh token.

### 3.2 Authorization Flow & Role Hierarchy
* **Defined Roles** (`com.example.backend.entity.Role`):
  * `ROLE_USER`: Standard buyer account.
  * `ROLE_SELLER`: Onboarded merchant account.
  * `ROLE_ADMIN`: Administrative account.
* **Security Filter Rules (`SecurityConfig.java`)**:
  * `/api/v1/auth/**` -> `.permitAll()` (Public access for auth endpoints).
  * `/api/**` -> `.authenticated()` (Requires valid JWT authentication).
* **Method Security (`@EnableMethodSecurity`)**:
  * `@PreAuthorize("hasRole('ADMIN')")`:
    * Category Management: `POST`, `PUT`, `DELETE` on `/api/v1/categories`.
    * Seller Administration: `GET /api/v1/seller/seller-requests`, `POST /api/v1/seller/approve/{id}`, `POST /api/v1/seller/reject/{id}`.
  * `@PreAuthorize("hasRole('SELLER')")`:
    * Product Creation: `POST /api/v1/products`.
  * `@PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")`:
    * Product Update & Deletion: `PUT /api/v1/products/{id}`, `DELETE /api/v1/products/{id}`.
  * `@PreAuthorize("isAuthenticated()")`:
    * Cart Operations: `/api/v1/cart/*`.
* **Role Assignment Mechanisms**:
  * Default registration assigns `ROLE_USER` if unassigned; however, `register` endpoint accepts a `role` parameter during signup.
  * Seller onboarding workflow promotes `ROLE_USER` to `ROLE_SELLER` upon admin approval.

---

## 4. List of Security-Sensitive Endpoints

| Endpoint Base / Path | HTTP Method | Permitted Roles / Auth | Sensitive Operations & Potential Security Areas |
| :--- | :--- | :--- | :--- |
| `/api/v1/auth/register` | `POST` | Public | Account creation, role assignment parameter, profile image upload |
| `/api/v1/auth/login` | `POST` | Public | Credential validation, token generation, rate limiting |
| `/api/v1/auth/verify-email` | `GET` | Public | Token consumption, account activation state |
| `/api/v1/auth/refresh-token` | `POST` | Public | Token rotation, token reuse, expiration checks |
| `/api/v1/auth/forgot-password` | `POST` | Public | OTP generation, rate limiting, email delivery |
| `/api/v1/auth/reset-password` | `POST` | Public | OTP validation, password update logic |
| `/api/v1/auth/dev/users` | `GET` | Public | Unrestricted listing of all user accounts and details |
| `/api/v1/auth/me` | `GET` / `DELETE` | Authenticated | User identity retrieval, account deletion |
| `/api/v1/auth/update-profile` | `PUT` | Authenticated | Profile modification, avatar upload |
| `/api/v1/auth/update-email` | `POST` | Authenticated | Email change initiation |
| `/api/v1/auth/update-email/verify` | `GET` | Public | Email change verification token |
| `/api/v1/auth/update-password` | `PUT` | Authenticated | Password modification |
| `/api/v1/products` | `POST` | `ROLE_SELLER` | Product creation, image upload |
| `/api/v1/products/{id}` | `PUT` / `DELETE` | `ROLE_SELLER`, `ROLE_ADMIN` | Product ownership check, asset deletion |
| `/api/v1/categories` | `POST`/`PUT`/`DELETE` | `ROLE_ADMIN` | Category management |
| `/api/v1/cart/*` | `POST`/`PUT`/`DELETE`/`GET` | Authenticated | Cart item modification, ID manipulation |
| `/api/v1/orders` | `POST` / `GET` | Authenticated | Order placement, cart checkout |
| `/api/v1/orders/{id}` | `GET` / `PATCH` | Authenticated | Individual order retrieval, order cancellation, IDOR |
| `/api/v1/payments/create/{orderId}` | `POST` | Authenticated | Payment session generation, order ownership |
| `/api/v1/payments/confirm` | `GET` | Authenticated | Payment verification by session ID |
| `/api/v1/payments/refund` | `POST` | Authenticated | Refund processing, amount validation |
| `/api/v1/seller/request` | `POST` | Authenticated | KYC document upload, onboarding request |
| `/api/v1/seller/seller-requests` | `GET` | `ROLE_ADMIN` | Accessing pending seller verification data |
| `/api/v1/seller/approve/{id}` | `POST` | `ROLE_ADMIN` | Role elevation (`ROLE_USER` -> `ROLE_SELLER`) |
| `/api/v1/seller/reject/{id}` | `POST` | `ROLE_ADMIN` | Seller application rejection |
| `/test-email` | `GET` | Unsecured / Public | Unrestricted email trigger |

---

## 5. Proposed Baseline Security Tests

### 5.1 Manual API Security Tests (Postman / cURL)

#### Test M-01: User Registration Role Assignment Behavior
* **Target Endpoint**: `POST /api/v1/auth/register` (`multipart/form-data`)
* **Test Procedure**:
  1. Send a registration request with parameters: `firstName=Test`, `lastName=User`, `email=admin_test@example.com`, `password=Password123!`, `role=ROLE_ADMIN`.
  2. Send a registration request with `role=ROLE_SELLER`.
  3. Inspect HTTP status code and response payload.
* **Expected Evidence**:
  * Record HTTP status code (e.g., `201 Created`).
  * Capture response body showing returned role and verification token.
  * Attempt login with credentials and inspect JWT claims (`role` / `roles`) or attempt calling `ROLE_ADMIN` restricted endpoints.

#### Test M-02: Unauthenticated User Listing Endpoint Inspection
* **Target Endpoint**: `GET /api/v1/auth/dev/users`
* **Test Procedure**:
  1. Issue a `GET` request to `http://localhost:8080/api/v1/auth/dev/users` without any `Authorization` header.
  2. Inspect response HTTP status code and body.
* **Expected Evidence**:
  * Record HTTP status code (e.g., `200 OK` vs `401/403`).
  * Capture returned JSON structure and user fields.

#### Test M-03: Insecure Direct Object Reference (IDOR) on Order Retrieval
* **Target Endpoint**: `GET /api/v1/orders/{id}`
* **Test Procedure**:
  1. Register and verify Account A and Account B.
  2. Place an order as Account A to generate Order ID `X`.
  3. Authenticate as Account B and request `GET /api/v1/orders/X` using Account B's JWT token.
* **Expected Evidence**:
  * Record HTTP status code for Account B's request (e.g., `200 OK` vs `403 Forbidden` / `404 Not Found`).
  * Capture response payload returned to Account B.

#### Test M-04: Order Ownership Validation in Payment Creation
* **Target Endpoint**: `POST /api/v1/payments/create/{orderId}`
* **Test Procedure**:
  1. Create Order ID `Y` using Account A.
  2. Authenticate as Account B and request `POST /api/v1/payments/create/Y`.
* **Expected Evidence**:
  * Capture HTTP status code and error message/response body returned to Account B.

#### Test M-05: Privilege Escalation & Access Control on Admin Endpoints
* **Target Endpoint**: `GET /api/v1/seller/seller-requests`, `POST /api/v1/categories`
* **Test Procedure**:
  1. Authenticate as a standard user with `ROLE_USER`.
  2. Issue `GET /api/v1/seller/seller-requests` with `ROLE_USER` JWT.
  3. Issue `POST /api/v1/categories` with `ROLE_USER` JWT.
* **Expected Evidence**:
  * Record HTTP status code (expected `403 Forbidden`).

#### Test M-06: JWT Expiration & Signature Tampering
* **Target Endpoint**: `GET /api/v1/auth/me`
* **Test Procedure**:
  1. Obtain a valid JWT for an authenticated user.
  2. Modify the payload (e.g., change `sub` to another user's email) without re-signing, and send `Authorization: Bearer <tampered_jwt>`.
  3. Change algorithm header to `none` (`eyJhbGciOiJub25lIn0...`) and send token.
* **Expected Evidence**:
  * Capture HTTP response status (expected `401 Unauthorized`).

#### Test M-07: File Upload Verification & Content Validation
* **Target Endpoint**: `POST /api/v1/seller/request`, `POST /api/v1/products`
* **Test Procedure**:
  1. Authenticate as user.
  2. Submit seller request uploading a file with non-standard extensions (e.g., `.html`, `.svg`, `.exe`, `.jsp`) or arbitrary binary data as `document`.
* **Expected Evidence**:
  * Capture HTTP status code and server response payload.

#### Test M-08: Password Reset OTP Response Inspection
* **Target Endpoint**: `POST /api/v1/auth/forgot-password`
* **Test Procedure**:
  1. Send request `{"email": "user@example.com"}`.
  2. Inspect response JSON body for presence of sensitive fields.
* **Expected Evidence**:
  * Capture JSON response structure (check if `otp` string is present in response).

---

### 5.2 OWASP ZAP Automated Scanning Tests

#### Test Z-01: Baseline Passive & Active Scan of Public Auth Endpoints
* **Target Context**: `http://localhost:8080/api/v1/auth/*`
* **ZAP Configuration**:
  * Import OpenAPI / REST API definitions or proxy Postman requests through ZAP.
  * Run ZAP Passive Scanner on `POST /api/v1/auth/login` and `POST /api/v1/auth/register`.
  * Run ZAP Active Scanner on parameter fields (input validation, SQL injection, XSS vectors).
* **Expected Evidence**:
  * ZAP Alert Summary Report (High, Medium, Low, Informational alerts).
  * HTTP request/response logs for flags.

#### Test Z-02: Authenticated Active Scan of Protected Endpoints
* **Target Context**: `/api/v1/cart`, `/api/v1/orders`, `/api/v1/products`, `/api/v1/wishlist`
* **ZAP Configuration**:
  * Configure ZAP HTTP Sender script or Authorization Header (`Authorization: Bearer <valid_jwt>`).
  * Run Active Scanner against protected routes.
* **Expected Evidence**:
  * ZAP Scan Progress Log and Alert Details.

#### Test Z-03: Security Headers & CORS Policy Analysis
* **Target Context**: All API endpoints
* **ZAP Configuration**:
  * Analyze HTTP response headers for missing security controls (`Content-Security-Policy`, `X-Content-Type-Options`, `X-Frame-Options`, `Strict-Transport-Security`).
  * Check CORS response headers (`Access-Control-Allow-Origin`, `Access-Control-Allow-Credentials`).
* **Expected Evidence**:
  * ZAP Header Inspection Alerts & raw response header captures.

---

## 6. Verification Summary & Next Steps

### 6.1 Summary of Inspection Findings
1. **Architecture**: Clean separation between Spring Boot 4.0.0 backend (port 8080) and Angular 21.2.0 frontend (port 4200).
2. **Authentication**: Stateless JWT implementation with 60-day access token TTL and 7-day refresh token TTL.
3. **UI Coverage**: Angular frontend currently implements UI forms only for Login and Registration; all remaining endpoints require direct API testing.
4. **Endpoint Exposure**: 26+ endpoints identified across Auth, Product, Category, Cart, Order, Payment, Seller, Wishlist, and Test controllers.

### 6.2 Items Requiring Runtime Verification
To complete baseline BEFORE-fix evidence collection:
* Execute proposed manual Postman/cURL test cases against the live running backend.
* Run OWASP ZAP automated scan against active local backend services.
* Capture and archive raw HTTP request/response logs and ZAP XML/HTML alert reports for baseline comparison.
