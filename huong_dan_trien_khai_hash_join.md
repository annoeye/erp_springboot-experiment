# Hướng Dẫn Tự Triển Khai: Custom Hash & Merge Join Engine

Tài liệu này hướng dẫn anh cách tự viết mã nguồn cho cơ chế tìm kiếm 4 bước: Lọc Index thô từ DB -> Đẩy RAM Cache -> Lọc cuốn chiếu trên RAM & Tự động xóa bản ghi lỗi -> Trả về kết quả phân trang (20/30/n dòng).

---

## BƯỚC 1: TRUY VẤN CỤM ID ỨNG VIÊN TỪ DB (ZONING 1)

Viết một hàm trong Repository của anh để truy vấn nhanh danh sách ID của các bản ghi thỏa mãn điều kiện index (như `productId`, `status`).

### Mã nguồn tham khảo ở Repository:
```java
public interface AttributesRepository extends JpaRepository<Attributes, Long> {

    // Chỉ SELECT duy nhất cột ID thô dựa trên các trường có index để SQL chạy siêu nhanh
    @Query("SELECT a.id FROM Attributes a " +
           "WHERE (:productId IS NULL OR a.product.id = :productId) " +
           "AND (:status IS NULL OR a.statusProduct = :status)")
    List<Long> findCandidateIdsByIndices(
        @Param("productId") Long productId,
        @Param("status") StockStatus status
    );
}
```

---

## BƯỚC 2 & 3: ĐẨY DỮ LIỆU VÀO RAM VÀ THỰC HIỆN LỌC CUỐN CHIẾU & XÓA BẢN GHI LỖI (ZONING 2)

Tại Service xử lý tìm kiếm, anh thực hiện quy trình:
1. Nhận danh sách ID ứng viên từ Bước 1 (~3000 IDs).
2. Lấy thông tin chi tiết của các ID này từ Caffeine L1 Cache (Nếu cache miss thì tự động query DB nạp lên RAM).
3. Sử dụng vòng lặp duyệt qua danh sách để lọc các điều kiện phức tạp (`>`, `<`, `like`). 
4. **Xóa bỏ (evict/remove)** các phần tử sai trực tiếp trên RAM để dọn dẹp tài nguyên.
5. Dừng lại khi thu thập đủ `pageSize` (20/30/n dòng).

### Mã nguồn tham khảo ở Service:
```java
@Service
@RequiredArgsConstructor
public class CustomSearchService {

    private final AttributesRepository attributesRepository;
    private final CacheManager cacheManager; // Caffeine cache manager
    private final AttributesMapper attributesMapper;

    public List<AttributesDto> searchAttributesCustom(
            Long productId,
            StockStatus status,
            Double minPrice,
            String keyword,
            int pageSize // kích thước yêu cầu (ví dụ: 20 hoặc 30)
    ) {
        // 1. Bước 1: Lấy cụm ID ứng viên từ DB (~3000 IDs)
        List<Long> candidateIds = attributesRepository.findCandidateIdsByIndices(productId, status);

        // Tạo một danh sách động để thao tác xóa bớt trên RAM
        List<Long> mutableCandidates = new ArrayList<>(candidateIds);
        List<AttributesDto> resultList = new ArrayList<>();

        // 2. Bước 2: Nạp dữ liệu chi tiết của các ID này lên RAM Cache bằng CacheUtils
        Map<Long, AttributesDto> detailMap = CacheUtils.getAll(
                cacheManager,
                "attributes", // Tên cache Caffeine của anh
                mutableCandidates,
                missingIds -> attributesRepository.findAllById(missingIds).stream()
                        .collect(Collectors.toMap(Attributes::getId, attributesMapper::toDto))
        );

        // 3. Bước 3: Vòng lặp lọc Merge Handle & Pruning (Xóa bớt trên RAM)
        Iterator<Long> iterator = mutableCandidates.iterator();
        while (iterator.hasNext()) {
            Long id = iterator.next();
            AttributesDto dto = detailMap.get(id);

            if (dto == null) {
                iterator.remove(); // Xóa khỏi danh sách RAM nếu không tìm thấy dữ liệu
                continue;
            }

            // --- Lọc các điều kiện Vùng 2 (Không đánh index) ---
            boolean isMatched = true;

            // Điều kiện lọc giá (Ví dụ: giá phải lớn hơn minPrice)
            if (minPrice != null && dto.getPrice() < minPrice) {
                isMatched = false;
            }

            // Điều kiện lọc chuỗi LIKE (Ví dụ: tên phải chứa từ khóa keyword)
            if (keyword != null && !keyword.isBlank()) {
                String name = dto.getName() != null ? dto.getName().toLowerCase() : "";
                if (!name.contains(keyword.toLowerCase().trim())) {
                    isMatched = false;
                }
            }

            // --- Xử lý kết quả ---
            if (isMatched) {
                resultList.add(dto);
                // Nếu đã gom đủ số lượng yêu cầu (20/30/n), ta dừng luôn vòng lặp
                if (resultList.size() >= pageSize) {
                    break;
                }
            } else {
                // Xóa bớt phần tử sai trực tiếp khỏi danh sách RAM đang xử lý
                iterator.remove();
            }
        }

        // Trả ra đúng kích thước size nhập vào (20/30/n)
        return resultList;
    }
}
```

---

## CÁC LƯU Ý KHI ANH TỰ VIẾT CODE:
1. **Thread-Safe:** Nếu danh sách `mutableCandidates` được thao tác bởi nhiều luồng cùng lúc, hãy đảm bảo cơ chế đồng bộ hoặc sử dụng bản copy cục bộ cho mỗi request (đoạn code trên dùng biến cục bộ nên an toàn).
2. **Caffeine Cache:** Tận dụng `CacheUtils.getAll` giúp anh tự động hóa hoàn toàn việc: "Nếu đã có trên RAM thì lấy dùng luôn, nếu chưa có thì chọc xuống DB lấy lên rồi tự nhét vào RAM".
