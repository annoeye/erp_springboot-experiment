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
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    @Override
    public Response<AttributesDto> create(CreateAttributesRequest request) {

        Optional<Product> optionalProduct = productRepository
                .findProductById(featureMerchandiseHelper.convertStringToUUID(request.getProductId()));

        List<Specification> Specification = request.getData().stream()
                .map(data  -> {
                    String key = merchandiseHelper.generateKey();
                    return new Specification(key, data);
                })
                .toList();

        Attributes attributes = new Attributes();
        attributes.setName(request.getName());
        attributes.setAuditInfo(new AuditInfo());
        attributes.setSku(new SkuInfo());
        attributes.setKeywords(request.getKeywords());
        attributes.setPrice(request.getPrice());
        attributes.setSalePrice(request.getSalePrice());
        attributes.setProduct(
                optionalProduct.orElseThrow(
                        () -> new BusinessException("Sản phẩm không tồn tại."))
        );
        attributes.setStockQuantity(request.getStockQuantity());
        attributes.setSpecifications(Specification);

        return Response.ok(
                attributesMapper.toDto(attributesRepository.save(attributes))
        );
    }
}
