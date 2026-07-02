package com.anno.ERP_SpringBoot_Experiment.repository.search;

import com.anno.ERP_SpringBoot_Experiment.model.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@EnableElasticsearchRepositories
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {
    Optional<ProductDocument> findByProductId(Long productId);
}
