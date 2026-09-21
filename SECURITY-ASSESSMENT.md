# SE4030 Secure Software Development
# Security Assessment

## 1. Project Information

**Project:** Faresaymann/ecommerce-platform

**Original Repository:**  
https://github.com/Faresaymann/ecommerce-platform

**Group Repository:**  
https://github.com/Chamisew/SE4030-secure-ecommerce-platform.git

**Course:** SE4030 – Secure Software Development

---

## 2. Assessment Purpose

This repository is being used for the SE4030 Secure Software Development
group assignment.

The purpose of this assessment is to identify security vulnerabilities
in the original application, document evidence of the vulnerabilities,
implement appropriate fixes, and evaluate the security of the improved
application.

An OAuth/OpenID Connect based feature will also be implemented as part
of the assignment.

---

## 3. Original Application Baseline

The original application has been preserved before any security-related
modifications were made.

**Baseline tag:** `original-baseline`

**Original commit:** [ADD COMMIT HASH]

**Original commit date:** [ADD COMMIT DATE]

**Assessment baseline:** The security assessment will be performed
against the version identified by the `original-baseline` tag.

No application source code has been modified at the baseline stage.

---

## 4. Technology Stack

The application consists of:

- Java / Spring Boot backend
- Spring Security
- JWT-based authentication
- PostgreSQL
- Spring Data JPA / Hibernate
- Angular frontend
- Cloudinary for file storage
- Stripe for payment processing
- Spring Mail for email functionality

---

## 5. Security Testing Approach

The security assessment will use both white-box and black-box
security testing techniques.

### White-box testing

The source code will be reviewed to identify potential security
weaknesses in areas including:

- Authentication
- Authorization
- JWT implementation
- Input validation
- File uploads
- Payment logic
- Error handling
- Configuration
- Dependency management
- Secret management

### Black-box testing

The running application will be tested using:

- Manual API testing
- Postman or equivalent API testing tools
- OWASP ZAP
- OWASP Dependency-Check
- Browser-based testing where applicable

Potential vulnerabilities identified during static analysis will be
validated against the original running application before being
classified as confirmed findings.

---

## 6. Vulnerability Tracking

The following table will be updated during the security assessment.

| ID | Vulnerability | Category | Status | Evidence |
|---|---|---|---|---|
| V01 | TBD | TBD | Pending | TBD |
| V02 | TBD | TBD | Pending | TBD |
| V03 | TBD | TBD | Pending | TBD |
| V04 | TBD | TBD | Pending | TBD |
| V05 | TBD | TBD | Pending | TBD |
| V06 | TBD | TBD | Pending | TBD |
| V07 | TBD | TBD | Pending | TBD |

---

## 7. Important Note

The vulnerabilities listed during initial reconnaissance are considered
candidate findings until they are reproduced and supported by evidence.

No security fixes will be applied to the original baseline until the
security assessment and evidence collection have been completed.

---

## 8. Team Members

| Name | Student Index | Responsibility |
|---|---|---|
| Member 1 | TBD | Authentication / Account Security |
| Member 2 | TBD | Authorization / Access Control |
| Member 3 | TBD | File Upload / API / Configuration |
| Member 4 | TBD | JWT / Payment / Dependencies |

---

## 9. Assessment Status

**Current phase:** Baseline preservation and security reconnaissance

**Application source code modified:** No

**Security fixes implemented:** No

**OAuth/OIDC implemented:** No
