package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.ProductSpecification;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.FilterRequest;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.Specification;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductSearchRequest extends FilterRequest<Product> {
    String name;
    @Override
    public Specification<Product> specification() {
        return ProductSpecification.builder()
                .withName(name)
                .build();
    }
}
