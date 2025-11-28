
package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.AttributesMapper;
import com.anno.ERP_SpringBoot_Experiment.mapper.SpecificationMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.Specification;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.SpecificationDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.SetSpecificationsRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.SetSpecificationsRequest.SpecificationOption;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.utils.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttributesServiceTest {

    @Mock
    private AttributesRepository attributesRepository;

    @Mock
    private AttributesMapper attributesMapper;

    @Mock
    private SpecificationMapper specificationMapper;

    @Mock
    private Helper featureMerchandiseHelper;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private AttributesService attributesService;

    @BeforeEach
    void setUp() {
        // Additional setup if needed
    }

    @Test
    void shouldSuccessfullyCreateNewSpecificationsWhenOptionIsCreateNewAndSpecificationDtoIsValid() {
        // Given
        String attributesId = "12345678123456781234567812345678";
        UUID attributesUUID = UUID.fromString("12345678-1234-5678-1234-567812345678");
        
        List<SpecificationDto> specificationDtos = List.of(
            new SpecificationDto("color", "Red"),
            new SpecificationDto("size", "M")
        );
        
        SetSpecificationsRequest request = new SetSpecificationsRequest();
        request.setAttributesId(attributesId);
        request.setOption(SpecificationOption.CREATE_NEW);
        request.setSpecificationDto(specificationDtos);
        request.setStatus(StockStatus.AVAILABLE);
        
        Attributes attributes = new Attributes();
        attributes.setId(attributesUUID);
        attributes.setStatusProduct(StockStatus.NOT_ACTIVE);
        attributes.setSku(new SkuInfo());
        attributes.getSku().setSKU("SKU-123");
        AuditInfo auditInfo = new AuditInfo();
        attributes.setAuditInfo(auditInfo);
        
        List<Specification> specifications = List.of(
            new Specification("abc12", "Red"),
            new Specification("def34", "M")
        );
        
        Attributes savedAttributes = new Attributes();
        savedAttributes.setId(attributesUUID);
        savedAttributes.setStatusProduct(StockStatus.AVAILABLE);
        savedAttributes.setSpecifications(specifications);
        savedAttributes.setSku(new SkuInfo());
        savedAttributes.getSku().setSKU("SKU-123");
        savedAttributes.setAuditInfo(auditInfo);
        
        AttributesDto mockAttributesDto = new AttributesDto(
            attributesUUID,           // id
            "Test Attribute",         // name
            null,                     // sku
            100.0,                    // price
            90.0,                     // salePrice
            10,                       // stockQuantity
            StockStatus.AVAILABLE,    // statusProduct
            null,                     // specifications
            null,                     // keywords
            null,                     // auditInfo
            null                      // product
        );
        
        when(featureMerchandiseHelper.convertStringToUUID(attributesId)).thenReturn(attributesUUID);
        when(attributesRepository.findById(attributesUUID)).thenReturn(Optional.of(attributes));
        when(specificationMapper.toEntity(any(SpecificationDto.class)))
            .thenReturn(new Specification("abc12", "Red"))
            .thenReturn(new Specification("def34", "M"));
        when(securityUtil.getCurrentUsername()).thenReturn("testuser");
        when(attributesRepository.save(any(Attributes.class))).thenReturn(savedAttributes);
        when(attributesMapper.toDto(any(Attributes.class))).thenReturn(mockAttributesDto);
        
        // When
        Response<AttributesDto> response = attributesService.setSpecifications(request);
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("Thiết lập thông số sản phẩm thành công.", response.getStatus());
        verify(attributesRepository).findById(attributesUUID);
        verify(attributesRepository).save(any(Attributes.class));
        verify(specificationMapper, times(2)).toEntity(any(SpecificationDto.class));
        verify(securityUtil).getCurrentUsername();
    }
}
