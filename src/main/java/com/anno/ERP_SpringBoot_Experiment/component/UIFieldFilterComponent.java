package com.anno.ERP_SpringBoot_Experiment.component;

import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.graphql.ProductPublicRecord;
import org.springframework.stereotype.Component;

/**
 * Component lọc và chuyển đổi dữ liệu từ DTO nội bộ sang Record công khai.
 *
 * <p>Vai trò:
 * <ul>
 *   <li>Nhận {@link ProductDto} đầy đủ từ Cache RAM (chứa tất cả các trường kể cả nội bộ).</li>
 *   <li>Map sang {@link ProductPublicRecord} chỉ chứa các trường hiển thị công khai
 *       cho giao diện người dùng (Customer UI).</li>
 * </ul>
 *
 * <p>Được inject vào các Controller phục vụ luồng GraphQL / Customer API.
 * Các REST Controller nội bộ (Admin) không cần dùng Component này.
 */
@Component
public class UIFieldFilterComponent {

    /**
     * Chuyển đổi {@link ProductDto} sang {@link ProductPublicRecord}.
     * Các trường nhạy cảm ({@code id}, {@code totalRevenue}, {@code viewCount},...) bị ẩn.
     *
     * @param dto DTO đầy đủ lấy từ cache RAM
     * @return Record công khai dùng để trả về cho Customer UI / GraphQL Service
     */
    public ProductPublicRecord toPublicRecord(ProductDto dto) {
        if (dto == null) return null;

        return new ProductPublicRecord(
                dto.getSkuInfo() != null ? dto.getSkuInfo().getSku() : null,
                dto.getName(),
                dto.getMediaItems(),
                dto.getDiscountPercent(),
                dto.getCategoryName(),
                dto.getStatus() != null ? dto.getStatus().name() : null
        );
    }
}
