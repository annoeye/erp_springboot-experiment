package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.AttributesMapper;
import com.anno.ERP_SpringBoot_Experiment.mapper.PromotionMapper;
import com.anno.ERP_SpringBoot_Experiment.mapper.SpecificationMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.Promotion;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.Specification;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SpecificationGroup;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.VariantOption;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.AttributesSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.VariantGroupInput;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SearchCriteria;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SpecificationBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iAttributes;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final PromotionMapper promotionMapper;
    private final AttributesMapper attributesMapper;
    private final Helper merchandiseHelper;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional
    public Response<List<AttributesDto>> create(@NonNull CreateAttributesRequest request) {
        if (request.getStockQuantity() < 0) {
            throw new BusinessException(ErrorCode.PRODUCT_OUT_OF_STOCK, "Số lượng tồn kho không thể là số âm.");
        }

        Product product = productRepository
                .findById(featureMerchandiseHelper.convertStringToUUID(request.getProductId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm không tồn tại."));

        // Tạo tổ hợp Cartesian product N chiều từ variantGroups
        List<List<VariantOption>> combinations = generateCartesianProduct(request.getVariantGroups());

        List<Attributes> attributesList = new ArrayList<>();
        for (List<VariantOption> combination : combinations) {
            Attributes attr = new Attributes();
            attr.setProduct(product);
            attr.getSku().createSku(product.getName());
            attr.setName(request.getName());
            attr.setPrice(request.getPrice());
            attr.setSalePrice(request.getSalePrice());
            attr.setStockQuantity(request.getStockQuantity());
            attr.setVariantOptions(combination);
            attr.setKeywords(request.getKeywords());

            if (request.getPromotions() != null && !request.getPromotions().isEmpty()) {
                List<Promotion> promo = request.getPromotions().stream()
                        .map(promotionMapper::toEntity)
                        .toList();
                attr.setPromotions(promo);
            }

            if (request.getSpecifications() != null && !request.getSpecifications().isEmpty()) {
                List<SpecificationGroup> specGroups = mapSpecificationGroups(request.getSpecifications());
                attr.setSpecifications(specGroups);
                attr.setStatusProduct(request.getStatusProduct());
            } else {
                attr.setStatusProduct(StockStatus.NOT_ACTIVE);
            }

            attributesList.add(attr);
        }

        List<Attributes> savedList = attributesRepository.saveAll(attributesList);

        log.info("Đã tạo {} attributes cho sản phẩm {} | variant groups: {}",
                savedList.size(),
                product.getName(),
                request.getVariantGroups().size());

        String message = savedList.size() == 1
                ? "Tạo attributes thành công."
                : String.format("Đã tạo thành công %d variants từ %d variant groups.",
                        savedList.size(), request.getVariantGroups().size());

        return Response.ok(attributesMapper.toDto(savedList), message);
    }

    @Override
    @Transactional
    public Response<?> update(@NonNull UpdateAttributesRequest request) {

        Attributes attributes = attributesRepository
                .findAttributesById(featureMerchandiseHelper.convertStringToUUID(request.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND,
                        "Thuộc tính sản phẩm không tồn tại."));

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

        if (request.getVariantOptions() != null && !request.getVariantOptions().isEmpty()) {
            List<VariantOption> variantOptions = request.getVariantOptions().stream()
                    .map(dto -> new VariantOption(dto.getKey(), dto.getValue()))
                    .toList();
            attributes.setVariantOptions(variantOptions);
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

        if (request.getSpecifications() != null) {
            attributes.setSpecifications(mapSpecificationGroups(request.getSpecifications()));
        }

        attributes.getPromotions().clear();
        attributes.getPromotions().addAll(
                promotionMapper.toEntity(request.getPromotions()));

        if (request.getStatus() != null) {
            attributes.setStatusProduct(request.getStatus());
        }

        attributesRepository.save(attributes);
        return Response.ok("Đã cập nhật thành công.");
    }

    @Override
    @Transactional
    public Response<?> delete(@NonNull List<String> ids) {
        if (ids.isEmpty()) {
            throw new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND, "Mã định danh không được để trống.");
        }

        List<Attributes> attributesToDelete = attributesRepository
                .findAllById(featureMerchandiseHelper.convertStringToUUID(String.valueOf(ids)));

        if (attributesToDelete.isEmpty()) {
            throw new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND,
                    "Không tìm thấy Danh mục với mã định danh đã cung cấp.");
        }

        if (attributesToDelete.size() != ids.size()) {
            log.warn("Một số SKU không tồn tại. Yêu cầu: {}, Tìm thấy: {}",
                    ids.size(),
                    attributesToDelete.size());
        }

        String currentUser = securityUtil.getCurrentUsername();

        // Soft delete
        attributesToDelete.forEach(attr -> {
            attr.getAuditInfo().markDeleted(currentUser);
        });

        attributesRepository.saveAll(attributesToDelete);

        log.info("Đã xóa {} attributes", attributesToDelete.size());
        return Response.noContent();
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

        attributesList.forEach(attr -> {
            attr.getAuditInfo().markDeleted(currentUser);
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
    public Page<AttributesDto> search(@NonNull AttributesSearchRequest request) {
        List<SearchCriteria> criteriaList = new ArrayList<>();

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            criteriaList.add(new SearchCriteria("name", ":", request.getKeyword()));
        }

        if (request.getAttributesIds() != null && !request.getAttributesIds().isEmpty()) {
            List<UUID> attrIds = request.getAttributesIds().stream()
                    .map(featureMerchandiseHelper::convertStringToUUID)
                    .toList();
            criteriaList.add(new SearchCriteria("id", "~", attrIds));
        }

        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            List<UUID> prodIds = request.getProductIds().stream()
                    .map(featureMerchandiseHelper::convertStringToUUID)
                    .toList();
            criteriaList.add(new SearchCriteria("product.id", "~", prodIds));
        }

        if (request.getSkus() != null && !request.getSkus().isEmpty()) {
            criteriaList.add(new SearchCriteria("sku.sku", "~", request.getSkus()));
        }

        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            criteriaList.add(new SearchCriteria("statusProduct", "~", request.getStatuses()));
        }

        if (request.getMinPrice() != null) {
            criteriaList.add(new SearchCriteria("price", ">", request.getMinPrice()));
        }
        if (request.getMaxPrice() != null) {
            criteriaList.add(new SearchCriteria("price", "<", request.getMaxPrice()));
        }

        if (request.getMinSalePrice() != null) {
            criteriaList.add(new SearchCriteria("salePrice", ">", request.getMinSalePrice()));
        }
        if (request.getMaxSalePrice() != null) {
            criteriaList.add(new SearchCriteria("salePrice", "<", request.getMaxSalePrice()));
        }

        if (request.getMinStockQuantity() != null) {
            criteriaList.add(new SearchCriteria("stockQuantity", ">", request.getMinStockQuantity()));
        }
        if (request.getMaxStockQuantity() != null) {
            criteriaList.add(new SearchCriteria("stockQuantity", "<", request.getMaxStockQuantity()));
        }

        if (request.getMinSoldQuantity() != null) {
            criteriaList.add(new SearchCriteria("soldQuantity", ">", request.getMinSoldQuantity()));
        }
        if (request.getMaxSoldQuantity() != null) {
            criteriaList.add(new SearchCriteria("soldQuantity", "<", request.getMaxSoldQuantity()));
        }

        if (request.getMinCostPrice() != null) {
            criteriaList.add(new SearchCriteria("costPrice", ">", request.getMinCostPrice()));
        }
        if (request.getMaxCostPrice() != null) {
            criteriaList.add(new SearchCriteria("costPrice", "<", request.getMaxCostPrice()));
        }
        
        if (request.getCreatedBy() != null && !request.getCreatedBy().isEmpty()) {
            criteriaList.add(new SearchCriteria("auditInfo.createdBy", "~", request.getCreatedBy()));
        }

        if (request.getCreatedFrom() != null) {
            criteriaList.add(new SearchCriteria("auditInfo.createdAt", ">", request.getCreatedFrom()));
        }
        if (request.getCreatedTo() != null) {
            criteriaList.add(new SearchCriteria("auditInfo.createdAt", "<", request.getCreatedTo()));
        }
        if (request.getUpdatedFrom() != null) {
            criteriaList.add(new SearchCriteria("auditInfo.updatedAt", ">", request.getUpdatedFrom()));
        }
        if (request.getUpdatedTo() != null) {
            criteriaList.add(new SearchCriteria("auditInfo.updatedAt", "<", request.getUpdatedTo()));
        }

        SpecificationBuilder<Attributes> builder = new SpecificationBuilder<>(criteriaList);
        org.springframework.data.jpa.domain.Specification<Attributes> spec = builder.build();

        Pageable pageable = (request.getPaging() != null) ? request.getPaging().pageable() : PageRequest.of(0, 10);

        return attributesRepository.findAll(spec, pageable)
                .map(attributesMapper::toDto);
    }

    /**
     * Tạo Cartesian product N chiều từ danh sách VariantGroupInput.
     *
     * VD: [{key:"Color", values:["Đen","Trắng"]}, {key:"Size", values:["S","M"]}]
     * → [[{Color:Đen, Size:S}, {Color:Đen, Size:M}], [{Color:Trắng, Size:S},
     * {Color:Trắng, Size:M}]]
     */
    private List<List<VariantOption>> generateCartesianProduct(List<VariantGroupInput> groups) {
        List<List<VariantOption>> result = new ArrayList<>();
        result.add(new ArrayList<>());

        for (VariantGroupInput group : groups) {
            List<List<VariantOption>> newResult = new ArrayList<>();
            for (List<VariantOption> existing : result) {
                for (String value : group.getValues()) {
                    List<VariantOption> combination = new ArrayList<>(existing);
                    combination.add(new VariantOption(group.getKey(), value));
                    newResult.add(combination);
                }
            }
            result = newResult;
        }

        return result;
    }

    /**
     * Map List<SpecificationGroupDto> → List<SpecificationGroup>
     */
    private List<SpecificationGroup> mapSpecificationGroups(
            List<com.anno.ERP_SpringBoot_Experiment.service.dto.SpecificationGroupDto> dtos) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream().map(dto -> {
            SpecificationGroup group = new SpecificationGroup();
            group.setTitle(dto.getTitle());
            if (dto.getItems() != null) {
                List<Specification> items = dto.getItems().stream()
                        .map(item -> new Specification(item.getKey(), item.getData()))
                        .toList();
                group.setItems(items);
            }
            return group;
        }).toList();
    }

}
