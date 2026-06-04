# Bản Thiết Kế Cốt Lõi: Mô hình BFF Aggregator Pattern (Phiên bản Tiêu chuẩn)

Tài liệu này là quy chuẩn (Blueprint) bắt buộc phải tuân thủ khi xây dựng bất kỳ API nào đóng vai trò **BFF (Backend for Frontend) / Aggregator** trong hệ thống. Mô hình này sử dụng **Java 21 (Virtual Threads)** và **Resilience4j Circuit Breaker**.

Mục tiêu tối thượng: Dọn dẹp code rác, cấm tuyệt đối việc tạo nhiều API lắt nhắt (như `getA`, `getB_for_A`) và thay bằng MỘT API Aggregator duy nhất để gộp Data Tĩnh và Động.

---

## 1. Nguyên tắc Thiết kế & Nghiệp vụ (Core Business Logic)

1.  **Dùng 1 Endpoint hợp nhất:** Không tạo ra nhiều hàm dư thừa như `getMyData`, `searchData`. Chỉ dùng 1 hàm duy nhất để hứng request, sau đó BFF tự động bóc tách và phân quyền.
2.  **Phân tách Tĩnh - Động (Static vs Dynamic):**
    *   **Tĩnh:** Dữ liệu ít đổi (Thông tin người dùng, Tên Sản phẩm, Hình ảnh). Bắt buộc phải đẩy lên RAM Cache (Caffeine).
    *   **Động:** Dữ liệu nhảy liên tục (Trạng thái, Tồn kho, Số dư). Bắt buộc phải móc trực tiếp từ DB hoặc API nội bộ tại thời điểm gọi.
3.  **Trộn Dữ Liệu bằng Virtual Threads:** Đẩy lệnh gọi Data Tĩnh và Động vào các Luồng Ảo song song. Trộn (Combine) chúng lại trong chưa tới 10ms.
4.  **Tải Lười (Lazy Loading):** API List không được trả về data rác khổng lồ. Chỉ những data nặng đô mới được gộp vào khi có cờ báo hiệu (ví dụ `?details=true`).
5.  **Cầu Dao Điện (Circuit Breaker):** Nếu Service cung cấp Data Động bị sập, hệ thống trả về Data Default (Fallback), tuyệt đối không văng lỗi 500 ra ngoài Frontend.

---

## 2. Tiêu chuẩn Triển khai Kỹ thuật

**Bước 1: Nền tảng (Platform)**
Bật Virtual Threads trong `application.yml` (`spring.threads.virtual.enabled: true`). 
Thiết lập Timeout (Connect, Read, Write) ở mức HTTP Client (WebClient/RestClient).

**Bước 2: Client Connectors (Tầng gọi Data Động)**
Mọi cuộc gọi ra service ngoài (hoặc query DB phức tạp dễ nghẽn) phải được bọc bởi `@CircuitBreaker(name = "...", fallbackMethod = "...")`.

**Bước 3: Tầng Aggregator (BFF Layer - Gộp luồng)**
Sử dụng chuẩn sau để gộp dữ liệu mà không gây Block OS Thread:
```java
// 1. Khởi tạo
private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

// 2. Forking: Chạy song song
var staticDataFuture = CompletableFuture.supplyAsync(() -> cache.get(...), virtualExecutor);
var dynamicDataFuture = CompletableFuture.supplyAsync(() -> database.get(...), virtualExecutor);

// 3. Joining: Trộn dữ liệu tĩnh và động
return staticDataFuture.thenCombine(dynamicDataFuture, (st, dy) -> {
    return new AggregatedResponse(st, dy);
}).join();
```
