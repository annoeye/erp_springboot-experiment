# Báo cáo Hiệu năng API — ERP SpringBoot Experiment

**Ngày thực hiện:** 2026-06-25  
**Người thực hiện:** annoeye  
**Môi trường:** localhost · Spring Boot 3 · Oracle XE · Caffeine Cache  
**Dataset:** 600 Products · 55,590 Attributes · 15 Categories  

---

## 1. Mục tiêu

Đánh giá hiệu năng của hệ thống hiện tại trước khi tích hợp Elasticsearch. Tập trung vào ba vấn đề cốt lõi:

1. Caffeine Cache có đang hoạt động hiệu quả không?
2. Các trường được đánh index có cho thấy sự khác biệt rõ ràng không?
3. Giới hạn chịu tải thực tế của hệ thống ở mức bao nhiêu concurrent users?

---

## 2. Kết quả đo lường

### 2.1 Cache Behavior

| Lần gọi | Thời gian | SQL xuống DB |
| :--- | :---: | :--- |
| Cold (lần 1) | ~30ms | SELECT id + SELECT WHERE id IN(...) + COUNT(*) |
| Warm (lần 2) | ~12ms | SELECT id + COUNT(*) |
| Fully cached (lần 3+) | ~12ms | Chỉ COUNT(*) |

Khi dữ liệu tăng từ 1K lên 55K bản ghi, Warm Cache vẫn giữ nguyên 12ms. Điều này xác nhận kiến trúc Deferred Join (lấy ID từ DB, lấy chi tiết từ RAM) đang hoạt động đúng thiết kế.

Tuy nhiên, `SELECT COUNT(*)` vẫn chạy mỗi request và không được cache. Đây là điểm ổn định cần xử lý.

### 2.2 So sánh Indexed vs Non-indexed Fields

| Loại truy vấn | Trường | 1K rows | 55K rows | Tỉ lệ tăng |
| :--- | :--- | :---: | :---: | :---: |
| productId (có index) | product\_id | 25ms | 25ms | 0% |
| SKU (có index) | sku\_name | 27ms | 27ms | 0% |
| Keyword LIKE (không index) | name | 19ms | 60ms | +216% |
| Price range (không index) | sale\_price | 44ms | 72ms | +64% |
| Status filter (không index) | status\_product | 28ms | 31ms | +11% |

Các trường có index giữ nguyên thời gian phản hồi khi data tăng 55 lần — đúng với đặc tính O(log N) của B-Tree Index. Ngược lại, Keyword LIKE tăng hơn 3 lần và sẽ tiếp tục tăng tuyến tính.

*Lưu ý:* Status filter ít bị ảnh hưởng vì Oracle tự dùng Bitmap scan trên column có cardinality thấp (4 giá trị enum).

### 2.3 Concurrent Users

**Endpoint có cache (Get All):**

| Concurrent Users | Avg (ms) | P95 (ms) | Lỗi |
| :---: | :---: | :---: | :---: |
| 50 | 84.8 | 104.5 | 0 |
| 100 | 100.7 | 126.1 | 0 |
| 200 | 192.5 | 265.9 | 0 |
| 500 | 521.9 | 712.3 | 0 |

**Endpoint không có cache (Keyword Search — LIKE '%...%'):**

| Concurrent Users | Avg (ms) | P95 (ms) | Lỗi |
| :---: | :---: | :---: | :---: |
| 50 | 611.6 | 1,006.6 | 0 |
| 100 | 980.7 | 1,800.9 | 0 |
| 200 | 1,947.1 | 3,546.6 | 0 |
| 500 | 5,000.2 | 9,248.6 | 0 |

Cached endpoint chịu tải tốt đến 500 users mà không có lỗi. Keyword search bắt đầu vượt ngưỡng chấp nhận được từ 50 users (>600ms avg), và ở 500 users P95 đạt 9.2 giây — gần sát ngưỡng timeout.

### 2.4 Ramp-up Analysis — Tìm Breaking Point

```
  10 users  →  Avg: 13.5ms   P99: 16.8ms   Wall: 23ms
  50 users  →  Avg: 76.3ms   P99: 100.3ms  Wall: 129ms   [!] Tăng 5.7x
 100 users  →  Avg: 98.8ms   P99: 142.3ms  Wall: 193ms
 200 users  →  Avg: 215.4ms  P99: 318.2ms  Wall: 406ms   [!] Tăng 2.2x
 300 users  →  Avg: 277.0ms  P99: 435.5ms  Wall: 606ms
 500 users  →  Avg: 483.6ms  P99: 792.8ms  Wall: 1171ms
```

Có hai điểm phi tuyến rõ ràng:

