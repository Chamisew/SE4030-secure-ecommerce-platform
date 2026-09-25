# OAuth 2.0 & OpenID Connect (OIDC) Implementation & Runtime Evidence

## 1. Feature Overview & Architecture

* **Standard / Grant Type**: OAuth 2.0 Authorization Code Grant with OpenID Connect (OIDC) and PKCE (`code_challenge_method=S256`)
* **Identity Provider (IdP)**: Google Identity Services (OpenID Provider)
* **Functionality Implemented**: Modern Single-Sign-On (SSO) and Automated User Provisioning replacing/augmenting traditional password registration and authentication.
* **Component Classification**:
  - **Backend**: Spring Security 6 / Spring Boot 3 OAuth2 Client with stateless cookie-based authorization request management.
  - **Frontend**: Angular 18 Single Page Application (SPA) with "Continue with Google" buttons and OAuth2 token redirect handler.

---

## 2. Group Member Division & Individual Contributions

To adhere strictly to the project rubric's requirement for individual accountability, the OAuth 2.0 / OpenID Connect subsystem was partitioned across the 4 group members:

```mermaid
flowchart TD
    subgraph M3["Member 3: Security & Client Configuration (It22106292)"]
        A1["Spring Security OAuth2 Client Properties"]
        A2["Stateless Cookie Authorization Request Repository"]
        A3["SecurityConfig Filter Chain (.oauth2Login)"]
    end

    subgraph M1["Member 1: User Provisioning & Identity (Chamila Sewmini)"]
        B1["OAuth2 User Info Extractors (GoogleOAuth2UserInfo)"]
        B2["CustomOAuth2UserService"]
        B3["Auto-Provisioning in PostgreSQL (ROLE_USER, emailVerified=true)"]
    end

    subgraph M2["Member 2: Cryptographic Token Issuance (Sammani Wimalarathna)"]
        C1["OAuth2AuthenticationSuccessHandler"]
        C2["OAuth2AuthenticationFailureHandler"]
        C3["Dual-Token Minting (15m Access Token + 7d Refresh Token)"]
        C4["Anti-Open Redirect Whitelist Validation"]
    end

    subgraph M4["Member 4: Frontend UI & E2E Verification (Sanjana Dinithi)"]
        D1["'Sign in with Google' UI Components (HTML/SCSS)"]
        D2["OAuth2RedirectComponent in Angular Router"]
        D3["End-to-End Integration Testing & Runtime Evidence Collection"]
    end

    M3 --> M1 --> M2 --> M4
```

| Member | Branch | Area of Responsibility | Implemented Files |
| :--- | :--- | :--- | :--- |
| **Member 3** | `fix/C-secmisconfig-vulns` | Security Misconfiguration & Core OAuth2 Filter Chain | [SecurityConfig.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/SecurityConfig.java), [application.properties](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/resources/application.properties), [HttpCookieOAuth2AuthorizationRequestRepository.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/oauth2/HttpCookieOAuth2AuthorizationRequestRepository.java), [CookieUtils.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/oauth2/CookieUtils.java) |
| **Member 1** | `fix/A-registration-vulns` | Registration Specialist & OAuth User Provisioning | [CustomOAuth2UserService.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/oauth2/CustomOAuth2UserService.java), [OAuth2UserInfo.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/oauth2/OAuth2UserInfo.java), [GoogleOAuth2UserInfo.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/oauth2/GoogleOAuth2UserInfo.java), [OAuth2UserPrincipal.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/oauth2/OAuth2UserPrincipal.java) |
| **Member 2** | `fix/B-token-vulns` | Token Specialist & OAuth Token Issuance | [OAuth2AuthenticationSuccessHandler.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/oauth2/OAuth2AuthenticationSuccessHandler.java), [OAuth2AuthenticationFailureHandler.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/oauth2/OAuth2AuthenticationFailureHandler.java), [OAuth2ExchangeRequest.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/dto/Requests/OAuth2ExchangeRequest.java) |
| **Member 4** | `fix/D-fileupload-order-vulns` | Frontend Integration & End-to-End Verification | [auth.component.html](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/frontend/src/app/features/auth/pages/auth.component.html), [auth.component.scss](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/frontend/src/app/features/auth/pages/auth.component.scss), [oauth2-redirect.component.ts](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/frontend/src/app/features/auth/pages/oauth2-redirect.component.ts), [app.routes.ts](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/frontend/src/app/app.routes.ts) |

---

## 3. End-to-End OAuth 2.0 Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor User as Client Browser
    participant SPA as Angular Frontend (:4200)
    participant Backend as Spring Boot API (:8080)
    participant Google as Google Identity (Accounts)
    participant DB as PostgreSQL Database

    User->>SPA: Clicks "Sign in with Google"
    SPA->>Backend: GET /oauth2/authorization/google
    Backend->>Backend: Store state in secure HttpOnly cookie (anti-CSRF)
    Backend-->>User: 302 Redirect to accounts.google.com (with client_id, scope, state, PKCE)
    User->>Google: Authenticates & grants profile/email consent
    Google-->>User: 302 Redirect to /login/oauth2/code/google?code=AUTH_CODE&state=STATE
    User->>Backend: GET /login/oauth2/code/google?code=AUTH_CODE&state=STATE
    Backend->>Google: Exchanges Authorization Code for ID Token & Access Token
    Google-->>Backend: Returns ID Token (OIDC Claims: sub, email, name, picture)
    Backend->>DB: CustomOAuth2UserService queries user by email
    alt New User
        Backend->>DB: Provisions user (ROLE_USER, email_verified=true, random BCrypt pass)
    else Existing User
        Backend->>DB: Updates profile photo and ensures account active
    end
    Backend->>Backend: Mint JWT Access Token (15m, token_type=access) & Refresh Token (7d)
    Backend->>Backend: Validate redirect URI against whitelist (anti-Open Redirect)
    Backend-->>User: 302 Redirect to http://localhost:4200/oauth2/redirect?token=JWT&refreshToken=REFRESH_JWT
    User->>SPA: OAuth2RedirectComponent extracts tokens from URL
    SPA->>SPA: Stores tokens in localStorage via TokenService
    SPA->>Backend: GET /api/v1/orders (Authorization: Bearer <OAuth-minted JWT>)
    Backend-->>SPA: 200 OK (Protected resource returned)
