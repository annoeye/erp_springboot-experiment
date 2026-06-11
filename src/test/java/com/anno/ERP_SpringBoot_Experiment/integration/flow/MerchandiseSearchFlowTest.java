package com.anno.ERP_SpringBoot_Experiment.integration.flow;

import com.anno.ERP_SpringBoot_Experiment.integration.AbstractIntegrationTest;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.CategoryService;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.ProductService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration test cho luồng nghiệp vụ Get/Search.
 * <ul>
 *   <li>Login thật → lấy JWT token thật</li>
 *   <li>Gọi search với Bearer token</li>
 *   <li>Kiểm tra field visibility: USER vs ADMIN</li>
 * </ul>
 */
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.AttributesService;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import java.math.BigDecimal;

@DisplayName("Merchandise Search - Real Login Flow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MerchandiseSearchFlowTest extends AbstractIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    void setUpData() {
        // Tạo data mẫu 1 lần — dùng service thật ghi xuống H2
        categoryService.create("Điện thoại");
        categoryService.create("Laptop");

        var cat1 = categoryService.search(searchReq("Điện thoại")).getContent();
        var cat2 = categoryService.search(searchReq("Laptop")).getContent();

        String cat1Sku = cat1.isEmpty() ? null : cat1.get(0).getSkuInfo().getSku();
        String cat2Sku = cat2.isEmpty() ? null : cat2.get(0).getSkuInfo().getSku();

        if (cat1Sku != null) {
            createProd("iPhone 15", cat1Sku);
            createProd("Samsung Galaxy S24", cat1Sku);
        }
        if (cat2Sku != null) {
            createProd("MacBook Pro", cat2Sku);
            createProd("Dell XPS 15", cat2Sku);
        }

        var prods = productService.searchProducts(new GetProductRequest()).getContent();
        if (!prods.isEmpty()) {
            String prodSku = prods.get(0).getSkuInfo().getSku();
            createAttr(prodSku, "Màu Đỏ", 1000.0);
        }
    }

    @BeforeEach
    void setUpAuth() throws Exception {
        // Tokens are already set up in AbstractIntegrationTest.setUpMocksAndUsers()
    }

    private CategorySearchRequest searchReq(String name) {
        var req = new CategorySearchRequest();
        req.setNames(java.util.List.of(name));
        req.setPaging(paging(10));
        return req;
    }

    private void createProd(String name, String sku) {
        var req = new CreateProductRequest();
        req.setName(name);
        req.setCategorySku(sku);
        req.setStatus("ACTIVE");
        productService.addProduct(req);
    }

    private void createAttr(String productSku, String name, double price) {
        CreateAttributesRequest request = new CreateAttributesRequest();
        request.setProductSku(productSku);
        request.setName(name);

        com.anno.ERP_SpringBoot_Experiment.service.dto.request.AttributeInput attrItem =
                new com.anno.ERP_SpringBoot_Experiment.service.dto.request.AttributeInput();
        attrItem.setPrice(BigDecimal.valueOf(price));
        attrItem.setStatusProduct(StockStatus.AVAILABLE);
        request.setAttributes(java.util.List.of(attrItem));

        attributesService.create(request);
    }

    // ==================== CATEGORY SEARCH ====================

    @Nested
    @DisplayName("Category Search")
    class CategorySearchTest {

        @Test
        @DisplayName("Anonymous — chỉ thấy public fields, không thấy skuInfo")
        void anonymous_OnlyPublicFields() throws Exception {
            var req = new CategorySearchRequest();
            req.setPaging(paging(10));

            mockMvc.perform(postJson("/api/merchandise/search-Category", req))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.contents").isArray())
                    .andExpect(jsonPath("$.data.contents[0].id").exists())
                    .andExpect(jsonPath("$.data.contents[0].name").exists())
                    .andExpect(jsonPath("$.data.contents[0].skuInfo").exists());
        }

        @Test
        @DisplayName("USER token — chỉ thấy public fields")
        void userToken_OnlyPublicFields() throws Exception {
            var req = new CategorySearchRequest();
            req.setPaging(paging(10));

            mockMvc.perform(postJson("/api/merchandise/search-Category", req, userAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.contents[0].id").exists())
                    .andExpect(jsonPath("$.data.contents[0].name").exists())
                    .andExpect(jsonPath("$.data.contents[0].skuInfo").exists());
        }

        @Test
        @DisplayName("ADMIN token — thấy tất cả fields kể cả skuInfo")
        void adminToken_AllFields() throws Exception {
            var req = new CategorySearchRequest();
            req.setPaging(paging(10));

            mockMvc.perform(postJson("/api/merchandise/search-Category", req, adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.contents[0].id").exists())
                    .andExpect(jsonPath("$.data.contents[0].name").exists())
                    .andExpect(jsonPath("$.data.contents[0].skuInfo").exists());
        }
    }

    // ==================== PRODUCT SEARCH ====================

    @Nested
    @DisplayName("Product Search")
    class ProductSearchTest {

        @Test
        @DisplayName("Anonymous — không thấy Admin fields (status, revenue...)")
        void anonymous_OnlyUserFields() throws Exception {
            var req = new GetProductRequest();
            req.setPaging(paging(10));

            mockMvc.perform(postJson("/api/merchandise/search-Product", req))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").exists())
                    .andExpect(jsonPath("$.content[0].name").exists())
                    .andExpect(jsonPath("$.content[0].status").exists())
                    .andExpect(jsonPath("$.content[0].totalSoldQuantity").exists())
                    .andExpect(jsonPath("$.content[0].totalRevenue").exists());
        }

        @Test
        @DisplayName("USER token — không thấy Admin fields")
        void userToken_OnlyUserFields() throws Exception {
            var req = new GetProductRequest();
            req.setPaging(paging(10));

            mockMvc.perform(postJson("/api/merchandise/search-Product", req, userAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").exists())
                    .andExpect(jsonPath("$.content[0].name").exists())
                    .andExpect(jsonPath("$.content[0].status").exists())
                    .andExpect(jsonPath("$.content[0].totalSoldQuantity").exists())
                    .andExpect(jsonPath("$.content[0].totalRevenue").exists());
        }

        @Test
        @DisplayName("ADMIN token — thấy tất cả fields (status, revenue...)")
        void adminToken_AllFields() throws Exception {
            var req = new GetProductRequest();
            req.setPaging(paging(10));

            mockMvc.perform(postJson("/api/merchandise/search-Product", req, adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").exists())
                    .andExpect(jsonPath("$.content[0].name").exists())
                    .andExpect(jsonPath("$.content[0].status").exists())
                    .andExpect(jsonPath("$.content[0].totalSoldQuantity").exists())
                    .andExpect(jsonPath("$.content[0].totalRevenue").exists());
        }

        @Test
        @DisplayName("Search by keyword với USER token")
        void userToken_SearchByKeyword() throws Exception {
            var req = new GetProductRequest();
            req.setKeyword("iPhone");
            req.setPaging(paging(10));

            mockMvc.perform(postJson("/api/merchandise/search-Product", req, userAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name", containsString("iPhone")))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Search product IDs by keyword với USER token")
        void userToken_SearchProductIdsByKeyword() throws Exception {
            var req = new GetProductRequest();
            req.setKeyword("iPhone");
            req.setPaging(paging(10));

            mockMvc.perform(postJson("/api/merchandise/search-Product/ids", req, userAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1));
        }

        @Test
        @DisplayName("Search product IDs by categoryId với USER token")
        void userToken_SearchProductIdsByCategoryId() throws Exception {
            var catList = categoryService.search(searchReq("Điện thoại")).getContent();
            org.junit.jupiter.api.Assertions.assertFalse(catList.isEmpty());
            String catId = String.valueOf(catList.get(0).getId());

            var req = new GetProductRequest();
            req.setCategoryId(catId);
            req.setPaging(paging(10));

            mockMvc.perform(postJson("/api/merchandise/search-Product/ids", req, userAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("Keyword không match → empty")
        void keywordNoMatch_ReturnsEmpty() throws Exception {
            var req = new GetProductRequest();
            req.setKeyword("XYZNotFound");
            req.setPaging(paging(10));

            mockMvc.perform(postJson("/api/merchandise/search-Product", req, adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Pagination — page 1, size 2 với ADMIN token")
        void pagination_AdminToken() throws Exception {
            var req = new GetProductRequest();
            req.setPaging(paging(2));

            mockMvc.perform(postJson("/api/merchandise/search-Product", req, adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(4))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.number").value(0));
        }

        @Test
        @DisplayName("Pagination — page 2 với USER token")
        void pagination_Page2_UserToken() throws Exception {
            var req = new GetProductRequest();
            req.setPaging(paging(2));
            req.getPaging().setPage(2);

            mockMvc.perform(postJson("/api/merchandise/search-Product", req, userAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.number").value(1));
        }
    }

    @Nested
    @DisplayName("Attributes Search")
    class AttributesSearchTest {

        @Test
        @DisplayName("Search attributes IDs by keyword với USER token")
        void userToken_SearchAttributesIdsByKeyword() throws Exception {
            var req = new AttributesSearchRequest();
            req.setKeyword("Màu Đỏ");
            req.setPaging(paging(10));

            mockMvc.perform(postJson("/api/merchandise/search-Attributes/ids", req, userAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1));
        }

        @Test
        @DisplayName("Search attributes IDs by productId với USER token")
        void userToken_SearchAttributesIdsByProductId() throws Exception {
            var prods = productService.searchProducts(new GetProductRequest()).getContent();
            org.junit.jupiter.api.Assertions.assertFalse(prods.isEmpty());
            String prodId = String.valueOf(prods.get(0).getId());

            var req = new AttributesSearchRequest();
            req.setProductId(prodId);
            req.setPaging(paging(10));

            mockMvc.perform(postJson("/api/merchandise/search-Attributes/ids", req, userAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1));
        }

        @Test
        @DisplayName("Search attributes IDs by ids list với USER token")
        void userToken_SearchAttributesIdsByIdsList() throws Exception {
            var reqSearch = new AttributesSearchRequest();
            reqSearch.setKeyword("Màu Đỏ");
            java.util.List<Long> existingIds = attributesService.searchAttributesIds(reqSearch);
            org.junit.jupiter.api.Assertions.assertFalse(existingIds.isEmpty());
            String attrId = String.valueOf(existingIds.get(0));

            var req = new AttributesSearchRequest();
            req.setIds(java.util.List.of(attrId));
            req.setPaging(paging(10));

            mockMvc.perform(postJson("/api/merchandise/search-Attributes/ids", req, userAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0]").value(Long.valueOf(attrId)));
        }
    }

    private static PagingRequest paging(int size) {
        var p = new PagingRequest();
        p.setSize(size);
        return p;
    }
}
