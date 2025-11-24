package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.AttributesMapper;
import com.anno.ERP_SpringBoot_Experiment.mapper.SpecificationMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.Specification;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.SetSpecificationsRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iAttributes;
import com.anno.ERP_SpringBoot_Experiment.utils.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import jakarta.transaction.Transactional;
import lombok.NonNull;
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
    private final SpecificationMapper specificationMapper;
    private final AttributesMapper attributesMapper;
    private final Helper merchandiseHelper;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional
    public Response<AttributesDto> create(@NonNull CreateAttributesRequest request) {

        if (request.getStockQuantity() < 0) {
            throw new BusinessException("Số lượng tồn kho không thể là số âm.");
        }

        Product product = productRepository
                .findById(UUID.fromString(request.getProductId()))
                .orElseThrow(() -> new BusinessException("Sản phẩm với ID " + request.getProductId() + " không tồn tại."));

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
        attributes.setStatusProduct(StockStatus.NOT_ACTIVE);
        attributes.setStockQuantity(request.getStockQuantity());
//        attributes.setSpecifications(specifications); // Chưa set thông tin

        Attributes savedAttributes = attributesRepository.save(attributes);
        log.info("Đã tạo attributes '{}' với SKU {} cho sản phẩm {}", 
                savedAttributes.getName(), 
                savedAttributes.getSku().getSKU(), 
                product.getName());

        return Response.ok(
                attributesMapper.toDto(savedAttributes),
                "Tạo attributes thành công. Nhưng chưa hoạt động. Hãy tạo Thông số sản phẩm. "
        );
    }

    @Override
    @Transactional
    public Response<AttributesDto> update(@NonNull UpdateAttributesRequest request) {
        if (request.getSku() == null || request.getSku().isBlank()) {
            throw new BusinessException("SKU của attributes không được để trống.");
        }

        Attributes attributes = attributesRepository.findAttributesBySku_SKU(request.getSku())
                .orElseThrow(() -> new BusinessException("Attributes với SKU " + request.getSku() + " không tồn tại."));

        if (request.getName() != null && !request.getName().isBlank()) {
            attributes.setName(request.getName());
        }

        if (request.getPrice() != null) {
            if (request.getPrice() < 0) {
                throw new BusinessException("Giá không thể là số âm.");
            }
            attributes.setPrice(request.getPrice());
        }

        if (request.getSalePrice() != null) {
            if (request.getSalePrice() < 0) {
                throw new BusinessException("Giá khuyến mãi không thể là số âm.");
            }
            double currentPrice = request.getPrice() != null ? request.getPrice() : attributes.getPrice();
            if (request.getSalePrice() > currentPrice) {
                throw new BusinessException("Giá khuyến mãi không thể lớn hơn giá gốc.");
            }
            attributes.setSalePrice(request.getSalePrice());
        }

        if (request.getStockQuantity() != null) {
            if (request.getStockQuantity() < 0) {
                throw new BusinessException("Số lượng tồn kho không thể là số âm.");
            }
            attributes.setStockQuantity(request.getStockQuantity());
        }

        if (request.getKeywords() != null) {
            attributes.setKeywords(new java.util.HashSet<>(request.getKeywords()));
        }

        if (request.getData() != null && !request.getData().isEmpty()) {
            List<Specification> updatedSpecs = request.getData().stream()
                    .map(data -> {
                        String key = merchandiseHelper.generateKey();
                        return new Specification(key, data);
                    }).toList();
            attributes.setSpecifications(updatedSpecs);
        }

        attributes.getAuditInfo().setUpdatedAt(LocalDateTime.now());
        attributes.getAuditInfo().setUpdatedBy(securityUtil.getCurrentUsername());

        Attributes updatedAttributes = attributesRepository.save(attributes);
        
        log.info("Đã cập nhật attributes '{}' với SKU {}", 
                updatedAttributes.getName(), 
                updatedAttributes.getSku().getSKU());

        return Response.ok(
                attributesMapper.toDto(updatedAttributes),
                "Cập nhật attributes thành công."
        );
    }

    @Override
    @Transactional
    public Response<?> delete(@NonNull List<String> skus) {
        if (skus.isEmpty()) {
            throw new BusinessException("Danh sách SKU không được để trống.");
        }

        List<Attributes> attributesToDelete = attributesRepository.findAttributesBySku_SKUIn(skus);

        if (attributesToDelete.isEmpty()) {
            throw new BusinessException("Không tìm thấy attributes nào với các SKU đã cung cấp.");
        }

        if (attributesToDelete.size() != skus.size()) {
            log.warn("Một số SKU không tồn tại. Yêu cầu: {}, Tìm thấy: {}", 
                    skus.size(), 
                    attributesToDelete.size());
        }

        String currentUser = securityUtil.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

        // Soft delete
        attributesToDelete.forEach(attr -> {
            attr.getAuditInfo().setDeletedAt(now);
            attr.getAuditInfo().setDeletedBy(currentUser);
        });

        attributesRepository.saveAll(attributesToDelete);

        log.info("Đã xóa {} attributes bởi user {}", attributesToDelete.size(), currentUser);

        return Response.ok(
                null,
                String.format("Đã xóa thành công %d attributes.", attributesToDelete.size())
        );
    }

    @Override
    @Transactional
    public Response<?> deleteByProduct(@NonNull String productId) {
        Product product = productRepository.findById(UUID.fromString(productId))
                .orElseThrow(() -> new BusinessException("Sản phẩm với ID " + productId + " không tồn tại."));

        List<Attributes> attributesList = attributesRepository.findAllByProduct(product);

        if (attributesList.isEmpty()) {
            return Response.ok(null, "Sản phẩm không có attributes nào để xóa.");
        }

        String currentUser = securityUtil.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

        attributesList.forEach(attr -> {
            attr.getAuditInfo().setDeletedAt(now);
            attr.getAuditInfo().setDeletedBy(currentUser);
        });

        attributesRepository.saveAll(attributesList);

        log.info("Đã xóa {} attributes của sản phẩm {} bởi user {}", 
                attributesList.size(), 
                product.getName(), 
                currentUser);

        return Response.ok(
                null,
                String.format("Đã xóa thành công %d attributes của sản phẩm.", attributesList.size())
        );
    }

    @Override
    @Transactional
    public Response<List<AttributesDto>> getByProduct(@NonNull String productId) {
        Product product = productRepository.findById(UUID.fromString(productId))
                .orElseThrow(() -> new BusinessException("Sản phẩm với ID " + productId + " không tồn tại."));

        List<Attributes> attributesList = attributesRepository.findAllByProductAndAuditInfo_DeletedAtIsNull(product);

        if (attributesList.isEmpty()) {
            log.info("Sản phẩm {} không có attributes nào", product.getName());
            return Response.ok(List.of(), "Sản phẩm không có attributes.");
        }

        log.info("Đã lấy {} attributes của sản phẩm {}", attributesList.size(), product.getName());
        return Response.ok(
                attributesMapper.toDto(attributesList)
        );
    }

    @Override
    @Transactional
    public Response<AttributesDto> getBySku(@NonNull String sku) {
        Attributes attributes = attributesRepository.findAttributesBySku_SKUAndAuditInfo_DeletedAtIsNull(sku)
                .orElseThrow(() -> new BusinessException("Attributes với SKU " + sku + " không tồn tại."));

        log.info("Đã lấy attributes với SKU {}", sku);
        return Response.ok(
                attributesMapper.toDto(attributes),
                "Lấy attributes thành công."
        );
    }

    @Override
    @Transactional
    public Response<AttributesDto> setSpecifications(SetSpecificationsRequest request) {
        final var attributes = attributesRepository.findById(featureMerchandiseHelper.convertStringToUUID(request.getAttributesId()))
                .orElseThrow(() -> new BusinessException("Attributes không tồn tại"));

        if (request.getOption() == null) {
            throw new BusinessException("Option không được để trống.");
        }

        List<Specification> specifications;
        
        switch (request.getOption()) {
            case CREATE_NEW -> {
                if (request.getSpecificationDto() == null || request.getSpecificationDto().isEmpty()) {
                    throw new BusinessException("Thông số không được để trống.");
                }

                attributes.setStatusProduct(request.getStatus());
                attributes.setSpecifications(request.getSpecificationDto()
                        .stream()
                        .map(specificationMapper::toEntity)
                        .toList());


                log.info("Đã tạo thông số mới cho attributes {}",
                        attributes.getSku().getSKU());
            }
            
            case COPY_FROM_OTHER -> {
                if (request.getSourceAttributesId() == null || request.getSourceAttributesId().isBlank()) {
                    throw new BusinessException("Source Attributes ID không được để trống khi copy.");
                }
                
                Attributes sourceAttributes = attributesRepository
                        .findById(featureMerchandiseHelper.convertStringToUUID(request.getSourceAttributesId()))
                        .orElseThrow(() -> new BusinessException("Source Attributes không tồn tại"));
                
                if (sourceAttributes.getSpecifications() == null || sourceAttributes.getSpecifications().isEmpty()) {
                    throw new BusinessException("Source Attributes không có thông số để copy.");
                }
                
                specifications = sourceAttributes.getSpecifications().stream()
                        .map(spec -> new Specification(spec.getKey(), spec.getData()))
                        .toList();
                
                attributes.setSpecifications(specifications);
                log.info("Đã copy {} thông số từ attributes {} sang attributes {}", 
                        specifications.size(),
                        sourceAttributes.getSku().getSKU(),
                        attributes.getSku().getSKU());
            }
        }
        
        attributes.getAuditInfo().setUpdatedAt(LocalDateTime.now());
        attributes.getAuditInfo().setUpdatedBy(securityUtil.getCurrentUsername());
        
        if (attributes.getStatusProduct() == StockStatus.NOT_ACTIVE) {
            attributes.setStatusProduct(request.getStatus());
            log.info("Đã kích hoạt trạng thái: {}", request.getStatus());
        }
        
        Attributes savedAttributes = attributesRepository.save(attributes);
        
        return Response.ok(
                attributesMapper.toDto(savedAttributes),
                "Thiết lập thông số sản phẩm thành công."
        );
    }
}
