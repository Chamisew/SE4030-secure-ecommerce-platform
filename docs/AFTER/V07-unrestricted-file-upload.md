# V07: Unrestricted File Upload Without MIME/Content Validation (AFTER FIX)

## 1. Vulnerability & Fix Overview

* **Vulnerability ID**: `V07`
* **Vulnerability Title**: Unrestricted File Upload Without MIME, Content, or Extension Validation
* **Assigned Member**: Member 4 — File Upload & Order Flow Specialist (`Sanjana Dinithi <it22082510@my.sliit.lk>`)
* **Branch**: `fix/D-fileupload-order-vulns`
* **CWE**: CWE-434 (Unrestricted Upload of File with Dangerous Type)
* **OWASP Top 10**: A04:2021 – Insecure Design / A03:2021 – Injection
* **Fix Status**: **VERIFIED & REMEDIATED**

---

## 2. Root Cause & Code Remediation

### 2.1 The Flaw (BEFORE)
In the original implementation:
1. `SellerServiceImpl.sendSellerRequest()` accepted a `MultipartFile document` without validating its file size, HTTP Content-Type header, or original file extension.
2. The raw file bytes were directly passed into `cloudinary.uploader().upload()` with the parameter `"resource_type", "auto"`.
3. An attacker could upload arbitrary scripts, HTML files (leading to stored Cross-Site Scripting / phishing), or malicious binaries disguised as KYC verification identity documents.

### 2.2 The Remediation (AFTER)
In [SellerServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/seller/service/Impl/SellerServiceImpl.java#L74-L114), multiple defense-in-depth layers were introduced:
1. **File Size Enforcement**:
   Enforced a strict 5MB limit:
   ```java
   if (document.getSize() > 5 * 1024 * 1024) {
       throw new IllegalArgumentException("Document size exceeds maximum allowed limit (5MB)");
   }
   ```
2. **Strict MIME Type Whitelisting**:
   The `Content-Type` is checked against an approved whitelist consisting solely of `application/pdf`, `image/jpeg`, and `image/png`. Any unsupported MIME type triggers an immediate HTTP 400 rejection:
   ```java
   List<String> allowedMimeTypes = List.of("application/pdf", "image/jpeg", "image/png");
   if (contentType == null || !allowedMimeTypes.contains(contentType.toLowerCase())) {
       throw new IllegalArgumentException("Invalid file type. Only PDF, JPEG, and PNG documents are allowed");
   }
   ```
3. **Strict Extension Whitelisting**:
   The filename extension is inspected to prevent extension bypasses and spoofed MIME types. Only `.pdf`, `.jpg`, `.jpeg`, and `.png` are permitted.
4. **Restricted Storage Resource Type**:
   Removed the hazardous `"resource_type", "auto"` and replaced it with explicit classification (`"raw"` for PDFs and `"image"` for PNG/JPEG), ensuring files cannot be executed or misinterpreted by cloud storage CDNs.

---

## 3. Re-Testing & Verification Evidence

### 3.1 Test Execution
Three attack variants were executed against the patched endpoint `/api/v1/seller/request`:
1. **HTML File Upload**: Sent `test_kyc.html` with `Content-Type: text/html`.
   * Result: **HTTP 400 Bad Request** (`"message": "Invalid file type. Only PDF, JPEG, and PNG documents are allowed"`).
2. **Spoofed Extension**: Sent `malicious_payload.exe` with spoofed `Content-Type: image/png`.
   * Result: **HTTP 400 Bad Request** (`"message": "Invalid file extension. Only .pdf, .jpg, .jpeg, and .png are allowed"`).
3. **Oversized Document**: Sent 6MB payload with `Content-Type: application/pdf`.
   * Result: **Rejected** (`"message": "Something went wrong: Maximum upload size exceeded"`).

---

## 4. Evidence File Link

Detailed execution log with full HTTP requests, response headers, and payloads:
* [evidence-log.txt](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/AFTER/V07-unrestricted-file-upload/evidence-log.txt)
