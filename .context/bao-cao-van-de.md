# Báo Cáo Chi Tiết Các Vấn Đề & Cách Giải Quyết

> Dự án: ERP Spring Boot  
> Ngày: 02/06/2026

---

## Mục Lục

1. [CORS Preflight 403](#1-cors-preflight-403)
2. [CustomUserDetails.getUsername() Sai Field](#2-customuserdetailsgetusername-sai-field)
3. [SecurityUtil.getCurrentUser() Dùng Sai Repository Method](#3-securityutilgetcurrentuser-dùng-sai-repository-method)
4. [LazyInitializationException Trên orderItems](#4-lazyinitializationexception-trên-orderitems)
5. [Service Bỏ Qua Request Parameters (Pagination)](#5-service-bỏ-qua-request-parameters-pagination)
6. [Endpoint Path Không Khớp (404)](#6-endpoint-path-không-khớp-404)
7. [Tổng Kết](#7-tổng-kết)

---

## 1. CORS Preflight 403

### Vấn đề

Request `OPTIONS /api/auth/login` từ frontend (http://localhost:5173) bị trả về **403**.
Browser gửi preflight `OPTIONS` trước khi gọi `POST` thật, nhưng Spring Security chặn lại.

**Log/Error:**
```
HTTP/1.1 403
Origin: http://localhost:5173
→ Không có header Access-Control-Allow-Origin trong response
```

### Nguyên nhân

Security filter chain chỉ match `/api/**` và kiểm tra auth trước khi CORS filter kịp xử lý preflight request. `OPTIONS` request không có token nên bị reject.

### Cách giải quyết

**2 bước:**

**Bước 1 — Cho phép tất cả OPTIONS preflight trong Security chain:**

```java
import org.springframework.web.cors.CorsUtils;

.authorizeHttpRequests(auth ->
    auth.requestMatchers(CorsUtils::isPreFlightRequest).permitAll()  // ← thêm dòng này
        .requestMatchers(SWAGGER_WHITELIST).permitAll()
        ...
```

**Bước 2 — Đăng ký CorsFilter ở tầng Servlet (trước Security):**

```java
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.CorsFilter;

@Bean
public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
    FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(
        new CorsFilter(corsConfigurationSource()));
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return bean;
}
```

`FilterRegistrationBean` với `HIGHEST_PRECEDENCE` đảm bảo `CorsFilter` chạy **trước** Spring Security filter chain.

---

## 2. CustomUserDetails.getUsername() Sai Field

### Vấn đề

`CustomUserDetails.getUsername()` trả về **fullName** (họ tên), nhưng `User.getUsername()` trả về **email**.

**File `User.java`:**
```java
@Override
public String getUsername() {
    return email;  // ✅ email
}
```

**File `CustomUserDetails.java` (trước fix):**
```java
public CustomUserDetails(User user, ...) {
    this.username = user.getName();  // = user.fullName ❌
}
```

### Hậu quả

| Ngữ cảnh | Giá trị `getUsername()` | Đúng ra phải là |
|---|---|---|
| JWT subject | **fullName** ❌ | email |
| `SecurityUtil.getCurrentUsername()` | **fullName** ❌ | email |
| WebSocket `EMAIL` attribute | **fullName** ❌ | email |
| `AuthResponse.username` | **email** ✅ (dùng User.getUsername()) | email |

### Cách giải quyết

```java
// CustomUserDetails.java
this.username = user.getEmail();  // ✅ đồng nhất với User.getUsername()
```

### Ví dụ nhỏ

```
User: fullName = "Nguyễn Văn A", email = "a@example.com"

Trước fix:
  CustomUserDetails.getUsername() = "Nguyễn Văn A" (fullName)
  JWT subject = "Nguyễn Văn A"
  loadUserByUsername("Nguyễn Văn A") → tìm được OR
  loadUserByUsername("a@example.com") → cũng tìm được (vì findByNameOrEmail)
  → Ngẫu nhiên vẫn chạy được nhưng semantic sai

Sau fix:
  CustomUserDetails.getUsername() = "a@example.com" (email)
  JWT subject = "a@example.com"
  loadUserByUsername("a@example.com") → tìm bằng email
  → Nhất quán, đúng semantic
```

---

## 3. SecurityUtil.getCurrentUser() Dùng Sai Repository Method

### Vấn đề

Sau khi fix `CustomUserDetails.getUsername()` trả về email, `SecurityUtil.getCurrentUser()` vẫn gọi `findByName(username)` — method search theo **fullName**.

**File `SecurityUtil.java` (trước fix):**
```java
public Optional<User> getCurrentUser() {
    String username = getCurrentUsername();   // = email
    return userRepository.findByName(username); // search WHERE fullName = email ❌
}
```

### Lỗi gặp phải

```
java.util.NoSuchElementException: No value present
    at OrderService.getMyOrders(OrderService.java:132)
    at securityUtil.getCurrentUser().orElseThrow()
```

### Cách giải quyết

```java
public Optional<User> getCurrentUser() {
    String username = getCurrentUsername();
    if (username == null || "anonymous".equals(username)) {
        return Optional.empty();
    }
    return userRepository.findByNameOrEmail(username); // search fullName OR email ✅
}
```

### Bài học

Khi thay đổi behavior của một class (`CustomUserDetails.getUsername()`), phải kiểm tra **tất cả chỗ phụ thuộc** vào behavior đó. Trong trường hợp này, `SecurityUtil.getCurrentUser()` phụ thuộc ngầm vào giá trị của `getCurrentUsername()`.

---

## 4. LazyInitializationException Trên orderItems

### Vấn đề

`Order.orderItems` được khai báo là `FetchType.LAZY`:

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
List<OrderItem> orderItems = new ArrayList<>();
```

Khi gọi `getMyOrders()`, Hibernate đóng session trước khi `OrderMapper.toDto()` kịp đọc `order.getOrderItems()`.

### Lỗi gặp phải

```
org.hibernate.LazyInitializationException: 
failed to lazily initialize a collection of role: ...Order.orderItems
    at OrderMapperImpl.toDto(OrderItemMapperImpl.java:103)
    at OrderService.getMyOrders(OrderService.java:149)
```

### Nguyên nhân

```java
// OrderService.java — thiếu @Transactional
@Override
public Response<PagingResponse<OrderDto>> getMyOrders(OrderSearchRequest r) {
    var p = orderRepository.findByCustomerId(u.getId(), pageable);
    return p.map(orderMapper::toDto)  // ❌ Session đã đóng, không load được LAZY collection
}
```

### Cách giải quyết

```java
@Override
@Transactional(readOnly = true)  // ← giữ session mở suốt method
public Response<PagingResponse<OrderDto>> getMyOrders(OrderSearchRequest r) {
    var p = orderRepository.findByCustomerId(u.getId(), pageable);
    return p.map(orderMapper::toDto)  // ✅ Session còn, load được orderItems
}
```

### Các giải pháp khác (nếu không muốn dùng @Transactional)

| Giải pháp | Ưu | Nhược |
|---|---|---|
| `@Transactional(readOnly = true)` ✅ | Đơn giản, hiệu quả | Giữ session lâu hơn |
| `JOIN FETCH` trong JPQL | Load sẵn dữ liệu, không cần session | Phải sửa query, join nhiều có thể chậm |
| `spring.jpa.open-in-view=true` | Mặc định Spring Boot, auto hoạt động | Có thể gây N+1, chiếm DB connection lâu |

### Ví dụ nhỏ

```
Luồng chạy:
  1. OrderService.getMyOrders() được gọi
  2. orderRepository.findByCustomerId() → query DB, trả về Proxy Order
  3. Method kết thúc → Spring đóng Hibernate session
  4. orderMapper.toDto(order) → gọi order.getOrderItems()
  5. Hibernate cố gắng lazy load nhưng session đã đóng
  6. → LazyInitializationException 💥

Sau fix (@Transactional):
  1. Bắt đầu transaction (mở session)
  2. Query DB
  3. Mapper đọc orderItems → lazy load được (vì còn session)
  4. Kết thúc transaction (đóng session)
  5. → OK ✅
```

---

## 5. Service Bỏ Qua Request Parameters (Pagination)

### Vấn đề

`getMyOrders()` và `searchOrders()` luôn dùng cứng `PageRequest.of(0, 20)` bất kể client gửi page, size, sort gì.

### Lỗi gặp phải

```java
// OrderService.java (trước fix)
@Override public Response<PagingResponse<OrderDto>> getMyOrders(OrderSearchRequest r) {
    var p = orderRepository.findByCustomerId(u.getId(), PageRequest.of(0, 20)); // ❌ ignore r
    ...
}

@Override public Response<PagingResponse<OrderDto>> searchOrders(OrderSearchRequest r) {
    var p = orderRepository.findAll(PageRequest.of(0, 20)); // ❌ ignore r
    ...
}
```

Client gửi `{"page": 2, "size": 5}` nhưng server vẫn trả page 0, size 20.

### Cách giải quyết

```java
@Override
@Transactional(readOnly = true)
public Response<PagingResponse<OrderDto>> getMyOrders(OrderSearchRequest r) {
    int page = r.getPage() != null ? r.getPage() : 0;
    int size = r.getSize() != null ? r.getSize() : 10;
    Sort sort = Sort.unsorted();
    if (r.getSortBy() != null && !r.getSortBy().isBlank()) {
        var dir = "DESC".equalsIgnoreCase(r.getSortDirection()) 
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        sort = Sort.by(dir, r.getSortBy());
    }
    var pageable = PageRequest.of(page, size, sort);
    
    Page<Order> p;
    if (r.getStartDate() != null && r.getEndDate() != null) {
        p = orderRepository.findByCustomerIdAndCreatedAtBetween(
            u.getId(), r.getStartDate(), r.getEndDate(), pageable);
    } else if (r.getOrderStatus() != null) {
        p = orderRepository.findByCustomerAndStatus(
            u, "%" + r.getOrderStatus().name() + "%", pageable);
    } else {
        p = orderRepository.findByCustomerId(u.getId(), pageable);
    }
    ...
}
```

---

## 6. Endpoint Path Không Khớp (404)

### Vấn đề

Frontend gọi `POST /auth/login` nhưng backend endpoint là `POST /api/auth/login`.

### Lỗi gặp phải

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "No static resource auth/login."
}
```

### Nguyên nhân

Controller có `@RequestMapping("/api/auth")` nên path đầy đủ là `/api/auth/login`.

```java
@RequestMapping("/api/auth")  // ← prefix /api
public interface AuthController {
    @PostMapping("/login")     // ← path /login
    Response<AuthResponse> login(...);
    // → Endpoint thực tế: POST /api/auth/login
}
```

### Cách giải quyết

Sửa frontend gọi đúng path: `POST /api/auth/login`

### Ví dụ nhỏ

```
Frontend gọi:  POST http://localhost:8080/auth/login      → 404
Đúng phải gọi: POST http://localhost:8080/api/auth/login  → 200
```

---

## 7. Tổng Kết

### Checklist kiểm tra khi gặp lỗi

| STT | Kiểm tra | Vấn đề thường gặp |
|---|---|---|
| 1 | CORS preflight có bị chặn? | Thêm `CorsUtils::isPreFlightRequest.permitAll()` + CorsFilter Servlet |
| 2 | `getUsername()` có nhất quán? | User vs CustomUserDetails trả về cùng field |
| 3 | `findByName` hay `findByNameOrEmail`? | Dùng đúng method theo field đang search |
| 4 | Có `@Transactional` khi đọc LAZY collection? | Thiếu → LazyInitializationException |
| 5 | Request params có được dùng không? | Service không được ignore page/size/sort |
| 6 | Path frontend có đúng không? | Kiểm tra @RequestMapping prefix |

### Nguyên tắc chung

1. **Nhất quán**: Cùng một field (`username`) phải trả về cùng một giá trị ở mọi lớp
2. **Kiểm tra dependency**: Thay đổi 1 chỗ → kiểm tra tất cả chỗ phụ thuộc
3. **Session management**: Entity có LAZY collection → cần `@Transactional` khi đọc
4. **Đừng ignore request params**: Request DTO có field thì phải dùng, không hardcode
5. **Log request/response**: Khi debug, log cả path và params để phát hiện sai sớm
