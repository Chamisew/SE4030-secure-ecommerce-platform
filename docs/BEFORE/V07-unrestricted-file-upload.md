# V07: Unrestricted File Upload Without MIME, Content, or Extension Validation

## 1. Vulnerability Overview
* **ID**: `V07`
* **Title**: Unrestricted File Upload Without MIME, Content, or Extension Validation
* **Severity Rationale**: **HIGH**. The seller onboarding endpoint (`POST /api/v1/seller/request`) accepts arbitrary multipart files intended as KYC/verification documents (`MultipartFile document`). Neither the controller nor the service performs any validation regarding file extensions (e.g., whitelist check for `.pdf`, `.png`, `.jpg`), MIME types (`Content-Type`), or magic-byte content signatures. The file is uploaded directly to external cloud storage with `resource_type: auto`. An authenticated user can upload arbitrary file types (e.g., HTML files containing executable JavaScript, SVG vectors, executable binaries, scripts), posing risks of Stored Cross-Site Scripting (XSS), phishing hosting, or malware distribution via trusted application URLs.
* **Affected Endpoint**: `/api/v1/seller/request`
* **HTTP Method**: `POST`
* **Authentication Requirement**: Authenticated (`ROLE_USER`)

---

## 2. Source-Code Location & Implementation Analysis

### 2.1 Controller Implementation
In `SellerController.java`:
* **File Link**: [SellerController.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/seller/Controller/SellerController.java#L49-L64)
* **Code Snippet**:
  ```java
  @PostMapping(value = "/request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SellerRequestResponse> requestSeller(
          Authentication authentication,
          @RequestParam String storeName,
          @RequestParam(required = false) String reason,
          @RequestPart MultipartFile document
  ) throws IOException {

      String email = authentication.getName();
      return ResponseEntity.ok(
              sellerService.requestSeller(email, storeName, reason, document)
      );
  }
  ```

### 2.2 Service Implementation
In `SellerServiceImpl.java`:
* **File Link**: [SellerServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/seller/service/Impl/SellerServiceImpl.java#L73-L85)
* **Code Snippet**:
  ```java
  String documentUrl = null;
  if (document != null && !document.isEmpty()) {
      Map upload = cloudinary.uploader().upload(
              document.getBytes(),
              ObjectUtils.asMap(
                      "folder", "SellerRequests",
                      "public_id", userEmail + "_" + UUID.randomUUID(),
                      "overwrite", true,
                      "resource_type", "auto" // AUTO PERMITS ANY FILE TYPE
              )
      );
      documentUrl = (String) upload.get("secure_url");
  }
  ```
* **Root Cause Analysis**: There is a complete absence of input validation on the uploaded `MultipartFile`. The server checks neither `document.getContentType()` nor the file name extension, nor does it inspect magic numbers. By configuring `"resource_type": "auto"`, any uploaded media type, document, or script is forwarded directly to storage.

### 2.3 Additional Affected Upload Locations
Similar unvalidated file upload patterns exist across the application:
* [AuthServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/auth/service/Impl/AuthServiceImpl.java#L98-L105): Profile image upload during registration (`cloudinary.uploader().upload(file.getBytes(), ...)`).
* [ProductServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/Product/service/Impl/ProductServiceImpl.java#L89-L95): Product image upload (`cloudinary.uploader().upload(file.getBytes(), ...)`).

---

## 3. Runtime Verification & Test Evidence

### 3.1 Exact Test Request
```bash
curl -i -X POST http://localhost:8080/api/v1/seller/request \
  -H "Authorization: Bearer [REDACTED_USER_JWT]" \
  -F "storeName=Audit Store V07" \
  -F "reason=KYC verification test" \
  -F "document=@test_kyc.html;type=text/html;filename=test_kyc.html"
```

### 3.2 Actual Response Summary
* **HTTP Status Code**: `500 Internal Server Error` (Caused by Cloudinary SDK attempting remote upload with mock test API key `123456789`).
* **Key Verification Point**:
  The request payload containing an HTML file (`type=text/html`, `filename=test_kyc.html`) passed through all Spring Web parameter parsing and controller/service checks without generating a validation failure (such as HTTP `400 Bad Request` or `415 Unsupported Media Type`). The code attempted to process and store the HTML file directly.
* **Server Response Body**:
  ```json
  {
    "status": 500,
    "timestamp": "2026-09-25T06:50:55.728714200Z",
    "message": "Something went wrong: Unknown API key 123456789",
    "error": "Internal Server Error"
  }
  ```

---

## 4. Security Assessment & Classification

### 4.1 Vulnerability Confirmation
* **Status**: **CONFIRMED VULNERABILITY**
* **Verification Detail**: Verified at code-level and runtime that no content-type, extension, or magic-byte filter exists. Arbitrary file extensions such as `.html` are unconditionally processed and forwarded for storage under `resource_type: auto`.

### 4.2 Security Impact
1. **Stored Cross-Site Scripting (XSS)**: If an attacker uploads an HTML or SVG document containing executable `<script>` payloads, viewing the KYC document link from an administrative portal or direct URL can execute scripts in the admin's session context.
2. **Malware Hosting & Phishing**: The platform can be abused as an arbitrary file host to distribute trojans, malicious documents, or phishing content using the application's domain reputation.
3. **Storage Exhaustion**: Lack of file size validation enables denial-of-service through storage quota exhaustion.

### 4.3 Classification Standards
* **OWASP Top 10 Category**: `A04:2021 – Insecure Design` / `A01:2021 – Broken Access Control`
* **CWE Identifier**: `CWE-434` (Unrestricted Upload of File with Dangerous Type)

### 4.4 Distinction from Other Findings
* **Distinctive Aspect**: `V07` specifically focuses on the file upload and media handling pipeline in `POST /api/v1/seller/request`. It differs fundamentally from authentication flaws (`V01`, `V03`, `V05`), information disclosures (`V02`, `V04`), route protection bypasses (`V06`), and inventory manipulation (`V08`).

---

## 5. Remediation & Retest Guidance

### 5.1 Recommended Fix Direction
1. **Strict File Extension Whitelist**: Reject any upload whose extension is not in a strictly defined whitelist (e.g., `.pdf`, `.png`, `.jpg`, `.jpeg`).
2. **MIME Type Validation**: Validate `document.getContentType()` against permitted MIME types (`application/pdf`, `image/jpeg`, `image/png`).
3. **Magic Byte / Header Verification**: Use Apache Tika or Java's `Files.probeContentType()` to inspect file magic numbers and prevent extension spoofing.
4. **Enforce Size Limits**: Set strict upload size thresholds (e.g. 5MB) using Spring Multipart properties and manual checks.

### 5.2 Exact Retest Procedure
1. Attempt uploading an `.html` file:
   ```bash
   curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://localhost:8080/api/v1/seller/request \
     -H "Authorization: Bearer <user_token>" \
     -F "storeName=Test" \
     -F "document=@test.html;type=text/html;filename=test.html"
   ```
2. **Expected Secure Result**:
   - HTTP Status: `400 Bad Request` or `415 Unsupported Media Type`.
   - Response message: `"Invalid file type. Only PDF and JPEG/PNG images are allowed."`.
