# BEFORE-Fix Automated Security Scan: OWASP Dependency-Check Report

## Scan Metadata

| Field | Value |
| :--- | :--- |
| **Tool** | OWASP Dependency-Check Maven Plugin |
| **Tool Version** | 12.1.0 |
| **Tool Category** | Software Composition Analysis (SCA) / White-Box Dependency Vulnerability Scanner |
| **Target** | `backend/pom.xml` (`com.example:backend:0.0.1-SNAPSHOT`) |
| **Scan Execution Date** | 2026-09-25 |
| **Application State** | Original BEFORE-fix application (unmodified) |
| **Command** | `mvnw org.owasp:dependency-check-maven:12.1.0:check -DfailBuildOnCVSS=11 -Dformats=JSON,HTML` |
| **HTML Report File** | [owasp-dependency-check-report.html](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/BEFORE/tools/owasp-dependency-check-report.html) |
| **JSON Report File** | [owasp-dependency-check-report.json](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/BEFORE/tools/owasp-dependency-check-report.json) |

---

## Executive Summary

The scan analyzed all direct and transitive Maven dependencies declared in `backend/pom.xml`.

| Metric | Value |
| :--- | :--- |
| **Total Dependencies Analyzed** | 126 JAR files |
| **Vulnerable Dependencies Identified** | **11** |
| **Total Vulnerability Matches (CVEs)** | **128** |
| **Primary Risk Vectors** | Legacy HTTP client (Apache HttpClient 4.4), Embedded Servlet Engine (Tomcat 11.0.14), Spring Framework & Security libraries |

---

## Vulnerable Dependencies Breakdown

| # | Dependency Artifact | Version | CVE Count | Primary Findings & Context |
| :-: | :--- | :---: | :-: | :--- |
| 1 | `httpclient-4.4.jar` | 4.4 | 1 | Pulled transitively by `cloudinary-http44:1.32.0`. Legacy Apache HTTP client (2015) containing known vulnerabilities. |
| 2 | `tomcat-embed-core-11.0.14.jar` | 11.0.14 | 17 | Embedded web server component; flagged for HTTP request handling and WebSocket parsing CVEs. |
| 3 | `spring-core-7.0.1.jar` | 7.0.1 | 33 | Core Spring Framework container library; contains matches related to resource handling and deserialization. |
| 4 | `spring-security-core-7.0.0.jar` | 7.0.0 | 19 | Spring Security authentication/authorization library. |
| 5 | `spring-security-oauth2-core-7.0.0.jar` | 7.0.0 | 19 | OAuth2 protocol handling core library. |
| 6 | `spring-boot-data-jpa-4.0.0.jar` | 4.0.0 | 3 | Spring Boot Data JPA integration module. |
| 7 | `spring-boot-web-server-4.0.0.jar` | 4.0.0 | 2 | Web server autoconfiguration and embedded server orchestration. |
| 8 | `spring-data-jpa-4.0.0.jar` | 4.0.0 | 1 | JPA repository abstraction layer. |
| 9 | `postgresql-42.7.8.jar` | 42.7.8 | 1 | PostgreSQL JDBC driver. |
| 10 | `hibernate-validator-9.0.1.Final.jar` | 9.0.1.Final | 1 | Bean Validation provider. |
| 11 | `angus-activation-2.0.3.jar` | 2.0.3 | 1 | Jakarta Activation runtime component. |

---

## Key Dependency Vulnerability Observations

1. **Cloudinary Transitive Dependency (`httpclient-4.4`)**:
   - `cloudinary-http44:1.32.0` relies on `org.apache.httpcomponents:httpclient:4.4`, a decade-old version.
   - Modern Spring applications should either exclude the legacy `httpclient` and supply an updated version or switch to a modern HTTP transport.

2. **Spring 7.x / Spring Boot 4.x Matches**:
   - The project uses early/milestone versions of Spring Framework 7.x and Spring Boot 4.x.
   - Some CVE matches may represent CPE (Common Platform Enumeration) identifier matches against broad Spring ecosystem CVEs rather than specific defects present in 7.0.x (a known characteristic of NVD keyword-based CPE matching).

3. **Database Driver (`postgresql:42.7.8`)**:
   - PostgreSQL JDBC driver version 42.7.8 is relatively recent; the matched CVE should be reviewed against specific deployment scenarios (e.g., untrusted connection properties).

---

## Limitations

1. **Sonatype OSS Index Rate Limiting**:
   - During analysis, Sonatype's free tier OSS Index endpoint (`https://ossindex.sonatype.org/api/v3/component-report`) returned `401 Unauthorized` due to unauthenticated request limits. The scan fell back to local NVD database correlation.
2. **CPE Matching False Positives**:
   - Dependency-Check correlates package coordinates against NIST NVD CPE entries. For bleeding-edge versions (e.g., Spring 7.0.x), CPE ranges sometimes match generically, requiring manual validation before remediation.
3. **Runtime vs. Compile Time**:
   - The presence of a vulnerable library in the classpath does not guarantee that the vulnerable code path is callable by untrusted user input at runtime.

---

## Artifact Links

- Full Interactive HTML Report: [owasp-dependency-check-report.html](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/BEFORE/tools/owasp-dependency-check-report.html)
- Machine-Readable JSON Export: [owasp-dependency-check-report.json](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/BEFORE/tools/owasp-dependency-check-report.json)
