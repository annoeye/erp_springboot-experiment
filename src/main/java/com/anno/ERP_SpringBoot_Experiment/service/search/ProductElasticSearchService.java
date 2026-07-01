package com.anno.ERP_SpringBoot_Experiment.service.search;

import com.anno.ERP_SpringBoot_Experiment.model.document.ProductDocument;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.GetProductRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductElasticSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public List<Long> searchProductIds(GetProductRequest request) {
        co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder boolQueryBuilder =
                new co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder();

        // Keyword Search
        if (StringUtils.hasText(request.getKeyword())) {
            boolQueryBuilder.must(m -> m.multiMatch(mm -> mm
                    .query(request.getKeyword())
                    .fields("name", "attributes.name", "attributes.keywords", "sku", "attributes.sku")
            ));
        }

        // Category Filter
        if (StringUtils.hasText(request.getCategoryId())) {
            boolQueryBuilder.filter(f -> f.term(t -> t
                    .field("categoryId")
                    .value(request.getCategoryId())
            ));
        }

        // Status Filter
        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            boolQueryBuilder.filter(f -> f.terms(t -> t
                    .field("status")
                    .terms(terms -> terms.value(request.getStatuses().stream()
                            .map(Object::toString)
                            .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                            .collect(Collectors.toList())))
            ));
        }

        // Build the query
        Pageable pageable = request.getPaging() != null ? request.getPaging().pageable() : PageRequest.of(0, 10);

        Query searchQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withPageable(pageable)
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(searchQuery, ProductDocument.class);

        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(ProductDocument::getProductId)
                .collect(Collectors.toList());
    }
    
    public long countProducts(GetProductRequest request) {
        co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder boolQueryBuilder =
                new co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder();

        if (StringUtils.hasText(request.getKeyword())) {
            boolQueryBuilder.must(m -> m.multiMatch(mm -> mm
                    .query(request.getKeyword())
                    .fields("name", "attributes.name", "attributes.keywords", "sku", "attributes.sku")
            ));
        }
        
        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            boolQueryBuilder.filter(f -> f.terms(t -> t
                    .field("status")
                    .terms(terms -> terms.value(request.getStatuses().stream()
                            .map(Object::toString)
                            .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                            .collect(Collectors.toList())))
            ));
        }

        Query countQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .build();

        return elasticsearchOperations.count(countQuery, ProductDocument.class);
    }
}
