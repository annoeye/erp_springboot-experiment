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
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iAttributes;
import com.anno.ERP_SpringBoot_Experiment.utils.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
    public Response<List<AttributesDto>> create(@NonNull CreateAttributesRequest request) {
        if (request.getStockQuantity() < 0) {
            throw new BusinessException(ErrorCode.PRODUCT_OUT_OF_STOCK, "Số lượng tồn kho không thể là số âm.");
        }

        List<String> colorsList = request.getColors();
        List<String> optionsList = request.getOptions();

        Product product = productRepository
                .findById(featureMerchandiseHelper.convertStringToUUID(request.getProductId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm không tồn tại."));

        String currentUser = securityUtil.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

        List<Attributes> attributesList = new ArrayList<>();

        for (String option : optionsList) {
            for (String color : colorsList) {
                AuditInfo audit = new AuditInfo();
                audit.setCreatedAt(now);
                audit.setCreatedBy(currentUser);

                Attributes attr = new Attributes();
                attr.setProduct(product);
                attr.setSku(new SkuInfo());
                attr.setName(request.getName());
                attr.setAuditInfo(audit);
                attr.setPrice(request.getPrice());
                attr.setSalePrice(request.getSalePrice());
                attr.setStockQuantity(request.getStockQuantity());
                attr.setOption(option);
                attr.setColor(color);
                attr.setKeywords(request.getKeywords());

                // Map specifications
                if (request.getSpecifications() != null && !request.getSpecifications().isEmpty()) {
                    List<Specification> specs = request.getSpecifications().stream()
                            .map(specificationMapper::toEntity)
                            .toList();
                    attr.setSpecifications(specs);
                    attr.setStatusProduct(request.getStatusProduct());
                } else {
                    attr.setStatusProduct(StockStatus.NOT_ACTIVE);
                }

                attributesList.add(attr);
            }
        }

        List<Attributes> savedList = attributesRepository.saveAll(attributesList);

        log.info("Đã tạo {} attributes cho sản phẩm {} | options: {} | colors: {}",
                savedList.size(),
                product.getName(),
                optionsList.size(),
                colorsList.size());

        String message = savedList.size() == 1
                ? "Tạo attributes thành công."
                : String.format("Đã tạo thành công %d variants từ %d options × %d colors.",
                        savedList.size(), optionsList.size(), colorsList.size());

        return Response.ok(attributesMapper.toDto(savedList), message);
    }

    @Override
    @Transactional
    public Response<AttributesDto> update(@NonNull UpdateAttributesRequest request) {
        if (request.getSku() == null || request.getSku().isBlank()) {
            throw new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND, "Mã định danh của thuộc tính sản phẩm không được để trống.");
        }

        Attributes attributes = attributesRepository.findAttributesBySku_SKU(request.getSku())
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND, "Thuộc tính sản phẩm không tồn tại."));

        if (request.getName() != null && !request.getName().isBlank()) {
            attributes.setName(request.getName());
        }

        if (request.getPrice() != null) {
            if (request.getPrice() < 0) {
                throw new BusinessException(ErrorCode.INVALID_PRICE, "Giá của sản phẩm thể là số âm.");
            }
            attributes.setPrice(request.getPrice());
        }

        if (request.getSalePrice() != null) {
            if (request.getSalePrice() < 0) {
                throw new BusinessException(ErrorCode.INVALID_PRICE, "Giá khuyến mãi không thể là số âm.");
            }
            double currentPrice = request.getPrice() != null ? request.getPrice() : attributes.getPrice();
            if (request.getSalePrice() > currentPrice) {
                throw new BusinessException(ErrorCode.INVALID_PRICE, "Giá khuyến mãi không thể lớn hơn giá gốc.");
            }
            attributes.setSalePrice(request.getSalePrice());
        }

        if (request.getStockQuantity() != null) {
            if (request.getStockQuantity() < 0) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY, "Số lượng tồn kho không thể là số âm.");
            }
            attributes.setStockQuantity(request.getStockQuantity());
        }

        if (request.getKeywords() != null) {
            attributes.setKeywords(new HashSet<>(request.getKeywords()));
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
                updatedAttributes.getSku().getSku());

        return Response.ok(
                attributesMapper.toDto(updatedAttributes),
                "Cập nhật attributes thành công.");
    }

    @Override
    @Transactional
    public Response<?> delete(@NonNull List<String> skus) {
        if (skus.isEmpty()) {
            throw new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND, "Mã định danh không được để trống.");
        }

        List<Attributes> attributesToDelete = attributesRepository.findAttributesBySku_SKUIn(skus);

        if (attributesToDelete.isEmpty()) {
            throw new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND, "Không tìm thấy Danh mục với mã định danh đã cung cấp.");
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
                String.format("Đã xóa thành công %d attributes.", attributesToDelete.size()));
    }

    @Override
    @Transactional
    public Response<?> deleteByProduct(@NonNull String productId) {
        Product product = productRepository.findById(featureMerchandiseHelper.convertStringToUUID(productId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm không tồn tại."));

        List<Attributes> attributesList = attributesRepository.findAllByProduct(product);

        if (attributesList.isEmpty()) {
            return Response.ok(null, "Sản phẩm không có danh mục nào để xóa.");
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
                String.format("Đã xóa thành công %d danh mục của sản phẩm.", attributesList.size()));
    }

    @Override
    @Transactional
    public Response<List<AttributesDto>> getByProduct(@NonNull String productId) {
        Product product = productRepository.findById(featureMerchandiseHelper.convertStringToUUID(productId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm không tồn tại."));

        List<Attributes> attributesList = attributesRepository.findAllByProductAndAuditInfo_DeletedAtIsNull(product);

        if (attributesList.isEmpty()) {
            log.info("Sản phẩm {} không có attributes nào", product.getName());
            throw new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND, "Không có danh mục nào trong sản phẩm.");
        }

        log.info("Đã lấy {} attributes của sản phẩm {}", attributesList.size(), product.getName());
        return Response.ok(
                attributesMapper.toDto(attributesList));
    }

    @Override
    @Transactional
    public Response<AttributesDto> getBySku(@NonNull String sku) {
        Attributes attributes = attributesRepository.findAttributesBySku_SKUAndAuditInfo_DeletedAtIsNull(sku)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND, "Danh mục sản phẩm mới mã này không tồn tại."));

        log.info("Đã lấy attributes với SKU {}", sku);
        return Response.ok(
                attributesMapper.toDto(attributes),
                "Lấy attributes thành công.");
    }

}
