package com.anno.ERP_SpringBoot_Experiment.mapper;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.repository.CategoryRepository;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductDto;
import jakarta.persistence.EntityNotFoundException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Mapper này kế thừa EntityMapper chung và xử lý các logic ánh xạ phức tạp
 * cho Product và ProductDto.
 *
 * Nó được định nghĩa là 'abstract class' thay vì 'interface' để cho phép
 * tiêm (inject) CategoryRepository vào cho mục đích tra cứu.
 */
@Mapper(config = DefaultConfigMapper.class)
public abstract class ProductMapper implements EntityMapper<ProductDto, Product> {

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Chuyển từ Entity (Product) sang DTO (ProductDto).
     */
    @Mappings({
            // Ánh xạ từ đối tượng 'sku' (của Product) vào các trường DTO
            @Mapping(source = "skuInfo.SKU", target = "sku"),
//            @Mapping(source = "skuInfo.name", target = "name"), // Giả định DTO và SkuInfo đều có trường 'name'

            // Lấy 'sku' từ đối tượng Category
            @Mapping(source = "category.skuInfo.SKU", target = "skuCategory")
    })
    @Override
    public abstract ProductDto toDto(Product entity);

    /**
     * Chuyển từ DTO (ProductDto) sang Entity (Product).
     */
    @Mappings({
            // Ánh xạ các trường DTO vào đối tượng 'sku' (của Product)
            @Mapping(source = "sku", target = "skuInfo.SKU"),
//            @Mapping(source = "name", target = "skuInfo.name"), // Giả định DTO và SkuInfo đều có trường 'name'

            // Sử dụng một phương thức tùy chỉnh (@Named) để tra cứu Category từ skuCategory
            @Mapping(source = "skuCategory", target = "category", qualifiedByName = "skuToCategory"),

            // Bỏ qua các trường mà ta không muốn DTO ghi đè
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "auditInfo", ignore = true), // Sẽ được xử lý bởi AuditEntityListener
    })
    @Override
    public abstract Product toEntity(ProductDto dto);

    /**
     * Phương thức tùy chỉnh (qualified) để MapStruct sử dụng.
     * Nó nhận một String 'sku' và trả về một đối tượng Category đầy đủ.
     *
     * @param sku Chuỗi SKU của Category.
     * @return Đối tượng Category được tìm thấy.
     * @throws EntityNotFoundException nếu không tìm thấy Category.
     */
    @Named("skuToCategory")
    protected Category skuToCategory(String sku) {
        if (sku == null || sku.trim().isEmpty()) {
            return null;
        }

        // Tên phương thức này đúng nếu Category có: @Embedded SkuInfo sku;
        return categoryRepository.findCategoriesBySkuInfo_SKU(sku)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Category với SKU: " + sku));
    }
}