- **10 → 50 users (+5.7x):** HikariCP connection pool mặc định là 10 connection. Khi 50 users cùng request, 40 user phải chờ connection → latency tăng đột ngột.
- **100 → 200 users (+2.2x):** Tomcat thread pool (mặc định 200 threads) bắt đầu saturate, đồng thời `COUNT(*)` chạy song song trên Oracle gây tranh giành resource.

### 2.5 Pagination Stress

| Thử nghiệm | Kết quả |
| :--- | :--- |
| Phân trang tuần tự trang 1–10 | 12–90ms (biến động mạnh do COUNT không cache) |
| Deep page (trang 100–2000) | 66–96ms — ổn định, Oracle xử lý offset tốt |
| 50 users concurrent phân trang | Avg 188ms, P95 335ms |

### 2.6 Page Size Impact

| Page size | Thời gian | Response body |
| :---: | :---: | :---: |
| 10 | 16ms | 5 KB |
| 50 | 22ms | 25 KB |
| 100 | 23ms | 52 KB |
| 500 | 286ms | 260 KB |
| 1000 | 1,308ms | 509 KB |

Từ 100 lên 500 rows, thời gian tăng 12 lần. Overhead nằm ở cả phía DB (đọc nhiều block hơn) lẫn phía Spring (serialize JSON 260KB).

---

## 3. Vấn đề xác định

| Vấn đề | Mức độ | Breaking point |
| :--- | :---: | :---: |
| `SELECT COUNT(*)` không được cache — chạy mỗi request | Cao | Mọi paginated endpoint |
| Keyword `LIKE '%...%'` không dùng được index | Cao | 50 concurrent users |
| HikariCP pool size = 10 (default) quá nhỏ | Trung bình | 10→50 users |
| Page size không bị giới hạn — size=1000 cho 1.3 giây | Trung bình | size > 100 |
| Tomcat thread pool 200 | Thấp | >200 users |

---

## 4. Khuyến nghị

### Phase 1 — Sửa ngay (1-2 ngày)

**4.1 Tăng HikariCP pool size**

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
```

Dự kiến: Xóa degradation x5.7 tại 50 users. Không cần thay đổi code.

**4.2 Cache COUNT trong Caffeine**

Hiện tại `searchProductIds()` đã được cache nhưng `COUNT(*)` thì không. Cần cache riêng `totalElements` với TTL 10 phút, keyed theo filter params.

```java
@Cacheable(value = "attributeCount", key = "#request.cacheKey()")
public long countAttributes(AttributeSearchRequest request) {
    return attributeRepository.count(spec);
}
```

Dự kiến: Xóa biến động 12–90ms trên pagination và giảm tải COUNT concurrent.

**4.3 Giới hạn page size**

```java
public class PagingRequest {
    @Min(1) @Max(100)
    private int size = 20;
}
```

Dự kiến: Ngăn client request 1300ms vô tình.

### Phase 2 — Tối ưu DB (1 tuần)

**4.4 Oracle Full-Text Index cho keyword search**

```sql
CREATE INDEX ft_attr_name ON attributes(name)
    INDEXTYPE IS CTXSYS.CONTEXT;
```

Thay thế `LIKE '%keyword%'` bằng `CONTAINS(name, 'keyword') > 0`. Dự kiến: keyword search từ 60ms → ~10ms, giải quyết breaking point 50 users.

**4.5 Composite index cho price + status**

```sql
CREATE INDEX idx_attr_status_price
    ON attributes(status_product, sale_price);
```

### Phase 3 — Scale dài hạn (2-4 tuần)

**4.6 Tích hợp Elasticsearch**

Dự án đã có sẵn Outbox Pattern và Kafka — đây là nền tảng lý tưởng để sync Oracle → ES.

Phân chia trách nhiệm sau khi tích hợp:
- **Oracle:** Xử lý toàn bộ write transaction (ACID).
- **Elasticsearch:** Xử lý toàn bộ search (keyword, price range, status filter, full-text).
- **Caffeine:** Giữ nguyên cho product detail cache.

Dự kiến: 500 concurrent keyword search < 50ms avg.

---

## 5. Tóm tắt

Hệ thống hiện tại có nền tảng kiến trúc tốt — Caffeine Cache hoạt động đúng, index DB chuẩn xác với pattern truy vấn thực tế. Vấn đề chính không phải do thiết kế sai mà do chưa hoàn thiện ở tầng infrastructure (pool size, COUNT cache) và chưa có giải pháp search chuyên biệt cho non-indexed queries.

Ba fix ở Phase 1 có thể thực hiện trong 1-2 ngày mà không cần refactor, dự kiến giải quyết được ~80% bottleneck hiện tại.

---

*Kết thúc báo cáo.*
