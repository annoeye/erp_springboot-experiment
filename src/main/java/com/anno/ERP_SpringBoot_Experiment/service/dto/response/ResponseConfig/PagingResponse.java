package com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig;

import com.anno.ERP_SpringBoot_Experiment.config.Views;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PagingResponse<T> {

    @JsonView(Views.Public.class)
    @Builder.Default
    List<T> contents = new ArrayList<>();

    @JsonView(Views.Public.class)
    PageableData paging;

    public static <T> PagingResponse<T> from(Page<T> page) {
        return PagingResponse.<T>builder()
                .contents(page.getContent())
                .paging(PageableData.from(page))
                .build();
    }
}
