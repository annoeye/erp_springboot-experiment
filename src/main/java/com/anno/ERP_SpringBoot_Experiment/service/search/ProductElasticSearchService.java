package com.anno.ERP_SpringBoot_Experiment.service.search;

import com.anno.ERP_SpringBoot_Experiment.model.document.ProductDocument;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.GetProductRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductElasticSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private volatile boolean indexInitialized = false;

    public void ensureIndexExists() {
        if (!indexInitialized) {
            synchronized (this) {
                if (!indexInitialized) {
                    try {
                        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
                        if (!indexOps.exists()) {
                            indexOps.create();
                            indexOps.putMapping(indexOps.createMapping());
                            log.info("Đã tạo mới Elasticsearch index 'products' và thiết lập mappings thành công.");
                        }
                        indexInitialized = true;
                    } catch (Exception e) {
                        log.error("Không thể kết nối hoặc khởi tạo index 'products' trong Elasticsearch: {}", e.getMessage());
                    }
                }
            }
        }
    }

    private Criteria buildCriteria(GetProductRequest request) {
        Criteria criteria = new Criteria();

        if (StringUtils.hasText(request.getKeyword())) {
            criteria = criteria.and(new Criteria("name").contains(request.getKeyword())
                    .or(new Criteria("attributes.name").contains(request.getKeyword()))
                    .or(new Criteria("attributes.keywords").contains(request.getKeyword()))
                    .or(new Criteria("sku").contains(request.getKeyword()))
                    .or(new Criteria("attributes.sku").contains(request.getKeyword())));
        }

        if (StringUtils.hasText(request.getCreatedBy())) {
            criteria = criteria.and(new Criteria("createdBy").is(request.getCreatedBy()));
        }

        if (!CollectionUtils.isEmpty(request.getProductIds())) {
            criteria = criteria.and(new Criteria("productId").in(request.getProductIds()));
        }

        if (StringUtils.hasText(request.getCategoryId())) {
            criteria = criteria.and(new Criteria("categoryId").is(request.getCategoryId()));
        }

        if (!CollectionUtils.isEmpty(request.getCategoryIds())) {
            criteria = criteria.and(new Criteria("categoryId").in(request.getCategoryIds()));
        }

        if (!CollectionUtils.isEmpty(request.getSkus())) {
            criteria = criteria.and(new Criteria("sku").in(request.getSkus()));
        }

        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            criteria = criteria.and(new Criteria("status").in(request.getStatuses()));
        }

        if (request.getMinSoldQuantity() != null) {
            criteria = criteria.and(new Criteria("totalSoldQuantity").greaterThanEqual(request.getMinSoldQuantity()));
        }
        if (request.getMaxSoldQuantity() != null) {
            criteria = criteria.and(new Criteria("totalSoldQuantity").lessThanEqual(request.getMaxSoldQuantity()));
        }

        if (request.getMinRevenue() != null) {
            criteria = criteria.and(new Criteria("totalRevenue").greaterThanEqual(request.getMinRevenue()));
        }
        if (request.getMaxRevenue() != null) {
            criteria = criteria.and(new Criteria("totalRevenue").lessThanEqual(request.getMaxRevenue()));
        }

        if (request.getMinOrders() != null) {
            criteria = criteria.and(new Criteria("totalOrders").greaterThanEqual(request.getMinOrders()));
        }
        if (request.getMaxOrders() != null) {
            criteria = criteria.and(new Criteria("totalOrders").lessThanEqual(request.getMaxOrders()));
        }

        if (request.getMinView() != null) {
            criteria = criteria.and(new Criteria("viewCount").greaterThanEqual(request.getMinView()));
        }
        if (request.getMinRating() != null) {
            criteria = criteria.and(new Criteria("averageRating").greaterThanEqual(request.getMinRating()));
        }
        if (request.getMinReviews() != null) {
            criteria = criteria.and(new Criteria("reviewCount").greaterThanEqual(request.getMinReviews()));
        }

        if (request.getCreatedFrom() != null) {
            criteria = criteria.and(new Criteria("createdAt").greaterThanEqual(request.getCreatedFrom()));
        }
        if (request.getCreatedTo() != null) {
            criteria = criteria.and(new Criteria("createdAt").lessThanEqual(request.getCreatedTo()));
        }

        if (request.getUpdatedFrom() != null) {
            criteria = criteria.and(new Criteria("updatedAt").greaterThanEqual(request.getUpdatedFrom()));
        }
        if (request.getUpdatedTo() != null) {
            criteria = criteria.and(new Criteria("updatedAt").lessThanEqual(request.getUpdatedTo()));
        }

        return criteria;
    }

    public List<Long> searchProductIds(GetProductRequest request) {
        ensureIndexExists();
        Criteria criteria = buildCriteria(request);
        Pageable pageable = request.getPaging() != null ? request.getPaging().pageable() : PageRequest.of(0, 10);
        
        Query query = new CriteriaQuery(criteria).setPageable(pageable);
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(query, ProductDocument.class);

        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(ProductDocument::getProductId)
                .collect(Collectors.toList());
    }
    
    public long countProducts(GetProductRequest request) {
        ensureIndexExists();
        Criteria criteria = buildCriteria(request);
        Query query = new CriteriaQuery(criteria);
        return elasticsearchOperations.count(query, ProductDocument.class);
    }
}
