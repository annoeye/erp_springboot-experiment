//package com.anno.ERP_SpringBoot_Experiment.web.rest.impl;
//
//import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.AttributesService;
//import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.CategoryService;
//import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.ProductService;
//import com.anno.ERP_SpringBoot_Experiment.service.MinioService;
//import com.anno.ERP_SpringBoot_Experiment.service.dto.*;
//import com.anno.ERP_SpringBoot_Experiment.service.dto.request.*;
//import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.http.MediaType;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.UUID;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * Unit Tests cho MerchandiseController
// *
// * Test Coverage:
// * - Product CRUD endpoints (4 tests)
// * - Product Images management (3 tests)
// * - Category CRUD endpoints (4 tests)
// * - Attributes management (5 tests)
// *
// * Total: 16 test cases
// */
//@WebMvcTest(merchandiseControllerImpl.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//@DisplayName("MerchandiseController Unit Tests")
//class MerchandiseControllerImplTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockitoBean
//    private ProductService productService;
//
//    @MockitoBean
//    private CategoryService categoryService;
//
//    @MockitoBean
//    private AttributesService attributesService;
//
//    @MockitoBean
//    private MinioService minioService;
//
//    private ProductDto mockProductDto;
//    private CategoryDto mockCategoryDto;
//    private AttributesDto mockAttributesDto;
//
//    @BeforeEach
//    void setUp() {
//        // Setup mock DTOs for reuse
//        mockProductDto = ProductDto.builder()
//                .id(UUID.randomUUID())
//                .name("Test Product")
//                .build();
//
//        mockCategoryDto = new CategoryDto(
//                UUID.randomUUID(),
//                "Test Category",
//                new SkuInfoDto("cat-001")
//        );
//
//        mockAttributesDto = new AttributesDto(
//                UUID.randomUUID(),
//                "Test Attribute",
//                new SkuInfoDto("SKU-001"),
//                100000.0,
//                90000.0,
//                10,
//                "Red",
//                "Option1",
//                null,
//                null,
//                null,
//                null,
//                null
//        );
//    }
//
//    // ==================== PRODUCT CRUD TESTS ====================
//
//    @Test
//    @DisplayName("Should successfully add a new product")
//    void testAddProduct_Success() throws Exception {
//        // Arrange
//        when(productService.addProduct(any(CreateProductRequest.class))).thenAnswer(invocation -> Response.ok(mockProductDto));
//
//        // Act & Assert
//        mockMvc.perform(post("/api/merchandise/add-Product")
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .param("name", "Test Product")
//                        .param("price", "100000"))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.data.name").value("Test Product"));
//
//        // Verify
//        verify(productService, times(1)).addProduct(any(CreateProductRequest.class));
//    }
//
//    @Test
//    @DisplayName("Should successfully update an existing product")
//    void testUpdateProduct_Success() throws Exception {
//        // Arrange
//        UpdateProductRequest request = new UpdateProductRequest();
//        request.setId(mockProductDto.getId().toString());
//        request.setName("Updated Product");
//
//        when(productService.updateProduct(any(UpdateProductRequest.class))).thenReturn(Response.ok(mockProductDto));
//
//        // Act & Assert
//        mockMvc.perform(put("/api/merchandise/update-Product")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.name").value("Test Product"));
//
//        // Verify
//        verify(productService, times(1)).updateProduct(any(UpdateProductRequest.class));
//    }
//
//    @Test
//    @DisplayName("Should successfully delete products by IDs")
//    void testDeleteProduct_Success() throws Exception {
//        // Arrange
//        List<String> ids = Arrays.asList(
//                "550e8400-e29b-41d4-a716-446655440000",
//                "550e8400-e29b-41d4-a716-446655440001"
//        );
//        when(productService.deleteProduct(anyList())).thenReturn(Response.noContent());
//
//        // Act & Assert
//        mockMvc.perform(delete("/api/merchandise/delete-Product")
//                        .param("ids", ids.toArray(new String[0])))
//                .andExpect(status().isNoContent());
//
//        // Verify
//        verify(productService, times(1)).deleteProduct(anyList());
//    }
//
//    @Test
//    @DisplayName("Should successfully search products with pagination")
//    void testSearchProduct_Success() throws Exception {
//        // Arrange
//        ProductSearchRequest searchRequest = new ProductSearchRequest();
//        searchRequest.setName("test");
//
//        Page<ProductDto> mockPage = new PageImpl<>(Arrays.asList(mockProductDto));
//        when(productService.search(any(ProductSearchRequest.class))).thenReturn(mockPage);
//
//        // Act & Assert
//        mockMvc.perform(post("/api/merchandise/search-Product")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(searchRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content").isArray())
//                .andExpect(jsonPath("$.content[0].name").value("Test Product"));
//
//        // Verify
//        verify(productService, times(1)).search(any(ProductSearchRequest.class));
//    }
//
//    // ==================== PRODUCT IMAGES TESTS ====================
//
//    @Test
//    @DisplayName("Should successfully add product images")
//    void testAddProductImages_Success() throws Exception {
//        // Arrange
//        String productId = UUID.randomUUID().toString();
//        MockMultipartFile image1 = new MockMultipartFile(
//                "images", "image1.jpg", "image/jpeg", "image1 content".getBytes()
//        );
//        MockMultipartFile image2 = new MockMultipartFile(
//                "images", "image2.jpg", "image/jpeg", "image2 content".getBytes()
//        );
//
//        when(productService.addProductImages(anyString(), anyList())).thenAnswer(invocation -> Response.ok("Images added successfully"));
//
//        // Act & Assert
//        mockMvc.perform(multipart("/api/merchandise/add-Product-Images/" + productId)
//                        .file(image1)
//                        .file(image2)
//                        .contentType(MediaType.MULTIPART_FORM_DATA))
//                .andExpect(status().isOk());
//
//        // Verify
//        verify(productService, times(1)).addProductImages(eq(productId), anyList());
//    }
//
//    @Test
//    @DisplayName("Should successfully delete a product image")
//    void testDeleteProductImage_Success() throws Exception {
//        // Arrange
//        String productId = UUID.randomUUID().toString();
//        String imageKey = "products/image123.jpg";
//
//        when(productService.deleteProductImage(anyString(), anyString())).thenAnswer(invocation -> Response.ok("Image deleted successfully"));
//
//        // Act & Assert
//        mockMvc.perform(delete("/api/merchandise/delete-Product-Image/" + productId)
//                        .param("imageKey", imageKey))
//                .andExpect(status().isOk());
//
//        // Verify
//        verify(productService, times(1)).deleteProductImage(productId, imageKey);
//    }
//
//    @Test
//    @DisplayName("Should successfully replace product images")
//    void testReplaceProductImages_Success() throws Exception {
//        // Arrange
//        String productId = UUID.randomUUID().toString();
//        MockMultipartFile newImage = new MockMultipartFile(
//                "images", "new-image.jpg", "image/jpeg", "new image content".getBytes()
//        );
//
//        when(productService.replaceProductImages(anyString(), anyList())).thenAnswer(invocation -> Response.ok("Images replaced successfully"));
//
//        // Act & Assert
//        mockMvc.perform(multipart("/api/merchandise/replace-Product-Images/" + productId)
//                        .file(newImage)
//                        .with(request -> {
//                            request.setMethod("PUT");
//                            return request;
//                        })
//                        .contentType(MediaType.MULTIPART_FORM_DATA))
//                .andExpect(status().isOk());
//
//        // Verify
//        verify(productService, times(1)).replaceProductImages(eq(productId), anyList());
//    }
//
//    // ==================== CATEGORY CRUD TESTS ====================
//
//    @Test
//    @DisplayName("Should successfully add a new category")
//    void testAddCategory_Success() throws Exception {
//        // Arrange
//        String categoryName = "New Category";
//        when(categoryService.create(anyString())).thenReturn(Response.ok("Category created with ID: cat-123"));
//
//        // Act & Assert
//        mockMvc.perform(post("/api/merchandise/add-Category")
//                        .param("name", categoryName))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.data").exists());
//
//        // Verify
//        verify(categoryService, times(1)).create(categoryName);
//    }
//
//    @Test
//    @DisplayName("Should successfully update a category")
//    void testUpdateCategory_Success() throws Exception {
//        // Arrange
//        CategoryDto updateRequest = new CategoryDto(
//                UUID.randomUUID(),
//                "Updated Category",
//                new SkuInfoDto("cat-001")
//        );
//
//        when(categoryService.update(any(CategoryDto.class))).thenReturn(Response.ok(mockCategoryDto));
//
//        // Act & Assert
//        mockMvc.perform(put("/api/merchandise/update-Category")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(updateRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.name").value("Test Category"));
//
//        // Verify
//        verify(categoryService, times(1)).update(any(CategoryDto.class));
//    }
//
//    @Test
//    @DisplayName("Should successfully delete categories by IDs")
//    void testDeleteCategory_Success() throws Exception {
//        // Arrange
//        List<String> ids = Arrays.asList("cat-001", "cat-002");
//        when(categoryService.delete(anyList())).thenReturn(Response.noContent());
//
//        // Act & Assert
//        mockMvc.perform(delete("/api/merchandise/delete-Category")
//                        .param("ids", ids.toArray(new String[0])))
//                .andExpect(status().isNoContent());
//
//        // Verify
//        verify(categoryService, times(1)).delete(ids);
//    }
//
//    @Test
//    @DisplayName("Should successfully search categories with pagination")
//    void testSearchCategory_WithPaging() throws Exception {
//        // Arrange
//        CategorySearchRequest searchRequest = new CategorySearchRequest();
//        PagingRequest pagingRequest = new PagingRequest();
//        pagingRequest.setPage(1);
//        pagingRequest.setSize(10);
//        searchRequest.setPaging(pagingRequest);
//
//        Page<CategoryDto> mockPage = new PageImpl<>(
//                Arrays.asList(mockCategoryDto),
//                org.springframework.data.domain.PageRequest.of(0, 10),
//                1
//        );
//        when(categoryService.search(any(CategorySearchRequest.class))).thenReturn(mockPage);
//
//        // Act & Assert
//        mockMvc.perform(post("/api/merchandise/search-Category")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(searchRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.contents").isArray())
//                .andExpect(jsonPath("$.data.paging.pageNumber").value(0))
//                .andExpect(jsonPath("$.data.paging.totalPage").value(1));
//
//        // Verify
//        verify(categoryService, times(1)).search(any(CategorySearchRequest.class));
//    }
//
//    // ==================== ATTRIBUTES MANAGEMENT TESTS ====================
//
//    @Test
//    @DisplayName("Should successfully add attributes")
//    void testAddAttributes_Success() throws Exception {
//        // Arrange
//        CreateAttributesRequest request = new CreateAttributesRequest();
//        request.setName("Test Attribute");
//        request.setPrice(100000.0);
//        request.setColor("Red");
//        request.setOption("Option1");
//        request.setStockQuantity(10);
//        request.setProductId(UUID.randomUUID().toString());
//
//        when(attributesService.create(any(CreateAttributesRequest.class))).thenReturn(Response.ok(mockAttributesDto));
//
//        // Act & Assert
//        mockMvc.perform(post("/api/merchandise/add-Attributes")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.data.sku.SKU").value("SKU-001"));
//
//        // Verify
//        verify(attributesService, times(1)).create(any(CreateAttributesRequest.class));
//    }
//
//    @Test
//    @DisplayName("Should successfully update attributes")
//    void testUpdateAttributes_Success() throws Exception {
//        // Arrange
//        UpdateAttributesRequest request = new UpdateAttributesRequest();
//        request.setSku("SKU-001");
//
//        when(attributesService.update(any(UpdateAttributesRequest.class))).thenReturn(Response.ok(mockAttributesDto));
//
//        // Act & Assert
//        mockMvc.perform(put("/api/merchandise/update-Attributes")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.sku.SKU").value("SKU-001"));
//
//        // Verify
//        verify(attributesService, times(1)).update(any(UpdateAttributesRequest.class));
//    }
//
//    @Test
//    @DisplayName("Should successfully delete attributes by SKUs")
//    void testDeleteAttributes_Success() throws Exception {
//        // Arrange
//        List<String> skus = Arrays.asList("SKU-001", "SKU-002");
//        when(attributesService.delete(anyList())).thenReturn(Response.noContent());
//
//        // Act & Assert
//        mockMvc.perform(delete("/api/merchandise/delete-Attributes")
//                        .param("skus", skus.toArray(new String[0])))
//                .andExpect(status().isNoContent());
//
//        // Verify
//        verify(attributesService, times(1)).delete(skus);
//    }
//
//    @Test
//    @DisplayName("Should successfully delete attributes by product ID")
//    void testDeleteAttributesByProduct_Success() throws Exception {
//        // Arrange
//        String productId = UUID.randomUUID().toString();
//        when(attributesService.deleteByProduct(anyString())).thenReturn(Response.noContent());
//
//        // Act & Assert
//        mockMvc.perform(delete("/api/merchandise/delete-Attributes-by-Product/" + productId))
//                .andExpect(status().isNoContent());
//
//        // Verify
//        verify(attributesService, times(1)).deleteByProduct(productId);
//    }
//
//    @Test
//    @DisplayName("Should successfully get attributes by product ID")
//    void testGetAttributesByProduct_Success() throws Exception {
//        // Arrange
//        String productId = UUID.randomUUID().toString();
//        List<AttributesDto> attributesList = Arrays.asList(mockAttributesDto);
//        when(attributesService.getByProduct(anyString())).thenReturn(Response.ok(attributesList));
//
//        // Act & Assert
//        mockMvc.perform(get("/api/merchandise/get-Attributes-by-Product/" + productId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data").isArray())
//                .andExpect(jsonPath("$.data[0].sku.SKU").value("SKU-001"));
//
//        // Verify
//        verify(attributesService, times(1)).getByProduct(productId);
//    }
//
//    @Test
//    @DisplayName("Should successfully get attributes by SKU")
//    void testGetAttributesBySku_Success() throws Exception {
//        // Arrange
//        String sku = "SKU-001";
//        when(attributesService.getBySku(anyString())).thenReturn(Response.ok(mockAttributesDto));
//
//        // Act & Assert
//        mockMvc.perform(get("/api/merchandise/get-Attributes-by-Sku/" + sku))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.sku.SKU").value("SKU-001"));
//
//        // Verify
//        verify(attributesService, times(1)).getBySku(sku);
//    }
//}
