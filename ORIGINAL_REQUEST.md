# Original User Request

## Initial Request — 2026-07-14T14:38:01+07:00

Hoàn thiện chức năng Giỏ hàng (Shopping Cart) trong backend Spring Boot của hệ thống ERP bằng cách bổ sung các phương thức còn thiếu ở tầng Service, tạo REST Controller đầy đủ, và tích hợp bảo mật JWT.

Working directory: /home/ddicgegd/Projects/erp_springboot-experiment

Integrity mode: development

---

## Context

Dự án đang sử dụng Spring Boot 3, Spring Security 6, Spring Data JPA với Oracle DB.

Tầng Service (`ShoppingCartService.java`) đã có `add()` và `remove()`, nhưng **chưa có**:
- `getCart()` — lấy giỏ hàng của người dùng hiện tại
- `clearCart()` — xóa toàn bộ sản phẩm trong giỏ

REST Controller cho giỏ hàng **chưa tồn tại**. Toàn bộ interface, service, mapper, dto, entity đã có sẵn và cần được tận dụng.

Cấu trúc hiện có để tham khảo:
- Interface: `iShoppingCart` — `add(List<CartItemRequest>)`, `remove(List<String> skus)`
- Service: `ShoppingCartService` implements `iShoppingCart`
- DTO: `ShoppingCartDto`, `CartItemRequest`
- Entity: `ShoppingCart`, `CartItem`
- Mapper: `ShoppingCartMapper`
- Repository: `ShoppingCartRepository`
- Chuẩn Response: `Response<T>` (xem các controller mẫu: `AuthController`, `MerchandiseController`)
- Chuẩn Controller: interface + impl tách biệt (xem `AuthController.java` + `authControllerImpl.java`)

---

## Requirements

### R1. Bổ sung phương thức còn thiếu trong Service Layer
Thêm `getCart()` và `clearCart()` vào `iShoppingCart` và triển khai chúng trong `ShoppingCartService`:
- `getCart()`: Trả về `Response<ShoppingCartDto>` giỏ hàng của người dùng đang đăng nhập. Nếu chưa có giỏ hàng thì tạo mới (giỏ rỗng).
- `clearCart()`: Xóa toàn bộ items trong giỏ, reset tổng tiền về 0, lưu lại, và trả về `Response<ShoppingCartDto>` của giỏ đã được làm trống.

### R2. Tạo REST Controller đầy đủ cho Giỏ hàng
Tạo interface `ShoppingCartController` và class `ShoppingCartControllerImpl` theo đúng cấu trúc của project, expose đầy đủ 4 endpoint:
- `GET /api/cart` — Lấy giỏ hàng
- `POST /api/cart/add` — Thêm/cập nhật sản phẩm (body: `List<CartItemRequest>`)
- `DELETE /api/cart/remove` — Xóa theo danh sách SKU (body: `List<String>`)
- `DELETE /api/cart/clear` — Làm trống toàn bộ giỏ hàng

### R3. Cấu hình bảo mật
Đảm bảo tất cả các endpoint `/api/cart/**` yêu cầu người dùng đã đăng nhập (authenticated). Cập nhật `SecurityConfiguration.java` nếu cần.

---

## Acceptance Criteria

### API hoạt động đúng
- [ ] `GET /api/cart` trả về HTTP 200 với payload `ShoppingCartDto` hợp lệ cho user đã xác thực.
- [ ] `POST /api/cart/add` với body `[{"sku": "SKU001", "quantity": 2}]` trả về HTTP 200 với giỏ hàng đã được cập nhật.
- [ ] `DELETE /api/cart/remove` với body `["SKU001"]` trả về HTTP 200 với giỏ hàng đã bỏ SKU đó.
- [ ] `DELETE /api/cart/clear` trả về HTTP 200 với giỏ hàng rỗng (items = [], total = 0).
- [ ] Tất cả 4 endpoint trả về HTTP 401 khi không có JWT token hợp lệ.

### Chuẩn codebase
- [ ] Controller tuân theo cấu trúc interface + impl đã có trong project.
- [ ] Response đóng gói trong `Response<ShoppingCartDto>` chuẩn.
- [ ] Build `./mvnw clean compile` hoàn thành không có lỗi.

## Follow-up — 2026-07-15T00:31:36+07:00

Kiểm thử rằng việc gỡ Elasticsearch khỏi `ProductService` thành công và tính năng tìm kiếm sản phẩm vẫn hoạt động đúng thông qua JPA Specification. Server đang chạy tại `localhost:8080`.

Working directory: /home/ddicgegd/Projects/erp_springboot-experiment

Integrity mode: development

---

## Requirements

### R1. API tìm kiếm sản phẩm hoạt động không có Elasticsearch

Endpoint `POST /api/merchandise/search-Product` phải trả về kết quả đúng với tất cả các filter sau (mỗi filter test riêng và kết hợp):
- `keyword` — tìm theo tên sản phẩm (LIKE, không phân biệt hoa thường)
- `statuses` — lọc theo danh sách trạng thái (`ACTIVE`, `LOCKED`)
- `categoryId` — lọc theo category (nếu có dữ liệu)
- `minSoldQuantity` / `maxSoldQuantity`
- `minRevenue`
- `createdFrom` / `createdTo`
- Không có filter nào (trả tất cả sản phẩm)
- Phân trang đúng (`page`, `size`, `totalElements` hợp lý)

### R2. Không có lỗi liên quan đến Elasticsearch khi chạy API

Khi gọi `POST /api/merchandise/search-Product`, không được xuất hiện bất kỳ lỗi nào liên quan đến:
- `DataAccessResourceFailureException`
- `no_shard_available_action_exception`
- Kết nối đến `localhost:9200`

### R3. Không có bean Elasticsearch nào được Spring load

Ứng dụng đang chạy không được có các bean `ElasticsearchTemplate`, `ElasticsearchClient`, hoặc `ProductElasticSearchService` trong ApplicationContext.

---

## Acceptance Criteria

### Kiểm thử API

- [ ] `POST /api/merchandise/search-Product` với body `{}` (không filter) trả về HTTP 200 và `content` là list (có thể rỗng)
- [ ] `POST /api/merchandise/search-Product` với `{"keyword": "abc"}` trả về HTTP 200, kết quả chỉ chứa sản phẩm có tên chứa "abc"
- [ ] `POST /api/merchandise/search-Product` với `{"statuses": ["ACTIVE"]}` trả về HTTP 200, tất cả kết quả có `status: "ACTIVE"`
- [ ] `POST /api/merchandise/search-Product` với `{"paging": {"page": 0, "size": 5}}` trả về HTTP 200 và `size <= 5`
- [ ] `POST /api/merchandise/search-Product` với `{"paging": {"page": 0, "size": 5}}` có `totalElements >= 0` (không bị lỗi `Page index must not be less than zero`)
- [ ] Không có HTTP 503 hay stack trace `elasticsearch` nào trong response

### Kiểm thử không có Elasticsearch

- [ ] Thực hiện `curl -s http://localhost:9200` — nếu ES không chạy thì API vẫn hoạt động bình thường (không crash)
- [ ] Log ứng dụng không chứa chuỗi `elasticsearch` hay `9200` khi gọi search API
