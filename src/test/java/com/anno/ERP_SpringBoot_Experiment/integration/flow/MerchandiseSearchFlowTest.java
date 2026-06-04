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
@DisplayName("Merchandise Search - Real Login Flow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MerchandiseSearchFlowTest extends AbstractIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

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
                    .andExpect(jsonPath("$.data.contents[0].skuInfo").doesNotExist());
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
                    .andExpect(jsonPath("$.data.contents[0].skuInfo").doesNotExist());
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
                    .andExpect(jsonPath("$.content[0].status").doesNotExist())
                    .andExpect(jsonPath("$.content[0].totalSoldQuantity").doesNotExist())
                    .andExpect(jsonPath("$.content[0].totalRevenue").doesNotExist());
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
                    .andExpect(jsonPath("$.content[0].status").doesNotExist())
                    .andExpect(jsonPath("$.content[0].totalSoldQuantity").doesNotExist())
                    .andExpect(jsonPath("$.content[0].totalRevenue").doesNotExist());
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

    private static PagingRequest paging(int size) {
        var p = new PagingRequest();
        p.setSize(size);
        return p;
    }
}
