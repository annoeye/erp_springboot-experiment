package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import lombok.Data;
import java.util.List;

import java.time.LocalDateTime;

@Data
public class AttributesSearchRequest {
    private String keyword;
    private String productId;
    private List<String> ids;
    private List<String> productIds;
    private List<String> skus;
    private List<String> statuses;
    private Double minPrice;
    private Double maxPrice;
    private Double minSalePrice;
    private Double maxSalePrice;
    private Integer minStockQuantity;
    private Integer maxStockQuantity;
    private Integer minSoldQuantity;
    private Integer maxSoldQuantity;
    private Double minCostPrice;
    private Double maxCostPrice;
    private String createdBy;
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;
    private LocalDateTime updatedFrom;
    private LocalDateTime updatedTo;
    private PagingRequest paging = new PagingRequest();
}
