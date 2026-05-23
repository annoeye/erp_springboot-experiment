package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GetProductRequest {
    private String keyword;
    private String categoryId;
    private String createdBy;
    private List<String> productIds;
    private List<String> statuses;
    private List<String> categoryIds;
    private Integer minSoldQuantity;
    private Integer maxSoldQuantity;
    private Double minRevenue;
    private Double maxRevenue;
    private Integer minOrders;
    private Integer maxOrders;
    private Integer minView;
    private Double minRating;
    private Integer minReviews;
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;
    private LocalDateTime updatedFrom;
    private LocalDateTime updatedTo;
    private PagingRequest paging = new PagingRequest();
}
