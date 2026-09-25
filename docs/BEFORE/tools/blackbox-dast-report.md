# BEFORE-Fix Automated Security Scan: Black-Box DAST Report

## Scan Metadata

| Field | Value |
| :--- | :--- |
| **Tool** | Python HTTP Security Scanner (Custom DAST) |
| **Tool Version** | 1.0.0 |
| **Tool Category** | Black-Box Dynamic Application Security Testing (DAST) |
| **Python Version** | 3.13.7 (MSC v.1944 64 bit AMD64) |
| **Target** | `http://localhost:8080` (Spring Boot 4.0.0 / Tomcat 11.0.14) |
| **Scan Date** | 2026-09-25T12:46:59+05:30 |
| **Application State** | Original BEFORE-fix application, unmodified |
| **Command** | `python dast_scanner.py` |

---

## Executive Summary

| Metric | Value |
| :--- | :--- |
| Total Checks Performed | **57** |
| Findings (Security Issues Detected) | **6** |
| Passed (Secure Configuration) | **51** |
| Errors | **0** |

The automated scanner identified **6 findings** across 6 distinct security check categories. The HIGH-severity findings corroborate vulnerabilities V01 (unauthenticated dev endpoint) and V06 (unauthenticated test-email endpoint) identified during manual testing.

---

## 1. Security Header Audit

### 1.1 `/api/v1/products` (HTTP 401 — protected by Spring Security)

| Header | Status | Value |
| :--- | :---: | :--- |
| `X-Content-Type-Options` | **PRESENT** | `nosniff` |
| `X-Frame-Options` | **PRESENT** | `DENY` |
| `Cache-Control` | **PRESENT** | `no-cache, no-store, max-age=0, must-revalidate` |
| `X-XSS-Protection` | **PRESENT** | `0` |
| `Strict-Transport-Security` | **MISSING** | — |
| `Content-Security-Policy` | **MISSING** | — |
| `Referrer-Policy` | **MISSING** | — |
| `Permissions-Policy` | **MISSING** | — |

**Severity**: MEDIUM — 4 of 8 standard security headers are missing. Endpoints within the `/api/**` filter chain receive basic Spring Security defaults but lack HSTS, CSP, Referrer-Policy, and Permissions-Policy.

### 1.2 `/` (HTTP 500 — outside Spring Security filter chain)

| Header | Status |
| :--- | :---: |
| All 8 headers | **MISSING** |

**Severity**: HIGH — Routes outside `/api/**` completely bypass Spring Security and receive zero security headers, confirming the architectural `securityMatcher("/api/**")` misconfiguration documented in V06.

---

## 2. Unauthenticated Endpoint Exposure Scan

23 public-facing endpoints were probed without any authentication credentials.

### HIGH-Severity Findings

| Endpoint | HTTP Status | Accessible | Content Length | Finding |
| :--- | :---: | :---: | :---: | :--- |
| `GET /test-email` | **200 OK** | **YES** | 11 bytes | Triggers outbound SMTP dispatch without authentication (V06) |
| `GET /api/v1/auth/dev/users` | **200 OK** | **YES** | 8,242 bytes | Returns full user database dump including BCrypt password hashes (V01) |

### Correctly Protected Endpoints

| Endpoint | HTTP Status | Assessment |
| :--- | :---: | :--- |
| `GET /api/v1/products` | 401 | Auth required — PASS |
| `GET /actuator` | 500 | Not deployed — PASS |
| `GET /actuator/health` | 500 | Not deployed — PASS |
| `GET /actuator/env` | 500 | Not deployed — PASS |
| `GET /actuator/beans` | 500 | Not deployed — PASS |
| `GET /actuator/configprops` | 500 | Not deployed — PASS |
| `GET /actuator/mappings` | 500 | Not deployed — PASS |
| `GET /swagger-ui.html` | 500 | Not deployed — PASS |
| `GET /swagger-ui/index.html` | 500 | Not deployed — PASS |
| `GET /v3/api-docs` | 500 | Not deployed — PASS |
| `GET /v2/api-docs` | 500 | Not deployed — PASS |
| `GET /h2-console` | 500 | Not deployed — PASS |
| `GET /heapdump` | 500 | Not deployed — PASS |
| `GET /threaddump` | 500 | Not deployed — PASS |
| `GET /loggers` | 500 | Not deployed — PASS |
| `GET /metrics` | 500 | Not deployed — PASS |

