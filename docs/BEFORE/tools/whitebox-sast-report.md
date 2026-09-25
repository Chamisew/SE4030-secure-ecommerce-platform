# BEFORE-Fix Automated Security Scan: White-Box SAST Report

## Scan Metadata

| Field | Value |
| :--- | :--- |
| **Tool** | Custom SAST Pattern Scanner + Maven Dependency Tree Analysis |
| **Tool Category** | White-Box Static Application Security Testing (SAST) / Software Composition Analysis (SCA) |
| **Scanner Method** | Automated `grep`/`ripgrep` pattern matching on Java source files for known-vulnerable code patterns |
| **SCA Method** | `mvnw dependency:tree` to enumerate all direct and transitive dependencies with versions |
| **Target Codebase** | `backend/src/main/java/com/example/backend/` (Java 21, Spring Boot 4.0.0) |
| **Scan Date** | 2026-09-25T12:49:31+05:30 |
| **Application State** | Original BEFORE-fix application, unmodified |
| **OWASP Dependency-Check** | Plugin v12.1.0 invoked but NVD database download in progress (see Section 5) |

---

## Executive Summary

| Category | Findings |
| :--- | :--- |
| Insecure PRNG (Weak Randomness) | **1 finding** — `java.util.Random` used for security-sensitive OTP generation |
| Excessive Token Lifetime | **1 finding** — 60-day access token expiration |
| Internal Exception Message Exposure | **1 finding** — 33 instances of `ex.getMessage()` in error handlers |
| Overly Permissive Security Matcher | **1 finding** — `securityMatcher("/api/**")` leaves non-API routes unprotected |
| CSRF Disabled | **1 finding** — `csrf.disable()` without alternative protections |
| Deprecated/Legacy Dependencies | **1 finding** — Apache HttpClient 4.4 (2015-era, via Cloudinary SDK) |
| **Total Static Findings** | **6** |

---

## 1. Insecure Pseudo-Random Number Generator (PRNG)

### Finding: `CWE-330` — Use of Insufficiently Random Values

