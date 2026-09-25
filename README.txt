================================================================================
SE4030 - Secure Software Development | Group Assignment Submission
================================================================================

1. GROUP MEMBERS & INDEX NUMBERS
--------------------------------------------------------------------------------
1. Chamila Sewmini        - IT22176424 (Leader / Registration Specialist)
2. Sammani Wimalarathna   - IT22081834 (Token & Cryptography Specialist)
3. H.M.D.K. Herath        - IT22106292 (Security Misconfiguration Specialist)
4. Sanjana Dinithi        - IT22082510 (File Upload & Order Logic Specialist)

*Note: Please adjust index numbers/names above if your official university registration differs.

2. GITHUB REPOSITORY LINKS
--------------------------------------------------------------------------------
Original Project Repository:
https://github.com/Chamisew/SE4030-secure-ecommerce-platform/tree/main

Modified Project Repository (With Full Detailed Git Commit History):
https://github.com/Chamisew/SE4030-secure-ecommerce-platform

Individual Member Contribution Branches:
- Member 1: fix/A-registration-vulns (V03, V04, OAuth User Provisioning)
- Member 2: fix/B-token-vulns (V02, V05, OAuth Token Issuance)
- Member 3: fix/C-secmisconfig-vulns (V01, V06, OAuth Security Config)
- Member 4: fix/D-fileupload-order-vulns (V07, V08, OAuth Frontend & Verification)

3. YOUTUBE VIDEO DEMONSTRATION LINK
--------------------------------------------------------------------------------
YouTube Video URL (Maximum 20 Minutes):
https://youtu.be/PLACEHOLDER_VIDEO_LINK_HERE

Video Breakdown Timestamps:
00:00 - 02:30 : Project Architecture, Scope, and Threat Modeling Overview
02:30 - 06:00 : Security Testing Tools Execution (SAST, DAST - OWASP ZAP, SCA - Dependency-Check)
06:00 - 09:30 : Member 1 & 2 Vulnerability Remediations & Runtime Evidence (V01-V04)
09:30 - 13:00 : Member 3 & 4 Vulnerability Remediations & Runtime Evidence (V05-V08)
13:00 - 17:30 : OAuth 2.0 / OpenID Connect Implementation (Authorization Code Grant with PKCE)
17:30 - 20:00 : Unfixed Architectural Vulnerabilities, SSDLC Best Practices, and Conclusion

4. SUMMARY OF REMEDIATED VULNERABILITIES (V01 - V08)
--------------------------------------------------------------------------------
- V01 (CWE-200, CWE-215): Exposed Development Endpoints & Password Hash Disclosure
  Fixed by: Restricting dev routes to ROLE_ADMIN, redacting passwords, replacing wildcard matchers.
- V02 (CWE-319, CWE-330): Cleartext OTP Exposure & Cryptographically Weak PRNG
  Fixed by: Suppressing OTP in HTTP response, upgrading to java.security.SecureRandom.
- V03 (CWE-269, CWE-732): Client-Controlled Role Assignment via Registration Parameter
  Fixed by: Stripping role parameter from DTO/controller, hardcoding newly registered accounts to ROLE_USER.
- V04 (CWE-200): Email Verification Token Disclosed in HTTP Registration Body
  Fixed by: Omitting verification token from RegisterResponse; sending strictly via outbound SMTP email.
- V05 (CWE-287, CWE-613): JWT Token-Type Confusion (Refresh Token used as Access Token)
  Fixed by: Enforcing explicit "token_type": "access" claim check in JWTFilter, shortening access token TTL to 15m.
- V06 (CWE-306, CWE-862): Unauthenticated Arbitrary Email Trigger via Security Matcher Bypass
  Fixed by: Removing restrictive securityMatcher, enforcing default-deny, requiring ROLE_ADMIN for test endpoints.
- V07 (CWE-434): Unrestricted File Upload Without MIME, Content, or Extension Validation
  Fixed by: Strict 5MB ceiling, strict MIME whitelist (PDF/JPEG/PNG), strict extension check, non-executable resource_type.
- V08 (CWE-840, CWE-799): Business Logic Inventory Management Flaw (Stock Inflation)
  Fixed by: Validating and deducting stock on order creation from cart, ensuring idempotent stock restoration on cancellation.

5. OAUTH 2.0 / OPENID CONNECT (OIDC) IMPLEMENTATION
--------------------------------------------------------------------------------
- Standard: OAuth 2.0 Authorization Code Grant with OpenID Connect (OIDC) & PKCE (S256).
- Identity Provider: Google Identity Services (OpenID Provider).
- Backend: CustomOAuth2UserService, HttpCookieOAuth2AuthorizationRequestRepository, OAuth2AuthenticationSuccessHandler.
- Frontend: Angular Single Page Application with "Sign in with Google" UI and /oauth2/redirect token capture handler.
================================================================================
