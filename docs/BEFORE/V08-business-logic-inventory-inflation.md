# V08: Business Logic Flaw: Arbitrary Product Stock Inflation via Order Cancellation

## 1. Vulnerability Overview
* **ID**: `V08`
* **Title**: Business Logic Flaw: Arbitrary Product Stock Inflation via Order Creation and Cancellation Lifecycle Asymmetry
* **Severity Rationale**: **HIGH**. The application exhibits an asymmetric inventory tracking flaw across its cart ordering and order cancellation workflows. When an authenticated user creates an order from their cart (`POST /api/v1/orders`), the server calculates totals and persists order items, but fails to decrement the product's available stock (`product.setStock(...)`). However, when an order is subsequently cancelled via `PATCH /api/v1/orders/{id}/cancel`, `OrderServiceImpl.cancelOrder()` unconditionally iterates over all order items and increments the product's stock (`product.setStock(product.getStock() + item.getQuantity())`). Because the system restores stock that was never decremented, any authenticated user can repeatedly place cart orders and immediately cancel them, artificially inflating product inventory to arbitrary numbers.
* **Affected Endpoints**: `POST /api/v1/orders` and `PATCH /api/v1/orders/{id}/cancel`
* **HTTP Method**: `POST`, `PATCH`
* **Authentication Requirement**: Authenticated (`ROLE_USER`)

---

## 2. Source-Code Location & Implementation Analysis

