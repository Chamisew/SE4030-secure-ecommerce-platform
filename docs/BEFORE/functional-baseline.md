# Functional Baseline Test Report (ORIGINAL Application)

## Executive Summary
This document establishes the functional baseline of the ORIGINAL e-commerce application prior to security testing and vulnerability remediation. All tests were performed using local test accounts and non-destructive legitimate API calls against the live Spring Boot backend (`http://localhost:8080`) and Angular frontend (`http://localhost:3000`).

---

## 1. Angular Frontend Navigation Tests

### Test FT-01: Frontend Root Route Loading
* **Test ID**: `FT-01`
* **Endpoint / Page**: `http://localhost:3000/`
* **HTTP Method**: `GET`
* **Authentication Requirement**: None (Public)
* **Request**: Standard GET request to root URL.
* **Response Status**: `200 OK`
* **Relevant Response Fields**: HTML document rendered, Angular application bundle loaded.
* **Result**: `PASS`
* **Notes**: Frontend dev server successfully redirects to `/auth`.

---

### Test FT-02: Auth Page Loading (Login & Registration Forms)
* **Test ID**: `FT-02`
* **Endpoint / Page**: `http://localhost:3000/auth`
* **HTTP Method**: `GET`
* **Authentication Requirement**: None (Public)
* **Request**: GET request to `/auth`.
* **Response Status**: `200 OK`
* **Relevant Response Fields**: Login and Sign-Up HTML form components loaded (`AuthComponent`).
* **Result**: `PASS`
* **Notes**: Form inputs for login (email, password) and registration (firstName, lastName, email, password, photo) present.

---

### Test FT-03: Authenticated Home Page Route Loading
* **Test ID**: `FT-03`
* **Endpoint / Page**: `http://localhost:3000/home`
* **HTTP Method**: `GET`
* **Authentication Requirement**: Authenticated (`AuthGuard` checks `localStorage` access token)
* **Request**: GET request to `/home`.
* **Response Status**: `200 OK`
* **Relevant Response Fields**: Placeholder view `HomeComponent` rendered.
* **Result**: `PASS`
* **Notes**: Protected by Angular `AuthGuard`.

---

## 2. Authentication & User Onboarding Tests

### Test FT-04: User Registration
* **Test ID**: `FT-04`
* **Endpoint / Page**: `/api/v1/auth/register`
* **HTTP Method**: `POST`
* **Authentication Requirement**: None (Public)
* **Request**:
  * Content-Type: `multipart/form-data`
  * Form Data: `firstName="Functional"`, `lastName="User"`, `email="func_user@example.com"`, `password="Password123!"`
* **Response Status**: `201 Created`
* **Relevant Response Fields**:
  ```json
  {
    "message": "User registered. Please check your email for verification.",
    "profileImageUrl": null,
    "verificationToken": "658ceffb-82b9-4610-9990-2bd3310c17cb"
  }
  ```
* **Result**: `PASS`
* **Notes**: Account created in unverified (`enabled=false`) state. Verification token returned in response body and sent via local SMTP server (`localhost:1025`).

---

### Test FT-05: Local Email Verification
* **Test ID**: `FT-05`
* **Endpoint / Page**: `/api/v1/auth/verify-email`
* **HTTP Method**: `GET`
* **Authentication Requirement**: None (Public)
* **Request**: Query string `?token=658ceffb-82b9-4610-9990-2bd3310c17cb`
* **Response Status**: `200 OK`
* **Relevant Response Fields**: `{"message":"Email verified successfully!"}`
* **Result**: `PASS`
* **Notes**: Account enabled (`enabled=true`, `emailVerified=true`) upon token consumption.

---

### Test FT-06: Verified Account Login
* **Test ID**: `FT-06`
* **Endpoint / Page**: `/api/v1/auth/login`
* **HTTP Method**: `POST`
* **Authentication Requirement**: None (Public)
* **Request**:
  * Content-Type: `application/json`
  * Body: `{"email":"func_user@example.com","password":"Password123!"}`
