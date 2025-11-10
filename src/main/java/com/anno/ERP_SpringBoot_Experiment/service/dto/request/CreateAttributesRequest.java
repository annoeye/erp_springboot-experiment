package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateAttributesRequest {
    @NotBlank(message = "Không được để trống name.")
    String name;
    @NotNull(message = "Không được để trống giá.")
    double price;
    double salePrice;
    @NotNull(message = "Không được để trống số lượng")
    int stockQuantity;
    @NotNull(message = "Khôn được để trống trạng thái hàng.")
    StockStatus statusProduct;
    List<String> data;
    Set<String> keywords;
    @NotNull(message = "Không được để trống nguồn sản phẩm")
    String productId;
}