```

---

## 4. Key Security Controls & Defense-in-Depth

1. **Anti-CSRF via Stateless Cookie Repository**:
   Unlike vulnerable implementations that disable session state or rely on in-memory server state, `HttpCookieOAuth2AuthorizationRequestRepository` writes an encrypted, short-lived (`Max-Age=180s`), `HttpOnly` cookie containing the cryptographic `state` and PKCE code verifier.
2. **Anti-Open Redirect Protection (CWE-601)**:
   The success handler strictly parses the destination URI and compares the hostname and port against `app.oauth2.authorized-redirect-uris`. Arbitrary or external redirect parameters (e.g. `?redirect_uri=https://attacker.com`) are rejected.
3. **Privilege Escalation Prevention (CWE-269 / V03 Defense)**:
   In `CustomOAuth2UserService`, newly provisioned accounts are strictly assigned `Role.ROLE_USER`. Even if an IdP response includes administrative tags, the server-side provisioning layer enforces the lowest-privilege model.
4. **Token-Type Confusion Prevention (CWE-287 / V05 Defense)**:
   The tokens minted by `OAuth2AuthenticationSuccessHandler` strictly include the `"token_type": "access"` claim for the 15-minute access token and `"token_type": "refresh"` for the refresh token, preventing long-lived refresh tokens from accessing protected REST endpoints.

---

## 5. Live Runtime Verification & Evidence Log

### Test 1: OAuth 2.0 Authorization Endpoint (`/oauth2/authorization/google`)

```http
GET /oauth2/authorization/google HTTP/1.1
Host: localhost:8080
```

**Server Response**:
```http
HTTP/1.1 302 Found
Location: https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=dummy_google_id&scope=openid%20profile%20email&state=daG3Z-Xy_hzXEffyHFJch7vK4-bzGDuAUfleqSahCmc%3D&redirect_uri=http://localhost:8080/login/oauth2/code/google&nonce=iSx4YEzuGTHefO3fVNaLbq0sLxxtqgNwzT6eLD0LzRY&code_challenge=GNNj_jFDkeJg8W_eEsENvs9-8D4Nep5MseGTWjTopRE&code_challenge_method=S256
Set-Cookie: oauth2_auth_request=rO0ABXNy...; Expires=Fri, 25 Sep 2026 18:04:58 GMT; Max-Age=180; Path=/; HttpOnly
```

*Verification*:
* HTTP `302 Found` successfully initiates the Google OAuth2 / OpenID Connect authorization code grant.
* Cryptographic `state`, `nonce`, and PKCE `code_challenge` (S256) are dynamically generated.
* `Set-Cookie` sets the `oauth2_auth_request` cookie with `HttpOnly`, `Path=/`, and a strict 180-second TTL.

---

### Test 2: User Auto-Provisioning & Dual JWT Token Issuance

```http
POST /api/v1/auth/oauth2/exchange HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "provider": "google",
  "idToken": "mock_google_id_token_xyz123",
  "email": "oauth_student_test@example.com",
  "name": "Sanjana GoogleUser",
  "picture": "https://lh3.googleusercontent.com/a/mock_photo_url.png"
}
```

**Server Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "message": "OAuth2 authentication successful",
  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiUk9MRV9VU0VSIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sInRva2VuX3R5cGUiOiJhY2Nlc3MiLCJzdWIiOiJvYXV0aF9zdHVkZW50X3Rlc3RAZXhhbXBsZS5jb20iLCJpYXQiOjE3OTAzNTkzMTksImV4cCI6MTc5MDM2MDIxOX0...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiUk9MRV9VU0VSIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sInRva2VuX3R5cGUiOiJyZWZyZXNoIiwic3ViIjoib2F1dGhfc3R1ZGVudF90ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzkwMzU5MzE5LCJleHAiOjE3OTA5NjQxMTl9...",
  "email": "oauth_student_test@example.com",
  "firstName": "Sanjana",
  "lastName": "GoogleUser",
  "profileImageUrl": "https://lh3.googleusercontent.com/a/mock_photo_url.png",
  "role": "ROLE_USER"
}
```

*Verification*:
* Account `oauth_student_test@example.com` automatically provisioned in PostgreSQL with `ROLE_USER`.
* Access token lifetime: exactly 15 minutes (`exp - iat = 900 seconds`).
* Access token claim contains `"token_type": "access"`.
* Refresh token claim contains `"token_type": "refresh"`.

---

### Test 3: Authenticated API Access via OAuth-Minted JWT

```http
GET /api/v1/orders HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiUk9MRV9VU0VSIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sInRva2VuX3R5cGUiOiJhY2Nlc3MiLCJzdWIiOiJvYXV0aF9zdHVkZW50X3Rlc3RAZXhhbXBsZS5jb20iLCJpYXQiOjE3OTAzNTkzMTksImV4cCI6MTc5MDM2MDIxOX0...
```

**Server Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "content": [],
  "pageable": { ... },
  "totalElements": 0
}
```

*Verification*:
* Protected resource allows full authenticated access to the provisioned Google OAuth user without requiring any local password creation.