* **Response Status**: `200 OK`
* **Relevant Response Fields**:
  * `message`: `"Login successful"`
  * `accessToken`: `eyJhbGciOiJIUzUxMiJ9...`
  * `refreshToken`: `eyJhbGciOiJIUzUxMiJ9...`
  * `role`: `"ROLE_USER"`
* **Result**: `PASS`
* **Notes**: Returns JWT access token (60-day TTL) and refresh token (7-day TTL). Tokens stored in local test session.

---

### Test FT-07: Get Authenticated Profile (`/me`)
* **Test ID**: `FT-07`
* **Endpoint / Page**: `/api/v1/auth/me`
* **HTTP Method**: `GET`
* **Authentication Requirement**: Authenticated (Bearer Token)
* **Request**: Header `Authorization: Bearer <user_access_token>`
* **Response Status**: `200 OK`
* **Relevant Response Fields**:
  ```json
  {
    "email": "func_user@example.com",
    "firstName": "Functional",
    "lastName": "User",
    "profileImageUrl": null
  }
  ```
* **Result**: `PASS`
* **Notes**: Identity extracted from SecurityContext principal.

---

### Test FT-08: Access Token Refresh
* **Test ID**: `FT-08`
* **Endpoint / Page**: `/api/v1/auth/refresh-token`
* **HTTP Method**: `POST`
* **Authentication Requirement**: None (Public)
* **Request**:
  * Content-Type: `application/json`
  * Body: `{"token":"<valid_user_refresh_token>"}`
* **Response Status**: `200 OK`
* **Relevant Response Fields**:
  * `message`: `"Token refreshed successfully"`
  * `accessToken`: `<new_jwt_access_token>`
  * `refreshToken`: `<same_refresh_token>`
* **Result**: `PASS`
* **Notes**: Successfully issues new access token while preserving refresh token.

---

## 3. Backend-Only Domain Workflows

### Test FT-09: Admin User Setup (`ROLE_ADMIN`)
* **Test ID**: `FT-09`
* **Endpoint / Page**: `/api/v1/auth/register` & `/api/v1/auth/login`
* **HTTP Method**: `POST`
* **Authentication Requirement**: None
* **Request**: Registration with `email="admin_test@example.com"`, `role="ROLE_ADMIN"`.
* **Response Status**: `201 Created` (Registration), `200 OK` (Login)
* **Relevant Response Fields**: `role: "ROLE_ADMIN"` in login response.
* **Result**: `PASS`
* **Notes**: Admin account registered, verified, and logged in for administrative workflows.

---

### Test FT-10: Category Creation (Admin Workflow)
* **Test ID**: `FT-10`
* **Endpoint / Page**: `/api/v1/categories`
* **HTTP Method**: `POST`
* **Authentication Requirement**: `ROLE_ADMIN`
* **Request**:
  * Header: `Authorization: Bearer <admin_access_token>`
  * Body: `{"name":"Electronics","description":"Gadgets and electronic items"}`
* **Response Status**: `201 Created`
* **Relevant Response Fields**: `{"id":1,"name":"Electronics","description":"Gadgets and electronic items"}`
* **Result**: `PASS`
* **Notes**: Category persisted in PostgreSQL database with ID `1`.

---

### Test FT-11: Category Listing
* **Test ID**: `FT-11`
* **Endpoint / Page**: `/api/v1/categories`
* **HTTP Method**: `GET`
* **Authentication Requirement**: Authenticated (User / Admin)
* **Request**: Header `Authorization: Bearer <user_access_token>`
* **Response Status**: `200 OK`
* **Relevant Response Fields**: Array of category objects containing Category ID 1.
* **Result**: `PASS`
* **Notes**: Note that unauthenticated requests to `/api/v1/categories` return `401 Unauthorized` per `SecurityConfig.java`.

---