### 2.1 Controller Endpoints
In `OrderController.java`:
* **File Link**: [OrderController.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/Order/controller/OrderController.java#L39-L45)
  ```java
  @PostMapping
  public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest req,
                                                   Authentication authentication) {
      String userEmail = authentication.getName();
      Order order = orderService.createOrderFromCart(userEmail, req.getShippingAddress());
      return ResponseEntity.status(HttpStatus.CREATED).body(OrderMapper.toDto(order));
  }
  ```
* **Cancellation Endpoint**: [OrderController.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/Order/controller/OrderController.java#L114-L119)
  ```java
  @PatchMapping("/{id}/cancel")
  public ResponseEntity<MessageResponse> cancelOrder(@PathVariable Long id, Authentication authentication) {
      String userEmail = authentication.getName();
      orderService.cancelOrder(id, userEmail);
      return ResponseEntity.ok(new MessageResponse("Order cancelled."));
  }
  ```

### 2.2 Cart Order Creation Omits Stock Decrement
In `OrderServiceImpl.java`:
* **File Link**: [OrderServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/Order/service/Impl/OrderServiceImpl.java#L47-L90)
* **Code Snippet**:
  ```java
  @Override
  @Transactional
  public Order createOrderFromCart(String userEmail, String shippingAddress) {
      Users user = usersRepo.findByEmail(userEmail)...;
      List<CartItemResponse> cartItems = cartService.getCart(userEmail).getItems();
      ...
      for (CartItemResponse ci : cartItems) {
          Product p = productRepository.findById(ci.getProductId())...;
          BigDecimal subtotal = p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
          total = total.add(subtotal);

          OrderItem item = OrderItem.builder()
                  .order(order)
                  .product(p)
                  .quantity(ci.getQuantity())
                  .priceAtPurchase(p.getPrice())
                  .build();

          order.getItems().add(item);
          // FLAW: product.setStock(...) is NEVER invoked here to deduct stock!
      }

      order.setTotalAmount(total);
      Order saved = orderRepository.save(order);
      cartService.clearCart(userEmail);
      return saved;
  }
  ```

### 2.3 Direct Order Decrements Stock (Asymmetric Logic)
In contrast, [createDirectOrder](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/Order/service/Impl/OrderServiceImpl.java#L114-L123) correctly decrements inventory:
```java
product.setStock(product.getStock() - quantity);
productRepository.save(product);
```

### 2.4 Order Cancellation Unconditionally Restores Stock
In `OrderServiceImpl.java`:
* **File Link**: [OrderServiceImpl.java](file:///c:/Users/CHAMILA/Desktop/SSD%20assignment/ecommerce-platform/backend/src/main/java/com/example/backend/Order/service/Impl/OrderServiceImpl.java#L195-L223)
* **Code Snippet**:
  ```java
  @Override
  @Transactional
  public void cancelOrder(Long orderId, String userEmail) {
      ...
      // restore stock here
      for (OrderItem item : order.getItems()) {
          Product product = item.getProduct();
          product.setStock(product.getStock() + item.getQuantity()); // RESTORES STOCK NEVER DEDUCTED
      }

      order.setStatus(OrderStatus.CANCELLED);
      orderRepository.save(order);
  }
  ```
* **Root Cause Analysis**: State lifecycle mismatch. `createOrderFromCart()` omits stock deduction, while `cancelOrder()` unconditionally restores stock for any cancelled order regardless of origin or deduction status.

---

## 3. Runtime Verification & Test Evidence

### 3.1 Step-by-Step Test Procedure
1. **Initial Stock Verification**:
   Query `GET /api/v1/products/d827840d-3b1b-48c3-b19e-85437741846b` ("Wireless Mouse"):
   - Initial recorded stock = `60`.
2. **Add 2 Units to Cart**:
   Call `POST /api/v1/cart/add` with `quantity=2`.
   - Verified product stock remains `60`.
3. **Place Order from Cart**:
   Call `POST /api/v1/orders` with `shippingAddress="AuditAddress"`:
   - Order ID `3` created with total `$59.98` (2 units).
   - Verified product stock still remains `60` (no deduction occurred).
4. **Cancel Order**:
   Call `PATCH /api/v1/orders/3/cancel`:
   - Server returns `{"message": "Order cancelled."}`.
5. **Post-Cancellation Stock Verification**:
   Query `GET /api/v1/products/d827840d-3b1b-48c3-b19e-85437741846b`:
   - Measured stock = **`62`**!

### 3.2 Actual Response Summary
* **Initial Stock**:
  ```json
  {"id": "d827840d-3b1b-48c3-b19e-85437741846b", "name": "Wireless Mouse", "stock": 60}
  ```
* **Order Creation (`POST /api/v1/orders`)**:
  ```json
  {"id": 3, "totalAmount": 59.98, "status": "CREATED", "items": [{"quantity": 2}]}
  ```
* **Order Cancellation (`PATCH /api/v1/orders/3/cancel`)**:
  ```json
  {"message": "Order cancelled."}
  ```
* **Final Stock (`GET /api/v1/products/{id}`)**:
  ```json
  {"id": "d827840d-3b1b-48c3-b19e-85437741846b", "name": "Wireless Mouse", "stock": 62}
  ```

---

## 4. Security Assessment & Classification

### 4.1 Vulnerability Confirmation
* **Status**: **CONFIRMED VULNERABILITY**
* **Verification Detail**: Successfully executed the end-to-end cart checkout and order cancellation sequence, observing a live increase in product stock from 60 to 62 units.

### 4.2 Security Impact
1. **Inventory Manipulation & Fraud**: Attackers can artificially inflate inventory numbers for out-of-stock items, causing legitimate buyers to place orders for products that do not exist, leading to merchant fulfillment failures, financial disputes, and reputational damage.
2. **Denial of Service / Catalog Integrity Corruption**: Malicious users can systematically corrupt product catalog inventory metrics across the platform.

### 4.3 Classification Standards
* **OWASP Top 10 Category**: `A04:2021 – Insecure Design`
* **CWE Identifier**: `CWE-840: Business Logic Errors` / `CWE-670: Always-Incorrect Control Flow Implementation`

### 4.4 Distinction from Other Findings
* **Distinctive Aspect**: `V08` is a pure business logic flaw situated in the e-commerce transaction state machine (`OrderServiceImpl`). It does not involve authentication headers or URL permissions, but rather corrupts application financial and inventory integrity through unintended interaction between business workflow methods.

---

## 5. Remediation & Retest Guidance

### 5.1 Recommended Fix Direction
1. **Consistent Inventory Lifecycle**:
   - Option A: Decrement stock immediately inside `createOrderFromCart()`, verifying that stock is sufficient before saving order items (`product.setStock(product.getStock() - ci.getQuantity())`).
   - Option B: If stock is intended to be decremented upon payment completion, modify `cancelOrder()` to only restore stock if the order status was `PAID` (or track an explicit `stockDeducted` boolean flag on the order).

### 5.2 Exact Retest Procedure
1. Check product stock (e.g. `X`).
2. Add `N` items to cart and create order via `POST /api/v1/orders`.
3. Cancel the order via `PATCH /api/v1/orders/{id}/cancel`.
4. Check product stock again.
5. **Expected Secure Result**:
   - Stock must remain `X` (or be decremented by `N` and then restored back to `X`, never exceeding initial `X`).
