package com.anno.ERP_SpringBoot_Experiment.mapper;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.SpecificationGroup;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.Specificationa;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AttributesMapperTest {

    private AttributesMapper attributesMapper;

    @BeforeEach
    void setUp() {
        AttributesMapperImpl mapper = new AttributesMapperImpl();
        ReflectionTestUtils.setField(mapper, "specificationMapper", new SpecificationMapperImpl());
        attributesMapper = mapper;
    }

    @Test
    void toDto_MapsSpecificationGroupsIntoResponseFields() {
        Attributes attributes = Attributes.builder()
                .specifications(List.of(SpecificationGroup.builder()
                        .groupName("Display")
                        .specifications(List.of(Specificationa.builder()
                                .name("Resolution")
                                .value("4K")
                                .build()))
                        .build()))
                .build();

        AttributesDto dto = attributesMapper.toDto(attributes);

        assertThat(dto.getSpecifications()).hasSize(1);
        assertThat(dto.getSpecifications().getFirst().getGroupName()).isEqualTo("Display");
        assertThat(dto.getSpecifications().getFirst().getSpecifications()).hasSize(1);
        assertThat(dto.getSpecifications().getFirst().getSpecifications().getFirst().getKey()).isEqualTo("Resolution");
        assertThat(dto.getSpecifications().getFirst().getSpecifications().getFirst().getData()).isEqualTo("4K");
    }
}
