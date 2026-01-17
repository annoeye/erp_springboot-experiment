package com.anno.ERP_SpringBoot_Experiment.service.dto.response.Page;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PagingResponse<T> {
    @Builder.Default
    List<T> contents = new ArrayList<>();

    PageableData paging;

    public PagingResponse<T> setContents(final List<T> contents) {
       this.contents = contents;
       return this;
    }

    public PagingResponse<T> setPaging(final PageableData paging) {
        this.paging = paging;
        return this;
    }
}
