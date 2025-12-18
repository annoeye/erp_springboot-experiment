
package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateAttributesRequest {

    @JsonProperty("sku")
    String sku; // SKU của attributes cần update

    @JsonProperty("name")
    String name; // Tên mới (optional)

    @JsonProperty("price")
    Double price; // Giá mới (optional)

    @JsonProperty("sale_price")
    Double salePrice; // Giá khuyến mãi mới (optional)

    @NotBlank(message = "Không được để trống mới.")
    String color; // Mới mới (optional)

    @NotBlank(message = "Không được để trống option.")
    String option; // Option mới (optional)

    @JsonProperty("stock_quantity")
    Integer stockQuantity; // Số lượng tồn kho mới (optional)

    @JsonProperty("keywords")
    List<String> keywords; // Keywords mới (optional)

    @JsonProperty("data")
    List<String> data; // Dữ liệu specifications mới (optional)
}
