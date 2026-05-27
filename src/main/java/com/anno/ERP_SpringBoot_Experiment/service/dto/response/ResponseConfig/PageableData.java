package com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig;

import com.anno.ERP_SpringBoot_Experiment.config.Views;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageableData {

    @JsonView(Views.Public.class)
    int pageNumber;

    @JsonView(Views.Public.class)
    int pageSize;

    @JsonView(Views.Public.class)
    int totalPages;

    @JsonView(Views.Public.class)
    long totalElements;

    public static PageableData from(Page<?> page) {
        return PageableData.builder()
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .build();
    }
}
