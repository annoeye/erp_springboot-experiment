package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import lombok.Data;
import org.springframework.data.jpa.domain.Specification;

@Data
public abstract class FilterRequest<T> {
    private PagingRequest paging = new PagingRequest();

    public abstract Specification<T> specification();
}
