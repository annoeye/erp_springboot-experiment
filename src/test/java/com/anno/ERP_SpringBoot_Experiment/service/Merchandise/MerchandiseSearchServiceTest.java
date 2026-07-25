package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SearchCriteria;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SpecificationBuilder;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.AttributesSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.PagingRequest;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchandiseSearchServiceTest {

    private final MerchandiseSearchService searchService = new MerchandiseSearchService(null);

    @Test
    void pageable_UsesDefaultWhenPagingIsNull() {
        var pageable = searchService.pageable(null);

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    void pageable_RejectsInvalidPage() {
        PagingRequest request = new PagingRequest();
        request.setPage(0);

        assertThatThrownBy(() -> searchService.pageable(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void pageable_RejectsInvalidSize() {
        PagingRequest request = new PagingRequest();
        request.setSize(0);

        assertThatThrownBy(() -> searchService.pageable(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void parseLongList_FiltersBlankAndParsesValidValues() {
        var result = searchService.parseLongList(List.of("1", " ", "2"), "ids");

        assertThat(result).containsExactly(1L, 2L);
    }

    @Test
    void parseLongList_RejectsInvalidValue() {
        assertThatThrownBy(() -> searchService.parseLongList(List.of("abc"), "ids"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void parseEnumList_ParsesCaseInsensitiveStatuses() {
        var result = searchService.parseEnumList(List.of("available", "NOT_ACTIVE"), StockStatus.class, "statuses");

        assertThat(result).containsExactly(StockStatus.AVAILABLE, StockStatus.NOT_ACTIVE);
    }

    @Test
    void parseEnumList_RejectsUnknownStatus() {
        assertThatThrownBy(() -> searchService.parseEnumList(List.of("unknown"), StockStatus.class, "statuses"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void specificationBuilderWithStringOperation_AddsCriteria() {
        SpecificationBuilder<Object> builder = new SpecificationBuilder<>();

        builder.with("name", "~", "phone");

        List<SearchCriteria> params = (List<SearchCriteria>) ReflectionTestUtils.getField(builder, "params");
        assertThat(params).hasSize(1);
        assertThat(params.getFirst().getKey()).isEqualTo("name");
        assertThat(params.getFirst().getValue()).isEqualTo("phone");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildAttributesCriteria_EmptyRequest_HasNoDynamicCriteria() {
        List<SearchCriteria> criteria = ReflectionTestUtils.invokeMethod(
                searchService,
                "buildAttributesCriteria",
                new AttributesSearchRequest());

        assertThat(criteria).isEmpty();
    }

    @Test
    void attributesSpecification_EmptyRequest_ReturnsActiveOnlySpecification() {
        var specification = searchService.attributesSpecification(new AttributesSearchRequest());

        assertThat(specification).isNotNull();
    }
}
