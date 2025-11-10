package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.AttributesMapper;
import com.anno.ERP_SpringBoot_Experiment.mapper.SpecificationMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.Specification;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iAttributes;
import com.anno.ERP_SpringBoot_Experiment.utils.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttributesService implements iAttributes {

    private final AttributesRepository attributesRepository;
    private final ProductRepository productRepository;
    private final Helper featureMerchandiseHelper;
    private final SpecificationMapper  specificationMapper;
    private final AttributesMapper attributesMapper;
    private final Helper merchandiseHelper;
    private final SecurityUtil securityUtil;

    @Transactional
    public Response<AttributesDto> create(CreateAttributesRequest request) {

        if (request.getStockQuantity() < 0) {
            throw new BusinessException("Số lượng tồn kho không thể là số âm.");
        }

        Product product = productRepository
                .findById(UUID.fromString(request.getProductId()))
                .orElseThrow(() -> new BusinessException("Sản phẩm với ID " + request.getProductId() + " không tồn tại."));


        List<Specification> Specification = request.getData().stream()
                .map(data  -> {
                    String key = merchandiseHelper.generateKey();
                    return new Specification(key, data);
                }).toList();

        AuditInfo audit = new AuditInfo();
        audit.setCreatedAt(LocalDateTime.now());
        audit.setCreatedBy(securityUtil.getCurrentUsername());

        Attributes attributes = new Attributes();
        attributes.setName(request.getName());
        attributes.setAuditInfo(audit);
        attributes.setSku(new SkuInfo());
        attributes.setKeywords(request.getKeywords());
        attributes.setPrice(request.getPrice());
        attributes.setSalePrice(request.getSalePrice());
        attributes.setProduct(product);
        attributes.setStockQuantity(request.getStockQuantity());
        attributes.setSpecifications(Specification);

        return Response.ok(
                attributesMapper.toDto(attributesRepository.save(attributes))
        );
    }
    // Test Create. Thiếu RUD
}