---

## 3. Authentication Enforcement Check

10 protected API endpoints were tested without any `Authorization` header.

| Method | Endpoint | HTTP Status | Enforcement | Severity |
| :--- | :--- | :---: | :--- | :---: |
| GET | `/api/v1/auth/me` | 500 | ENFORCED* | PASS |
| GET | `/api/v1/orders` | 401 | ENFORCED | PASS |
| GET | `/api/v1/cart` | 401 | ENFORCED | PASS |
| GET | `/api/v1/wishlist` | 401 | ENFORCED | PASS |
| GET | `/api/v1/seller/seller-requests` | 401 | ENFORCED | PASS |
| GET | `/api/v1/categories` | 401 | ENFORCED | PASS |
| POST | `/api/v1/orders` | 401 | ENFORCED | PASS |
| POST | `/api/v1/seller/request` | 401 | ENFORCED | PASS |
| PATCH | `/api/v1/orders/1/cancel` | 401 | ENFORCED | PASS |
| DELETE | `/api/v1/cart/clear` | 401 | ENFORCED | PASS |

**Result**: All tested `/api/**` endpoints correctly reject unauthenticated requests with HTTP 401.

*Note: `/api/v1/auth/me` returns HTTP 500 (internal exception when no JWT is in the request), which is functional but should ideally return 401.

---

## 4. Information Disclosure Check

Error responses were tested for patterns that could leak internal implementation details.

| Test Case | HTTP Status | Disclosed Patterns | Severity |
| :--- | :---: | :--- | :---: |
| Invalid UUID path parameter | 500 | `Exception`, `com.example.backend` | MEDIUM |
| Non-existent resource ID | 401 | — | PASS |
| Malformed JSON body | 500 | `Exception` | MEDIUM |
| Invalid verification token | 500 | `Exception`, `com.example.backend` | MEDIUM |
| Non-existent route | 500 | — | PASS |

**Finding**: The global exception handler (`GlobalExceptionHandler.java:341`) exposes raw Java exception messages in HTTP 500 responses, disclosing internal package names (`com.example.backend`) and framework exception class names. This aids attackers in fingerprinting the application stack.

---

## 5. CORS Configuration Check

| Test Origin | ACAO Header | ACAC Header | Assessment | Severity |
| :--- | :--- | :--- | :--- | :---: |
| `http://localhost:3000` | `http://localhost:3000` | `true` | Secure — whitelisted origin | PASS |
| `http://evil.com` | `null` | `null` | Secure — not reflected | PASS |
| `https://attacker.example.com` | `null` | `null` | Secure — not reflected | PASS |
| `null` | `null` | `null` | Secure — not reflected | PASS |

**Result**: CORS is correctly configured with explicit origin whitelisting. Untrusted origins are properly rejected.

---

## 6. HTTP Method Enumeration

| Method | Endpoints Tested | Finding |
| :--- | :--- | :--- |
| `OPTIONS` | `/api/v1/products`, `/api/v1/auth/login`, `/` | Returns 200/405 — INFO (normal for CORS preflight) |
| `TRACE` | All 3 endpoints | HTTP 405 — PASS (TRACE correctly disabled) |
| `PUT` | All 3 endpoints | HTTP 401/405/500 — PASS |
| `DELETE` | All 3 endpoints | HTTP 401/405/500 — PASS |

**Result**: No dangerous HTTP methods are enabled. TRACE is correctly rejected.

---

## Limitations

1. **No Active Exploitation**: This scanner performs passive probing only. It does not exploit vulnerabilities like SQL injection, XSS, or business logic flaws.
2. **No Authentication Testing**: The scanner did not test authenticated endpoints with valid JWTs. Vulnerabilities like V03 (role escalation), V04 (token disclosure), V05 (token-type confusion), V07 (file upload), and V08 (inventory manipulation) require authenticated multi-step attack sequences that are beyond the scope of automated DAST scanning.
3. **No Spider/Crawler**: The scanner tests only predefined URL paths. It does not discover endpoints automatically.
4. **Scope**: Only the backend REST API at `http://localhost:8080` was scanned. The Angular frontend was not tested.

---

## Raw Data

Machine-readable JSON report: [blackbox-dast-scan.json](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/BEFORE/tools/blackbox-dast-scan.json)
