package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.CategorySpecification;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.Specification;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategorySearchRequest extends FilterRequest<Category> {
    String name;
    @Override
    public Specification<Category> specification() {
        return CategorySpecification.builder()
                .withName(name)
                .build();
    }
}