### Test FT-12: Direct Seller Setup (`ROLE_SELLER`)
* **Test ID**: `FT-12`
* **Endpoint / Page**: `/api/v1/auth/register` & `/api/v1/auth/login`
* **HTTP Method**: `POST`
* **Authentication Requirement**: None
* **Request**: Registration with `email="seller_direct@example.com"`, `role="ROLE_SELLER"`.
* **Response Status**: `201 Created` (Registration), `200 OK` (Login)
* **Relevant Response Fields**: `role: "ROLE_SELLER"` in login response.
* **Result**: `PASS`
* **Notes**: Direct seller registration bypasses Cloudinary document upload step.

---

### Test FT-13: Product Creation (Seller Workflow)
* **Test ID**: `FT-13`
* **Endpoint / Page**: `/api/v1/products`
* **HTTP Method**: `POST`
* **Authentication Requirement**: `ROLE_SELLER`
* **Request**:
  * Header: `Authorization: Bearer <seller_access_token>`
  * Form Data: `name="Wireless Mouse"`, `description="Ergonomic optical mouse"`, `price=29.99`, `categoryId=1`, `stock=50`
* **Response Status**: `201 Created`
* **Relevant Response Fields**: `{"id":"d827840d-3b1b-48c3-b19e-85437741846b","name":"Wireless Mouse","price":29.99,"stock":50}`
* **Result**: `PASS`
* **Notes**: Product UUID `d827840d-3b1b-48c3-b19e-85437741846b` generated.

---

### Test FT-14: Product Retrieval by ID
* **Test ID**: `FT-14`
* **Endpoint / Page**: `/api/v1/products/d827840d-3b1b-48c3-b19e-85437741846b`
* **HTTP Method**: `GET`
* **Authentication Requirement**: Authenticated
* **Request**: Header `Authorization: Bearer <user_access_token>`
* **Response Status**: `200 OK`
* **Relevant Response Fields**: Product detail DTO containing Wireless Mouse metadata.
* **Result**: `PASS`
* **Notes**: Confirms product catalog read operation.

---

### Test FT-15: Add Item to Cart
* **Test ID**: `FT-15`
* **Endpoint / Page**: `/api/v1/cart/add`
* **HTTP Method**: `POST`
* **Authentication Requirement**: Authenticated (`ROLE_USER`)
* **Request**:
  * Header: `Authorization: Bearer <user_access_token>`
  * Body: `{"productId":"d827840d-3b1b-48c3-b19e-85437741846b","quantity":2}`
* **Response Status**: `200 OK`
* **Relevant Response Fields**: `{"cartId":"99fec1a0-fab5-49bd-b36d-9e2cb05c43d2","items":[{"productId":"...","quantity":2}],"totalPrice":59.98}`
* **Result**: `PASS`
* **Notes**: Cart created automatically and populated with 2 items.

---

### Test FT-16: Retrieve Cart
* **Test ID**: `FT-16`
* **Endpoint / Page**: `/api/v1/cart`
* **HTTP Method**: `GET`
* **Authentication Requirement**: Authenticated
* **Request**: Header `Authorization: Bearer <user_access_token>`
* **Response Status**: `200 OK`
* **Relevant Response Fields**: Cart item array and current total (`59.98`).
* **Result**: `PASS`
* **Notes**: Confirms user cart persistence.

---

### Test FT-17: Add Product to Wishlist
* **Test ID**: `FT-17`
* **Endpoint / Page**: `/api/v1/wishlist/add/d827840d-3b1b-48c3-b19e-85437741846b`
* **HTTP Method**: `POST`
* **Authentication Requirement**: Authenticated
* **Request**: Header `Authorization: Bearer <user_access_token>`
* **Response Status**: `200 OK`
* **Relevant Response Fields**: `{"wishlistId":"08e907b6-89d3-46e3-abaa-eec74cf49e1a","products":[...]}`
* **Result**: `PASS`
* **Notes**: Product associated with user wishlist.

---

### Test FT-18: Order Placement from Cart
* **Test ID**: `FT-18`
* **Endpoint / Page**: `/api/v1/orders`
* **HTTP Method**: `POST`
* **Authentication Requirement**: Authenticated
* **Request**:
  * Header: `Authorization: Bearer <user_access_token>`
  * Body: `{"shippingAddress":"123 Main St, Tech City, NY 10001"}`
