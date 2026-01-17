package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.common.annotation.NormalizedId;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO để cập nhật thông tin Product.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProductRequest {

    /**
     * ID của Product cần cập nhật (bắt buộc).
     * Được normalize tự động: uppercase + remove dashes.
     */
    @NormalizedId
    String id;

    /**
     * Tên mới của sản phẩm (optional).
     */
    String name;

    /**
     * SKU của Category mới (optional).
     */
    String categorySku;

    /**
     * Trạng thái active của sản phẩm (optional).
     */
    ActiveStatus status;
}
