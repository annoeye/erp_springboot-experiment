package com.anno.ERP_SpringBoot_Experiment.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;
import com.anno.ERP_SpringBoot_Experiment.model.enums.SearchOperation;

public class SpecificationBuilder<T> {
    private final List<SearchCriteria> params;

    public SpecificationBuilder() {
        this.params = new ArrayList<>();
    }

    public SpecificationBuilder(List<SearchCriteria> params) {
        this.params = params;
    }

    public SpecificationBuilder<T> with(String key, String operation, Object value) {
        return this;
    }
    
    public SpecificationBuilder<T> with(String key, SearchOperation operation, Object value) {
        params.add(new SearchCriteria(key, operation, value));
        return this;
    }

    public Specification<T> build() {
        return null;
    }
}
