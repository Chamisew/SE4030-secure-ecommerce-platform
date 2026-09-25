# Test Account Identifiers

This document lists test account identifiers and assigned roles created during the functional baseline testing phase.

> **Note**: Plaintext passwords and JWT tokens are deliberately excluded from this file in compliance with security guidelines.

| Account Identifier (Email) | Role | Primary Usage Scope |
| :--- | :--- | :--- |
| `func_user@example.com` | `ROLE_USER` | Standard User Flow (Auth, Cart, Wishlist, Orders, Password Reset) |
| `admin_test@example.com` | `ROLE_ADMIN` | Admin Workflow (Category Management, Admin Approvals) |
| `seller_direct@example.com` | `ROLE_SELLER` | Seller Workflow (Product Management, Catalog Operations) |
| `seller_candidate@example.com` | `ROLE_USER` | Seller Onboarding Candidate |
