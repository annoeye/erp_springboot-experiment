package com.anno.ERP_SpringBoot_Experiment.integration;

import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.AttributesService;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.CategoryService;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.ProductService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.*;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Factory tạo test data thông qua service thật.
 * Data được persist xuống DB và có thể search lại.
 */
@Component
public class TestDataFactory {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final AttributesService attributesService;

    public TestDataFactory(CategoryService categoryService,
                           ProductService productService,
                           AttributesService attributesService) {
        this.categoryService = categoryService;
        this.productService = productService;
        this.attributesService = attributesService;
    }

    /**
     * Tạo một Category với tên cho trước.
     */
    public String createCategory(String name) {
        Response<?> response = categoryService.create(name);
        return response.getStatus().getMessage();
    }

    /**
     * Tạo một Product với tên cho trước + categorySku.
     * Trả về response.
     */
    public Response<?> createProduct(String name, String categorySku) {
        CreateProductRequest request = new CreateProductRequest();
        request.setName(name);
        request.setCategorySku(categorySku);
        request.setStatus("ACTIVE");

        return productService.addProduct(request);
    }

    /**
     * Tạo Attributes cho product.
     * Trả về danh sách AttributesDto.
     */
    public List<AttributesDto> createAttributes(String productSku, String name, double price) {
        CreateAttributesRequest request = new CreateAttributesRequest();
        request.setProductSku(productSku);
        request.setName(name);

        com.anno.ERP_SpringBoot_Experiment.service.dto.request.AttributeInput attrItem =
                new com.anno.ERP_SpringBoot_Experiment.service.dto.request.AttributeInput();
        attrItem.setPrice(BigDecimal.valueOf(price));
        attrItem.setStatusProduct(StockStatus.AVAILABLE);
        request.setAttributes(List.of(attrItem));

        Response<List<AttributesDto>> response = attributesService.create(request);
        return response.getData();
    }

    /**
     * Tạo toàn bộ dữ liệu test mẫu: 2 categories, 2 products mỗi category, 2 attributes mỗi product.
     * Trả về danh sách category SKUs đã tạo.
     */
    public void createFullSampleData() {
        // === CATEGORY 1: "Điện thoại" ===
        createCategory("Điện thoại");
        // search lại để lấy SKU
        CategorySearchRequest catSearch = new CategorySearchRequest();
        catSearch.setNames(List.of("Điện thoại"));
        catSearch.setPaging(new PagingRequest());
        catSearch.getPaging().setSize(10);

        var catPage = categoryService.search(catSearch);
        String category1Sku = catPage.getContent().isEmpty() ? null
                : catPage.getContent().get(0).getSkuInfo().getSku();

        // === PRODUCT 1: "iPhone 15" ===
        if (category1Sku != null) {
            createProduct("iPhone 15", category1Sku);
            createProduct("Samsung Galaxy S24", category1Sku);
        }

        // === CATEGORY 2: "Laptop" ===
        createCategory("Laptop");
        catSearch.setNames(List.of("Laptop"));
        var catPage2 = categoryService.search(catSearch);
        String category2Sku = catPage2.getContent().isEmpty() ? null
                : catPage2.getContent().get(0).getSkuInfo().getSku();

        if (category2Sku != null) {
            createProduct("MacBook Pro", category2Sku);
            createProduct("Dell XPS 15", category2Sku);
        }
    }

    /**
     * Tạo sample data và trả về các category SKU và product name để dùng trong test.
     */
    public SampleData createSampleData() {
        createFullSampleData();
        return new SampleData();
    }

    /**
     * Container cho dữ liệu mẫu.
     */
    public static class SampleData {
        // Có thể thêm các trường nếu cần
    }
}