* **Response Status**: `201 Created`
* **Relevant Response Fields**: `{"id":1,"totalAmount":59.98,"status":"CREATED","shippingAddress":"123 Main St..."}`
* **Result**: `PASS`
* **Notes**: Order 1 created from active cart items.

---

### Test FT-19: Get Order Details
* **Test ID**: `FT-19`
* **Endpoint / Page**: `/api/v1/orders/1`
* **HTTP Method**: `GET`
* **Authentication Requirement**: Authenticated
* **Request**: Header `Authorization: Bearer <user_access_token>`
* **Response Status**: `200 OK`
* **Relevant Response Fields**: Order details DTO with status `CREATED` and total `59.98`.
* **Result**: `PASS`
* **Notes**: Confirms order retrieval for owner.

---

### Test FT-20: Stripe Payment Session Creation
* **Test ID**: `FT-20`
* **Endpoint / Page**: `/api/v1/payments/create/1`
* **HTTP Method**: `POST`
* **Authentication Requirement**: Authenticated
* **Request**: Header `Authorization: Bearer <user_access_token>`
* **Response Status**: `502 Bad Gateway`
* **Relevant Response Fields**: `{"message":"Stripe error while creating checkout session","status":502}`
* **Result**: `EXPECTED FAIL (ENVIRONMENT)`
* **Notes**: Failed due to placeholder Stripe API secret key (`sk_test_51MockKey...`) in `application.properties`.

---

### Test FT-21: Seller Onboarding Request (Multipart KYC Upload)
* **Test ID**: `FT-21`
* **Endpoint / Page**: `/api/v1/seller/request`
* **HTTP Method**: `POST`
* **Authentication Requirement**: Authenticated (`seller_candidate@example.com`)
* **Request**: Form Data `storeName="Awesome Tech Store"`, `document=@kyc_doc.txt`
* **Response Status**: `500 Internal Server Error`
* **Relevant Response Fields**: `{"message":"Something went wrong: Unknown API key 123456789","status":500}`
* **Result**: `EXPECTED FAIL (ENVIRONMENT)`
* **Notes**: Cloudinary upload fails because placeholder API credentials (`cloudinary.api_key=123456789`) are set in properties. Direct seller registration (`FT-12`) serves as the functional workaround for role assignment.

---

### Test FT-22: Forgot Password OTP Request
* **Test ID**: `FT-22`
* **Endpoint / Page**: `/api/v1/auth/forgot-password`
* **HTTP Method**: `POST`
* **Authentication Requirement**: None (Public)
* **Request**: Body `{"email":"func_user@example.com"}`
* **Response Status**: `200 OK`
* **Relevant Response Fields**: `{"message":"OTP sent successfully. Please check your inbox.","otp":"333157"}`
* **Result**: `PASS`
* **Notes**: 6-digit numeric OTP generated, stored in DB (15-min TTL), sent via SMTP, and returned in response payload.

---

### Test FT-23: Password Reset via OTP
* **Test ID**: `FT-23`
* **Endpoint / Page**: `/api/v1/auth/reset-password`
* **HTTP Method**: `POST`
* **Authentication Requirement**: None (Public)
* **Request**: Body `{"email":"func_user@example.com","otp":"333157","newPassword":"NewPassword123!"}`
* **Response Status**: `200 OK`
* **Relevant Response Fields**: `{"message":"Password reset successfully"}`
* **Result**: `PASS`
* **Notes**: Password updated to new BCrypt hash; OTP deleted after consumption.

---

### Test FT-24: Login Verification with Updated Password
* **Test ID**: `FT-24`
* **Endpoint / Page**: `/api/v1/auth/login`
* **HTTP Method**: `POST`
* **Authentication Requirement**: None (Public)
* **Request**: Body `{"email":"func_user@example.com","password":"NewPassword123!"}`
* **Response Status**: `200 OK`
* **Relevant Response Fields**: `message: "Login successful"`, new access & refresh tokens.
* **Result**: `PASS`
* **Notes**: Confirms password update operational success.
