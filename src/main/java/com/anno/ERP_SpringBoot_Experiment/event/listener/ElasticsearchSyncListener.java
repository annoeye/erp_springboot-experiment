package com.anno.ERP_SpringBoot_Experiment.event.listener;

import com.anno.ERP_SpringBoot_Experiment.model.document.AttributeDocument;
import com.anno.ERP_SpringBoot_Experiment.model.document.ProductDocument;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.search.ProductSearchRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchSyncListener {

    private final ProductSearchRepository productSearchRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "product-events", groupId = "erp-elasticsearch-sync-group")
    public void handleProductEvent(String message) {
        log.info("Received product event for Elasticsearch sync: {}", message);
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            Long productId = rootNode.get("productId").asLong();
            String eventType = rootNode.get("eventType").asText();

            if ("PRODUCT_DELETED".equals(eventType)) {
                productSearchRepository.findByProductId(productId)
                        .ifPresent(doc -> productSearchRepository.deleteById(doc.getId()));
                log.info("Deleted product {} from Elasticsearch", productId);
            } else {
                productRepository.findByIdWithDetails(productId).ifPresent(product -> {
                    ProductDocument document = mapToDocument(product);
                    productSearchRepository.save(document);
                    log.info("Synced product {} to Elasticsearch", productId);
                });
            }
        } catch (Exception e) {
            log.error("Error syncing product to Elasticsearch", e);
        }
    }

    private ProductDocument mapToDocument(Product product) {
        List<AttributeDocument> attrDocs = product.getAttributes().stream()
                .map(attr -> AttributeDocument.builder()
                        .attributeId(attr.getId())
                        .name(attr.getName())
                        .sku(attr.getSku() != null ? attr.getSku().getSku() : null)
                        .price(attr.getPrice())
                        .salePrice(attr.getSalePrice())
                        .statusProduct(attr.getStatusProduct())
                        .keywords(attr.getKeywords())
                        .build())
                .collect(Collectors.toList());

        return ProductDocument.builder()
                .id(product.getId().toString())
                .productId(product.getId())
                .name(product.getName())
                .sku(product.getSkuInfo() != null ? product.getSkuInfo().getSku() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .status(product.getStatus())
                .discountPercent(product.getDiscountPercent())
                .totalSoldQuantity(product.getTotalSoldQuantity())
                .averageRating(product.getAverageRating())
                .attributes(attrDocs)
                .build();
    }
}
