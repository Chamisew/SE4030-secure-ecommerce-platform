# V08: Business Logic Flaw in Inventory Management (AFTER FIX)

## 1. Vulnerability & Fix Overview

* **Vulnerability ID**: `V08`
* **Vulnerability Title**: Business Logic Inventory Management Flaw (Stock Omission on Order Creation & Inflation on Cancellation)
* **Assigned Member**: Member 4 — File Upload & Order Flow Specialist (`Sanjana Dinithi <it22082510@my.sliit.lk>`)
* **Branch**: `fix/D-fileupload-order-vulns`
* **CWE**: CWE-840 (Business Logic Errors), CWE-799 (Improper Control of Generation of Code or Resource)
* **OWASP Top 10**: A04:2021 – Insecure Design
* **Fix Status**: **VERIFIED & REMEDIATED**

---

## 2. Root Cause & Code Remediation

### 2.1 The Flaw (BEFORE)
In the original implementation:
1. `createDirectOrder()` decremented stock, but `createOrderFromCart()` completely omitted stock availability verification and stock deduction when an order was placed from a user's shopping cart.
2. In contrast, `cancelOrder()` restored stock for all items in the order (`product.setStock(product.getStock() + item.getQuantity())`).
3. This asymmetry enabled an attacker to repeatedly add items to their cart, place an order without stock decrementing, and immediately cancel the order to arbitrarily inflate warehouse inventory in the database, breaking inventory integrity and allowing phantom stock sales.

### 2.2 The Remediation (AFTER)
In [OrderServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/Order/service/Impl/OrderServiceImpl.java#L66-L82):
1. **Stock Validation on Cart Order**:
   Before deducting, the system checks whether sufficient stock exists for each product in the cart. If insufficient, a `ProductOutOfStockException` is thrown:
   ```java
   if (p.getStock() < ci.getQuantity()) {
       throw new ProductOutOfStockException("Insufficient stock for product: " + p.getName());
   }
   ```
2. **Immediate Stock Deduction**:
   When the order is placed, stock is atomically decremented and persisted:
   ```java
   p.setStock(p.getStock() - ci.getQuantity());
   productRepository.save(p);
   ```
3. **Idempotent Cancellation**:
   In [OrderServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/Order/service/Impl/OrderServiceImpl.java#L210-L230), the cancellation logic verifies that already-cancelled orders cannot restore stock a second time:
   ```java
   if (order.getStatus() == OrderStatus.CANCELLED) {
       return;
   }
   for (OrderItem item : order.getItems()) {
       Product product = item.getProduct();
       product.setStock(product.getStock() + item.getQuantity());
       productRepository.save(product);
   }
   order.setStatus(OrderStatus.CANCELLED);
   orderRepository.save(order);
   ```

---

## 3. Re-Testing & Verification Evidence

### 3.1 Test Execution
A full order lifecycle test was performed against the patched endpoints:
1. **Initial Stock Audit**: Queried `Wireless Mouse` -> Stock = 62.
2. **Order Placement**: Added 2 units to cart and placed order (`POST /api/v1/orders`).
3. **Post-Order Stock Audit**: Queried `Wireless Mouse` -> Stock = **60** (deducted by 2).
4. **Order Cancellation**: Sent `PATCH /api/v1/orders/{id}/cancel` -> Status returned 200.
5. **Post-Cancel Stock Audit**: Queried `Wireless Mouse` -> Stock = **62** (restored to baseline).
6. **Duplicate Cancellation Attack**: Sent repeated `PATCH /api/v1/orders/{id}/cancel` -> Handled idempotently; Stock remained **62** (no inflation).

---

## 4. Evidence File Link

Detailed execution log with step-by-step requests and verified stock states:
* [evidence-log.txt](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/docs/AFTER/V08-business-logic-inventory-inflation/evidence-log.txt)