* **File**: [AuthServiceImpl.java:534](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/service/Impl/AuthServiceImpl.java#L534)
* **Code**:
  ```java
  String otp = String.format("%06d", new Random().nextInt(1_000_000));
  ```
* **Issue**: `java.util.Random` uses a 48-bit linear congruential generator (LCG) that is **not cryptographically secure**. An attacker who observes a few OTP values can predict future OTPs by recovering the generator state.
* **Recommendation**: Replace with `java.security.SecureRandom`:
  ```java
  String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
  ```
* **OWASP**: A02:2021 – Cryptographic Failures
* **Severity**: MEDIUM

---

## 2. Excessive JWT Access Token Lifetime

### Finding: `CWE-613` — Insufficient Session Expiration

* **File**: [JWTserviceImpl.java:43-44](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/service/Impl/JWTserviceImpl.java#L43)
* **Code**:
  ```java
  private static final long ACCESS_TOKEN_EXPIRATION =
          60L * 24 * 60 * 60 * 1000;  // 60 DAYS
  ```
* **Issue**: Access tokens are valid for **60 days** (5,184,000 seconds). Industry best practice for stateless JWTs is **15–30 minutes**. A stolen access token provides persistent unauthorized access for two months.
* **Recommendation**: Reduce access token lifetime to 15 minutes; rely on refresh tokens for session extension.
* **OWASP**: A07:2021 – Identification and Authentication Failures
* **Severity**: HIGH

---

## 3. Internal Exception Message Exposure

### Finding: `CWE-209` — Generation of Error Messages Containing Sensitive Information

* **File**: [GlobalExceptionHandler.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/exception/GlobalExceptionHandler.java)
* **Instances Found**: **33 occurrences** of `ex.getMessage()` embedded directly into HTTP response bodies.
* **Critical Pattern** (line 341):
  ```java
  return build(HttpStatus.INTERNAL_SERVER_ERROR,
               "Something went wrong: " + ex.getMessage());
  ```
* **Issue**: Raw Java exception messages are returned to external callers, disclosing internal package names (`com.example.backend`), framework class names, database driver errors, and configuration details (e.g., `Unknown API key 123456789`).
* **Recommendation**: Return generic error messages to clients; log detailed exception information server-side only.
* **OWASP**: A09:2021 – Security Logging and Monitoring Failures
* **Severity**: MEDIUM

---

## 4. Overly Narrow Security Matcher Scope

### Finding: `CWE-284` — Improper Access Control

* **File**: [SecurityConfig.java:35](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/SecurityConfig.java#L35)
* **Code**:
  ```java
  return http
      .securityMatcher("/api/**")
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
          .requestMatchers("/api/v1/auth/**").permitAll()
          .anyRequest().authenticated()
      )
  ```
* **Issue**: Spring Security filter chain is scoped exclusively to `/api/**`. Any controller mapped outside this path (e.g., `TestEmailController` at `/test-email`) is completely unprotected — no authentication, no authorization, no security headers.
* **Recommendation**: Remove `.securityMatcher("/api/**")` or define a fallback `SecurityFilterChain` with default-deny for all non-API paths.
* **OWASP**: A05:2021 – Security Misconfiguration
* **Severity**: HIGH

---

## 5. CSRF Protection Disabled

### Finding: `CWE-352` — Cross-Site Request Forgery

* **File**: [SecurityConfig.java:36](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/security/SecurityConfig.java#L36)
* **Code**:
  ```java
  .csrf(csrf -> csrf.disable())
  ```
* **Issue**: CSRF protection is globally disabled. While stateless JWT-based APIs are generally not vulnerable to traditional CSRF (since tokens are not sent as cookies), the application uses no alternative CSRF mitigation strategy. If cookies were used for session management in the future, this would be a critical vulnerability.
* **Assessment**: LOW risk in current stateless JWT architecture, but the blanket `disable()` is a security anti-pattern.
* **OWASP**: A01:2021 – Broken Access Control
* **Severity**: LOW (contextual)

---

## 6. Software Composition Analysis (SCA) — Dependency Audit

### 6.1 Direct Dependencies (from `pom.xml` via `mvnw dependency:tree`)

| Dependency | Version | Assessment |
| :--- | :--- | :--- |
| Spring Boot | 4.0.0 | Current release |
| Spring Security | 7.0.0 | Current release |
| PostgreSQL JDBC | 42.7.8 | Current release |
| JJWT (io.jsonwebtoken) | 0.12.6 | Current release |
| Hibernate Validator | 9.0.1.Final | Current release |
| Jackson Databind | 3.0.2 / 2.20.1 | Current releases |
| Tomcat Embed | 11.0.14 | Current release |
| Lombok | 1.18.42 | Current release |
| Stripe Java SDK | 31.2.0-alpha.1 | **Alpha pre-release — may have undiscovered issues** |
| Cloudinary HTTP44 | 1.32.0 | Current release |
| H2 Database | 2.4.240 | Runtime scope — **should be test-only** |

### 6.2 Legacy / Outdated Transitive Dependencies

| Dependency | Version | Concern |
| :--- | :--- | :--- |
| `org.apache.httpcomponents:httpclient` | **4.4** | 2015-era library pulled in by Cloudinary SDK. Superseded by Apache HttpClient 5.x. May contain known CVEs. |
| `org.apache.httpcomponents:httpmime` | **4.4** | Same legacy bundle. |

### 6.3 OWASP Dependency-Check Status

The OWASP Dependency-Check Maven plugin (v12.1.0) was invoked via:

```
mvnw org.owasp:dependency-check-maven:12.1.0:check -DfailBuildOnCVSS=11 -Dformats=JSON,HTML
```

**Status**: The scan requires downloading the complete NVD (National Vulnerability Database) which contains **397,489 records**. Without an NVD API key, this download takes 30–60+ minutes and was still in progress at the time of this report.

> **Note**: When the NVD download completes, the full dependency-check HTML and JSON reports will be generated at `backend/target/dependency-check-report.html` and `backend/target/dependency-check-report.json`. These should be copied to `docs/BEFORE/tools/` for inclusion in the final report.

---

## SAST Scan Pattern Summary

The following automated pattern searches were executed against the Java source tree:

| Pattern | Query | Files Scanned | Matches | Verdict |
| :--- | :--- | :---: | :---: | :--- |
| Weak PRNG | `new Random()` | All `*.java` | **1** | FINDING — `AuthServiceImpl.java:534` |
| Exception Message Exposure | `getMessage` | All `*.java` | **33** | FINDING — `GlobalExceptionHandler.java` (27), `JWTFilter.java` (2), others |
| Permit All Wildcard | `permitAll` | All `*.java` | **1** | FINDING — `SecurityConfig.java:39` |
| CSRF Disabled | `csrf` + `disable` | All `*.java` | **1** | FINDING — `SecurityConfig.java:36` |
| Token Lifetime Constants | `ACCESS_TOKEN_EXPIRATION` | All `*.java` | **3** | FINDING — 60-day access token |
| Hardcoded Secrets | `hardcoded` | All `*.java` | **0** | No hardcoded secrets found in source |

---

## Limitations

1. **Not a Full SAST Tool**: This is a pattern-matching scanner, not a full abstract syntax tree (AST) analyzer like SpotBugs, SonarQube, or Semgrep. It cannot detect data-flow vulnerabilities (e.g., taint tracking for SQL injection).
2. **No NVD CVE Matching (Yet)**: The OWASP Dependency-Check NVD download was in progress. Once complete, it will provide CVE-level vulnerability matching for all 80+ transitive dependencies.
3. **False Positives**: Some `getMessage()` calls may be in appropriately handled exception paths (e.g., logging). Manual review is recommended to distinguish between client-facing and server-side-only usage.
4. **Scope**: Only Java backend source code was analyzed. The Angular frontend was not scanned.
